package com.face.nd.service;

import com.face.nd.controller.EgciController;
import com.face.nd.entity.EquipmentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;

@Service
public class EquipmentStatusService extends Thread {
    @Autowired
    private EquipmentService equipmentService;

    private Logger logger = LoggerFactory.getLogger(EquipmentStatusService.class);

    /**
     * 获取所有设备状态
     */
    @Override
    public void run() {
        int i = 0;
        logger.info("开始查询设备状态" + new Date());
//        LoginService loginService = new LoginService();
        for (EquipmentEntity equipmentEntity : EgciController.equipmentEntitySet) {
            i++;
            NetStateService netStateService = new NetStateService();
            try {
                //判断是否在线
                if (equipmentEntity.getEquipmentType() == 1 || equipmentEntity.getEquipmentType() == 3) {
                    logger.info("正在查询：" + i + " 个 " + equipmentEntity.getEquipmentName() + "-" + equipmentEntity.getEquipmentIp());
                    //判断一体机和抓拍机
                    if (netStateService.ping(equipmentEntity.getEquipmentIp(), 5000)) {
//                            loginService.login(equipmentEntity.getEquipmentIp(), Egci.devicePort, Egci.deviceName, Egci.devicePass);
//                            if (loginService.getlUserID().longValue() > -1) {
//                                equipmentEntity.setIsLogin(1);
//                                equipmentEntity.setCardNumber(Integer.parseInt(statusService.getWorkStatus(loginService.getlUserID()).getCardNumber()));
//                                equipmentEntity.setPassMode(Integer.parseInt(statusService.getWorkStatus(loginService.getlUserID()).getPassMode()));
//                                loginService.logout();
//                            } else {
//                                equipmentEntity.setIsLogin(0);
//                                equipmentEntity.setCardNumber(0);
//                                equipmentEntity.setPassMode(0);
//                            }
                        equipmentEntity.setIsLogin(1);
                        equipmentEntity.setCardNumber(0);
                        equipmentEntity.setPassMode(0);
                    } else {
                        equipmentEntity.setIsLogin(0);
                        equipmentEntity.setCardNumber(0);
                        equipmentEntity.setPassMode(0);
                    }
                } else if (equipmentEntity.getEquipmentType() == 4) {
                    logger.info("正在查询：" + i + " 个 " + equipmentEntity.getEquipmentName() + "-" + equipmentEntity.getEquipmentIp());
                    /*
                     * 判断切换器,将Validity用来判断切换器状态:
                     * 0是离线
                     * 2:自动、蓝灯亮、启用人脸系统
                     * 3:自动、绿灯亮、停用人脸系统
                     * 4:手动、蓝灯亮、启用人脸系统
                     * 5:手动、绿灯亮、停用人脸系统
                     * */
                    //1.判断是否在线
                    try {
                        if (netStateService.ping(equipmentEntity.getEquipmentIp(), 5000)) {
                            equipmentEntity.setEquipmentValidity(1);
                            //2.判断模式
                            DatagramSocket socket = new DatagramSocket();
                            byte[] buf;
                            DatagramPacket packet;
                            buf = new byte[]{(byte) 0xA5, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A};
                            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(equipmentEntity.getEquipmentIp()), 20108);
                            socket.send(packet);
                            byte[] bufReceive = new byte[buf.length];
                            DatagramPacket dp = new DatagramPacket(bufReceive, bufReceive.length);
                            socket.receive(dp);
                            logger.info("接收的消息为：" + dp.getData()[3] + dp.getData()[4] + dp.getData()[5]);
                            if (dp.getData()[3] == 1 && dp.getData()[4] == 0 && dp.getData()[5] == 1) {
                                equipmentEntity.setEquipmentValidity(2);//2:自动、蓝灯亮、启用人脸系统
                            } else if (dp.getData()[3] == 0 && dp.getData()[4] == 1 && dp.getData()[5] == 1) {
                                equipmentEntity.setEquipmentValidity(3);//3:自动、绿灯亮、停用人脸系统
                            } else if (dp.getData()[3] == 1 && dp.getData()[4] == 0 && dp.getData()[5] == 0) {
                                equipmentEntity.setEquipmentValidity(4);//4:手动、蓝灯亮、启用人脸系统
                            } else if (dp.getData()[3] == 0 && dp.getData()[4] == 1 && dp.getData()[5] == 0) {
                                equipmentEntity.setEquipmentValidity(5);//5:手动、绿灯亮、停用人脸系统
                            }
                            socket.close();
                        } else {
                            equipmentEntity.setEquipmentValidity(0);//切换器不在线
                        }
                    } catch (Exception e) {
                        logger.error("查询切换器状态出错", e);
                        equipmentEntity.setEquipmentValidity(0);//切换器不在线
                    }
                }
            } catch (Exception e) {
                logger.error("获取状态出错", e);
                equipmentEntity.setIsLogin(0);
                equipmentEntity.setCardNumber(0);
                equipmentEntity.setPassMode(0);
                equipmentEntity.setEquipmentValidity(0);
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //更新设备信息
            EgciController.equipmentEntitySet.add(equipmentEntity);
            logger.info("已查询第：" + i + " 个 " + equipmentEntity.getEquipmentName() + "-" + equipmentEntity.getEquipmentIp());
        }
        //定时状态更新 attention
        equipmentService.updateNetStatus();
        logger.info("结束查询设备状态" + new Date());
        EgciController.equipmentStatus = 1;
    }
}
