package com.face.nd.task;

import com.alibaba.fastjson.JSON;
import com.face.nd.controller.EgciController;
import com.face.nd.dao.AlarmDao;
import com.face.nd.dao.EventLogDao;
import com.face.nd.entity.AlarmEntity;
import com.face.nd.entity.EquipmentEntity;
import com.face.nd.entity.EventLogEntity;
import com.face.nd.service.NetStateService;
import com.face.nd.service.ProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;

@Service
public class PingTaskService extends TimerTask {
    @Autowired
    private AlarmDao alarmDao;
    @Autowired
    private EventLogDao eventLogDao;
    private Logger logger = LoggerFactory.getLogger(PingTaskService.class);
    private EquipmentEntity equipmentEntity;

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    private int timeOut;

    public PingTaskService(EquipmentEntity equipmentEntity) {
        this.equipmentEntity = equipmentEntity;
    }

    @Override
    public void run() {
        try {
            NetStateService netStateService = new NetStateService();
            if (equipmentEntity.getEquipmentType() == 1) {//判断一体机
                if (netStateService.ping(equipmentEntity.getEquipmentIp(), timeOut)) {//一体机连接正常
                    if (equipmentEntity.getIsLogin() == 0) {//一体机之前断线
                        logger.info(equipmentEntity.getEquipmentIp() + ":加入布防重连任务");
                        EgciController.equipmentEntitySetAlarmFailure.add(equipmentEntity);
                        equipmentEntity.setIsLogin(1);
                        EgciController.equipmentEntitySet.add(equipmentEntity);
                        sendEquipmentStatusInfo();
                        //一体机设备上线
                        EventLogEntity eventLogEntity = new EventLogEntity();
                        eventLogEntity.setEventLogContent(equipmentEntity.getEquipmentName() + "-上线");
                        eventLogDao.addEventLog(eventLogEntity);
                        EgciController.equipmentEntitySetOnline.add(equipmentEntity);
                    }
                } else {
                    Thread.sleep(3000);//等待3秒
                    if (!netStateService.ping(equipmentEntity.getEquipmentIp(), timeOut)) {//二次确认能不能ping通
                        if (EgciController.equipmentEntitySetAlarmFailure.contains(equipmentEntity)) {
                            EgciController.equipmentEntitySetAlarmFailure.remove(equipmentEntity);
                        }
                        if (EgciController.equipmentEntitySetOnline.contains(equipmentEntity)) {
                            EgciController.equipmentEntitySetOnline.remove(equipmentEntity);
                        }
                        if (equipmentEntity.getIsLogin() == 1) {
                            equipmentEntity.setIsLogin(0);
                            EgciController.equipmentEntitySet.add(equipmentEntity);
                            sendEquipmentStatusInfo();
                            //离线
                            EventLogEntity eventLogEntity = new EventLogEntity();
                            eventLogEntity.setEventLogContent(equipmentEntity.getEquipmentName() + "-离线");
                            eventLogDao.addEventLog(eventLogEntity);
                            //新增掉线报警
                            AlarmEntity alarmEntity = new AlarmEntity();
                            alarmEntity.setAlarmName("离线报警");
                            alarmEntity.setAlarmType(3);
                            alarmEntity.setAlarmEquipmentName(equipmentEntity.getEquipmentName());
                            alarmEntity.setAlarmPermission(equipmentEntity.getEquipmentPermission());
                            alarmDao.addAlarm(alarmEntity);
                            sendAlarmInfo(alarmEntity);
                        }
                    }
                }
            } else {//判断切换器
                if (netStateService.ping(equipmentEntity.getEquipmentIp(), timeOut)) {//切换器连接正常
                    //判断模式
                    DatagramSocket socket = new DatagramSocket();
                    byte[] buf;
                    DatagramPacket packet;
                    buf = new byte[]{(byte) 0xA5, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A};
                    packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(equipmentEntity.getEquipmentIp()), 20108);
                    socket.send(packet);
                    byte[] bufReceive = new byte[buf.length];
                    DatagramPacket dp = new DatagramPacket(bufReceive, bufReceive.length);
                    socket.receive(dp);
                    int preStatus = equipmentEntity.getEquipmentValidity();//先前的切换器状态
//                    logger.info("接收的消息为：" + dp.getData()[3] + dp.getData()[4] + dp.getData()[5]);
                    if (dp.getData()[3] == 1 && dp.getData()[4] == 0 && dp.getData()[5] == 1) {
                        equipmentEntity.setEquipmentValidity(2);//2:自动、蓝灯亮、启用人脸系统
                    } else if (dp.getData()[3] == 0 && dp.getData()[4] == 1 && dp.getData()[5] == 1) {
                        equipmentEntity.setEquipmentValidity(3);//3:自动、绿灯亮、停用人脸系统
                    } else if (dp.getData()[3] == 1 && dp.getData()[4] == 0 && dp.getData()[5] == 0) {
                        equipmentEntity.setEquipmentValidity(4);//4:手动、蓝灯亮、启用人脸系统
                    } else if (dp.getData()[3] == 0 && dp.getData()[4] == 1 && dp.getData()[5] == 0) {
                        equipmentEntity.setEquipmentValidity(5);//5:手动、绿灯亮、停用人脸系统
                    }
                    if (equipmentEntity.getEquipmentValidity() != preStatus) {//状态发生变化
                        EgciController.equipmentEntitySet.add(equipmentEntity);
                        sendEquipmentStatusInfo();
                        EventLogEntity eventLogEntity = new EventLogEntity();
                        eventLogEntity.setEventLogContent(equipmentEntity.getEquipmentName() + "-上线");
                        eventLogDao.addEventLog(eventLogEntity);
                    }
                    socket.close();
                } else {//切换器断线
                    Thread.sleep(3000);//等待3秒
                    if (!netStateService.ping(equipmentEntity.getEquipmentIp(), timeOut)) {//二次确认能不能ping通
                        if (equipmentEntity.getEquipmentValidity() != 0) {
                            equipmentEntity.setEquipmentValidity(0);
                            EgciController.equipmentEntitySet.add(equipmentEntity);
                            sendEquipmentStatusInfo();
                            EventLogEntity eventLogEntity = new EventLogEntity();
                            eventLogEntity.setEventLogContent(equipmentEntity.getEquipmentName() + "-离线");
                            eventLogDao.addEventLog(eventLogEntity);
                            //新增掉线报警
                            AlarmEntity alarmEntity = new AlarmEntity();
                            alarmEntity.setAlarmName("离线报警");
                            alarmEntity.setAlarmType(3);
                            alarmEntity.setAlarmEquipmentName(equipmentEntity.getEquipmentName());
                            alarmEntity.setAlarmPermission(equipmentEntity.getEquipmentPermission());
                            alarmDao.addAlarm(alarmEntity);
                            sendAlarmInfo(alarmEntity);
                        }
                    }
                }
            }


//            if (netStateService.ping(equipmentEntity.getEquipmentIp(), timeOut)) {
//                if (!previousStatus) {
//                    if (equipmentEntity.getEquipmentType() == 1) {//一体机机布防
//                        logger.info(equipmentEntity.getEquipmentIp() + ":加入布防重连任务");
//                        Egci.equipmentEntitySetAlarmFailure.add(equipmentEntity);
//                    } else if (equipmentEntity.getEquipmentType() == 4) {//切换器状态
//                        //2.判断模式
//                        DatagramSocket socket = new DatagramSocket();
//                        byte[] buf;
//                        DatagramPacket packet;
//                        buf = new byte[]{(byte) 0xA5, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A};
//                        packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(equipmentEntity.getEquipmentIp()), 20108);
//                        socket.send(packet);
//                        byte[] bufReceive = new byte[buf.length];
//                        DatagramPacket dp = new DatagramPacket(bufReceive, bufReceive.length);
//                        socket.receive(dp);
//                        logger.info("接收的消息为：" + dp.getData()[3] + dp.getData()[4] + dp.getData()[5]);
//                        if (dp.getData()[3] == 1 && dp.getData()[4] == 0 && dp.getData()[5] == 1) {
//                            equipmentEntity.setEquipmentValidity(2);//2:自动、蓝灯亮、启用人脸系统
//                        } else if (dp.getData()[3] == 0 && dp.getData()[4] == 1 && dp.getData()[5] == 1) {
//                            equipmentEntity.setEquipmentValidity(3);//3:自动、绿灯亮、停用人脸系统
//                        } else if (dp.getData()[3] == 1 && dp.getData()[4] == 0 && dp.getData()[5] == 0) {
//                            equipmentEntity.setEquipmentValidity(4);//4:手动、蓝灯亮、启用人脸系统
//                        } else if (dp.getData()[3] == 0 && dp.getData()[4] == 1 && dp.getData()[5] == 0) {
//                            equipmentEntity.setEquipmentValidity(5);//5:手动、绿灯亮、停用人脸系统
//                        }
//                        socket.close();
//                    }
//                    equipmentEntity.setIsLogin(1);
//                    Egci.equipmentEntitySet.add(equipmentEntity);
//                    sendEquipmentStatusInfo();
//                    //上线
//                    EventLogEntity eventLogEntity = new EventLogEntity();
//                    eventLogEntity.setEventLogContent(equipmentEntity.getEquipmentName() + "-上线");
//                    Tool.addEvent(eventLogEntity);
//                }
//                previousStatus = true;
//                Egci.equipmentEntitySetOnline.add(equipmentEntity);
//            } else {
//                Thread.sleep(3000);//等待3秒
//                if (!netStateService.ping(equipmentEntity.getEquipmentIp(), timeOut)) {//二次确认能不能ping通
//                    if (Egci.equipmentEntitySetAlarmFailure.contains(equipmentEntity)) {
//                        Egci.equipmentEntitySetAlarmFailure.remove(equipmentEntity);
//                    }
//                    if (Egci.equipmentEntitySetOnline.contains(equipmentEntity)) {
//                        Egci.equipmentEntitySetOnline.remove(equipmentEntity);
//                    }
//                    if (previousStatus) {
//                        if (equipmentEntity.getEquipmentType() == 4) {
//                            equipmentEntity.setEquipmentValidity(0);
//                        }
//                        equipmentEntity.setIsLogin(0);
//                        Egci.equipmentEntitySet.add(equipmentEntity);
//                        sendEquipmentStatusInfo();
//                        //离线
//                        EventLogEntity eventLogEntity = new EventLogEntity();
//                        eventLogEntity.setEventLogContent(equipmentEntity.getEquipmentName() + "-离线");
//                        Tool.addEvent(eventLogEntity);
//                        //新增掉线报警
//                        AlarmEntity alarmEntity = new AlarmEntity();
//                        alarmEntity.setAlarmName("离线报警");
//                        alarmEntity.setAlarmDetail(equipmentEntity.getEquipmentName() + "-" + equipmentEntity.getEquipmentIp() + "-离线");
//                        alarmEntity.setAlarmPermission(equipmentEntity.getEquipmentPermission());
//                        Tool.addAlarm(alarmEntity);
//                        sendAlarmInfo(alarmEntity);
//                    }
//                    previousStatus = false;
//                }
//            }
        } catch (Exception e) {
            logger.error("定时获取设备网络状态失败", e);
        }
    }

    /*
     * 发送设备状态到客户端
     * */
    private void sendEquipmentStatusInfo() {
        if (EgciController.equipmentStatus == 0) {
            return;
        }
        //推送通信到消费者
        switch (equipmentEntity.getEquipmentPermission()) {
            case 1:
                for (ProducerService producerService : EgciController.producerMonitorOneServices) {
                    try {
                        producerService.sendToQueue("status#" + JSON.toJSONString(EgciController.equipmentEntitySet));
                    } catch (Exception e) {
                        logger.error("推送通信到消费者失败", e);
                    }
                }
                break;
            case 2:
                for (ProducerService producerService : EgciController.producerMonitorTwoServices) {
                    try {
                        producerService.sendToQueue("status#" + JSON.toJSONString(EgciController.equipmentEntitySet));
                    } catch (Exception e) {
                        logger.error("推送通信到消费者失败", e);
                    }
                }
                break;
            case 3:
                for (ProducerService producerService : EgciController.producerMonitorThreeServices) {
                    try {
                        producerService.sendToQueue("status#" + JSON.toJSONString(EgciController.equipmentEntitySet));
                    } catch (Exception e) {
                        logger.error("推送通信到消费者失败", e);
                    }
                }
                break;
            default:
                break;
        }
    }

    /*
     * 发送报警信息到客户端
     * */
    private void sendAlarmInfo(AlarmEntity alarmEntity) {
        //推送通信到消费者
        switch (equipmentEntity.getEquipmentPermission()) {
            case 1:
                for (ProducerService producerService : EgciController.producerMonitorOneServices) {
                    try {
                        producerService.sendToQueue("alarm#" + JSON.toJSONString(alarmEntity));
                    } catch (Exception e) {
                        logger.error("推送通信到消费者失败", e);
                    }
                }
                break;
            case 2:
                for (ProducerService producerService : EgciController.producerMonitorTwoServices) {
                    try {
                        producerService.sendToQueue("alarm#" + JSON.toJSONString(alarmEntity));
                    } catch (Exception e) {
                        logger.error("推送通信到消费者失败", e);
                    }
                }
                break;
            case 3:
                for (ProducerService producerService : EgciController.producerMonitorThreeServices) {
                    try {
                        producerService.sendToQueue("alarm#" + JSON.toJSONString(alarmEntity));
                    } catch (Exception e) {
                        logger.error("推送通信到消费者失败", e);
                    }
                }
                break;
            default:
                break;
        }
    }
}