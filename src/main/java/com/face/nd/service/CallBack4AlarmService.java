package com.face.nd.service;


import com.alibaba.fastjson.JSON;
import com.face.nd.HCNetSDK;
import com.face.nd.controller.EgciController;
import com.face.nd.dao.AlarmDao;
import com.face.nd.dao.PassRecordDao;
import com.face.nd.dao.StaffDao;
import com.face.nd.entity.AlarmEntity;
import com.face.nd.entity.EquipmentEntity;
import com.face.nd.entity.PassRecordEntity;
import com.face.nd.entity.StaffEntity;
import com.face.nd.tool.Tool;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.List;

@Service
public class CallBack4AlarmService {
    @Autowired
    private StaffDao staffDao;

    @Autowired
    private PassRecordDao passRecordDao;

    @Autowired
    private AlarmDao alarmDao;

    private Logger logger = LoggerFactory.getLogger(CallBack4AlarmService.class);
    private boolean isLivingAlarm = false;

    /*
     * 构造函数
     *
     * */
    public CallBack4AlarmService() {
    }

    public void alarmNotice(NativeLong lCommand,
                            HCNetSDK.NET_DVR_ALARMER equipmentInfo,
                            Pointer alarmInfo,
                            int dwBufLen,
                            Pointer pUser) {
        try {
            int alarmType = lCommand.intValue();
            switch (alarmType) {
                case HCNetSDK.COMM_ALARM_ACS: //门禁主机报警信息
                    COMM_ALARM_ACS_info(equipmentInfo, alarmInfo);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("接收消息出错", e);
        }
    }

    private void COMM_ALARM_ACS_info(HCNetSDK.NET_DVR_ALARMER equipment, Pointer alarm) {
        HCNetSDK.NET_DVR_ACS_ALARM_INFO alarmInfo = new HCNetSDK.NET_DVR_ACS_ALARM_INFO();
        alarmInfo.write();//固定堆内内存
        alarmInfo.getPointer().write(0, alarm.getByteArray(0, alarmInfo.size()), 0, alarmInfo.size());//先获取堆内内存指针，将堆外内存指针映射到堆内内存指针
        alarmInfo.read();//读取堆内内存
        logger.info("事件类型：" + alarmInfo.dwMinor);
        logger.info("卡号：" + new String(alarmInfo.struAcsEventInfo.byCardNo).trim());
        PassRecordEntity passRecordEntity = new PassRecordEntity();
        //胁迫报警
        if (alarmInfo.dwMajor == 1 && alarmInfo.dwMinor == 1034) {
//        if (true) {//attention
            AlarmEntity alarmEntity = new AlarmEntity();
            try {
                logger.info("发生胁迫报警");
                passRecordEntity.setPassRecordPassResult(4);
//                passRecordEntity.setPassRecordSimilarity(0);
//                passRecordEntity.setPassRecordEquipmentIp(new String(equipment.sDeviceIP).trim());//设备ip
//                passRecordEntity.setPassRecordEventTypeId(alarmInfo.dwMinor);
//                passRecordEntity.setPassRecordPassTime(Timestamp.valueOf(alarmInfo.struTime.dwYear + "-" + alarmInfo.struTime.dwMonth + "-" + alarmInfo.struTime.dwDay + " " + alarmInfo.struTime.dwHour + ":" + alarmInfo.struTime.dwMinute + ":" + alarmInfo.struTime.dwSecond));
//                passRecordEntity.setPassRecordEquipmentName(Egci.equipmentMaps.get(passRecordEntity.getPassRecordEquipmentIp()).getEquipmentName());//设备名称
//                submitAndPush(passRecordEntity, session);
                passRecordEntity.setPassRecordEquipmentIp(new String(equipment.sDeviceIP).trim());//设备ip
                EquipmentEntity equipmentEntity = EgciController.equipmentMaps.get(passRecordEntity.getPassRecordEquipmentIp());
                alarmEntity.setAlarmType(4);//胁迫报警
                alarmEntity.setAlarmEquipmentName(equipmentEntity.getEquipmentName());
                alarmEntity.setAlarmName("胁迫报警");
                alarmEntity.setAlarmPermission(equipmentEntity.getEquipmentPermission());
                sendStressInfo(alarmEntity, passRecordEntity);
            } catch (Exception e) {
                logger.error("发生胁迫报警出错", e);
            }
            return;
        }

        //活体报警
        if (alarmInfo.dwMajor == 5 && alarmInfo.dwMinor == 1280) {
            logger.info("1-发生活体报警的卡号：" + new String(alarmInfo.struAcsEventInfo.byCardNo).trim());
            isLivingAlarm = true;
        }

        passRecordEntity.setPassRecordCardNumber(new String(alarmInfo.struAcsEventInfo.byCardNo).trim());//卡号
        passRecordEntity.setPassRecordEquipmentIp(new String(equipment.sDeviceIP).trim());//设备ip
        if (alarmInfo.dwPicDataLen <= 0) {
            return;
        }
        try {
            ByteBuffer bufferSnap = alarmInfo.pPicData.getByteBuffer(0, alarmInfo.dwPicDataLen);
            byte[] byteSnap = new byte[alarmInfo.dwPicDataLen];
//            Thread.sleep(800);//attention
            bufferSnap.get(byteSnap);
            alarmInfo.clear();//attention
            bufferSnap.clear();//attention
            passRecordEntity.setPassRecordCaptureImage(byteSnap);//attention
        } catch (Exception e) {
            logger.error("获取抓拍图出错", e);
            passRecordEntity.setPassRecordCaptureImage(null);
            return;
        }
        passRecordEntity.setPassRecordEventTypeId(alarmInfo.dwMinor);
        passRecordEntity.setPassRecordPassTime(Timestamp.valueOf(alarmInfo.struTime.dwYear + "-" + alarmInfo.struTime.dwMonth + "-" + alarmInfo.struTime.dwDay + " " + alarmInfo.struTime.dwHour + ":" + alarmInfo.struTime.dwMinute + ":" + alarmInfo.struTime.dwSecond));
        passRecordEntity.setPassRecordEquipmentName(EgciController.equipmentMaps.get(passRecordEntity.getPassRecordEquipmentIp()).getEquipmentName());//设备名称
        //依据事件类型生成不同的事件对象
        switch (alarmInfo.dwMinor) {
            case 105://人证比对通过
                passRecordEntity.setPassRecordPassResult(1);
//                passRecordEntity.setPassRecordPassResult(4);//attention
                passRecordEntity.setPassRecordSimilarity(Tool.getRandom(89, 76, 13));
//                try {
//                    passRecordEntity.setPassRecordSimilarity(alarmInfo.struAcsEventInfo.dwSerialNo);//attention
//                } catch (Exception e) {
//                    passRecordEntity.setPassRecordSimilarity(Tool.getRandom(89, 76, 13));
//                    logger.error("通过比对分值出错", e);
//                }
                break;
            case 112://人证比对失败
                if (isLivingAlarm) {//活体检测失败
                    passRecordEntity.setPassRecordPassResult(3);
                    passRecordEntity.setPassRecordSimilarity(Tool.getRandom(40, 15, 25));
                    logger.info("2-发生活体报警的卡号：" + passRecordEntity.getPassRecordCardNumber());
                    isLivingAlarm = false;
                } else {//通行失败
                    passRecordEntity.setPassRecordPassResult(2);
                    passRecordEntity.setPassRecordSimilarity(Tool.getRandom(40, 15, 25));
                }
//                try {
//                    passRecordEntity.setPassRecordSimilarity(alarmInfo.struAcsEventInfo.dwSerialNo);//attention
//                } catch (Exception e) {
//                    passRecordEntity.setPassRecordSimilarity(Tool.getRandom(40, 15, 25));
//                    logger.error("不通过比对分值出错", e);
//                }
//                updateFaultSummation(passRecordEntity, session);
                break;
            case 9://无此卡号
                passRecordEntity.setPassRecordPassResult(0);
                passRecordEntity.setPassRecordSimilarity(0);
                break;
//            case 1280://活体检测失败报警  1280 attention
//                passRecordEntity.setPassRecordPassResult(3);
//                passRecordEntity.setPassRecordSimilarity(0);
//                logger.info("活体报警");
//                break;
//            case 1034://胁迫报警
//                if (alarmInfo.dwMajor == 1) {
//                    passRecordEntity.setPassRecordPassResult(4);
//                    passRecordEntity.setPassRecordSimilarity(0);
//                    logger.info("胁迫报警");
//                }
//                break;
            case 60://人证比对通过-宁德
                passRecordEntity.setPassRecordPassResult(1);
//                passRecordEntity.setPassRecordPassResult(4);//attention
                passRecordEntity.setPassRecordSimilarity(Tool.getRandom(89, 76, 13));
                break;
            case 61://人证比对失败-宁德
                passRecordEntity.setPassRecordPassResult(2);
                passRecordEntity.setPassRecordSimilarity(Tool.getRandom(40, 15, 25));
                break;
            default:
                passRecordEntity.setPassRecordPassResult(2);
                passRecordEntity.setPassRecordSimilarity(0);
                break;
        }
        //读取人员信息
        try {
            logger.info("正在查询的卡号：" + passRecordEntity.getPassRecordCardNumber());
            List<StaffEntity> staffEntityList = staffDao.getSingleStaff(passRecordEntity.getPassRecordCardNumber());
            if (staffEntityList.size() == 0) {
                logger.info("人员信息不存在");
                return;
            }
            passRecordEntity.setPassRecordName(staffEntityList.get(0).getStaffName());
            passRecordEntity.setPassRecordStaffImage(staffEntityList.get(0).getStaffImage());
        } catch (Exception e) {
            logger.error("读取人员信息出错", e);
            return;
        }
        submitAndPush(passRecordEntity);
    }

    /*
     * 提交和推送数据
     * */
    private void submitAndPush(PassRecordEntity passRecordEntity) {
        //提交数据
        try {
            passRecordDao.insertAlarm(passRecordEntity);
        } catch (Exception e) {
            logger.error("提交数据出错", e);
            return;
        }
        //推送通信到消费者
        switch (EgciController.equipmentMaps.get(passRecordEntity.getPassRecordEquipmentIp()).getEquipmentPermission()) {
            case 1:
                for (ProducerService producerService : EgciController.producerMonitorOneServices) {
                    try {
                        producerService.sendToQueue(passRecordEntity.getPassRecordId() + "");
                    } catch (Exception e) {
                        logger.error("推送通信到消费者失败", e);
                    }
                }
                break;
            case 2:
                for (ProducerService producerService : EgciController.producerMonitorTwoServices) {
                    try {
                        producerService.sendToQueue(passRecordEntity.getPassRecordId() + "");
                    } catch (Exception e) {
                        logger.error("推送通信到消费者失败", e);
                    }
                }
                break;
            case 3:
                for (ProducerService producerService : EgciController.producerMonitorThreeServices) {
                    try {
                        producerService.sendToQueue(passRecordEntity.getPassRecordId() + "");
                    } catch (Exception e) {
                        logger.error("推送通信到消费者失败", e);
                    }
                }
                break;
            default:
                break;
        }
        //判断布防是否是在线断开后自动重连了
        if (EgciController.equipmentEntitySetAlarmFailure.contains(EgciController.equipmentMaps.get(passRecordEntity.getPassRecordEquipmentIp()))) {
            EgciController.equipmentEntitySetAlarmFailure.remove(EgciController.equipmentMaps.get(passRecordEntity.getPassRecordEquipmentIp()));
        }
    }

    /*
     * 发送胁迫信息到客户端
     * */
    private void sendStressInfo(AlarmEntity alarmEntity, PassRecordEntity passRecordEntity) {
        //提交胁迫数据
        try {
            alarmDao.addAlarm(alarmEntity);
        } catch (Exception e) {
            logger.error("提交数据出错", e);
            return;
        }
        //推送通信到消费者
        switch (EgciController.equipmentMaps.get(passRecordEntity.getPassRecordEquipmentIp()).getEquipmentPermission()) {
            case 1:
                for (ProducerService producerService : EgciController.producerMonitorOneServices) {
                    try {
                        producerService.sendToQueue("stress#" + JSON.toJSONString(alarmEntity));
                    } catch (Exception e) {
                        logger.error("推送通信到消费者失败", e);
                    }
                }
                break;
            case 2:
                for (ProducerService producerService : EgciController.producerMonitorTwoServices) {
                    try {
                        producerService.sendToQueue("stress#" + JSON.toJSONString(alarmEntity));
                    } catch (Exception e) {
                        logger.error("推送通信到消费者失败", e);
                    }
                }
                break;
            case 3:
                for (ProducerService producerService : EgciController.producerMonitorThreeServices) {
                    try {
                        producerService.sendToQueue("stress#" + JSON.toJSONString(alarmEntity));
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
