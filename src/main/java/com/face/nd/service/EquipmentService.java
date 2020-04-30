package com.face.nd.service;

import com.face.nd.controller.EgciController;
import com.face.nd.dao.EquipmentDao;
import com.face.nd.entity.EquipmentEntity;
import com.face.nd.timer.PingTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EquipmentService {
    @Autowired
    private EquipmentDao equipmentDao;
    private Logger logger = LoggerFactory.getLogger(EquipmentService.class);

    public void initEquipmentInfo() {
        try {
            //获取全部设备
            EgciController.equipmentEntityList = equipmentDao.getAllEquipment();
            for (EquipmentEntity equipmentEntity : EgciController.equipmentEntityList) {
                //如果对象中有数据，就会循环打印出来
                if (equipmentEntity.getEquipmentType() == 1) {
                    EgciController.equipmentMaps.put(equipmentEntity.getEquipmentIp(), equipmentEntity);
                }
                if (equipmentEntity.getEquipmentType() != 2) {
                    EgciController.equipmentEntitySet.add(equipmentEntity);
                }
                logger.info("设备信息:" + equipmentEntity.getEquipmentName() + "-" + equipmentEntity.getEquipmentIp());
            }
            logger.info("数量：" + EgciController.equipmentEntitySet.size());
        } catch (Exception e) {
            logger.error("连接数据库和获取全部设备IP失败：", e);
        }
    }

    /*
     * 对所有门禁一体机设备进行布防
     * */
    public void initEquipmentAlarm() {
        for (EquipmentEntity equipmentEntity : EgciController.equipmentEntitySetOnline) {
            if (equipmentEntity.getEquipmentType() == 1) {
                LoginService loginService = new LoginService();
                loginService.login(equipmentEntity.getEquipmentIp(), EgciController.configEntity.getDevicePort(), EgciController.configEntity.getDeviceName(), EgciController.configEntity.getDevicePass());
                AlarmService alarmService = new AlarmService();
                if (!alarmService.setupAlarmChan(loginService.getlUserID())) {
                    logger.info(equipmentEntity.getEquipmentIp() + "：第一次布防失败");
                    EgciController.equipmentEntitySetAlarmFailure.add(equipmentEntity);
                    loginService.logout();
                } else {
                    logger.info("设备：" + equipmentEntity.getEquipmentName() + " 布防成功");
                }
                try {
                    //每布防一台设备后暂停时间，用来防止数据量瞬间过大导致程序出错
                    Thread.sleep(EgciController.configEntity.getAlarmTime());
                } catch (InterruptedException e) {
                    logger.error(equipmentEntity.getEquipmentIp() + "布防延迟失败", e);
                }
            }
        }
    }

    /*
     * 获取设备网络状态,并设置定时状态更新
     * */
    public void updateNetStatus() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (EquipmentEntity equipmentEntity : EgciController.equipmentEntitySet) {//已经不包括采集设备
                    try {
                        PingTimer pingTimer = new PingTimer(equipmentEntity, 5000);
                        Thread.sleep(1500);//避免同时插入离线报警信息
                        pingTimer.start();
                    } catch (Exception e) {
                        logger.error("获取设备网络状态,并设置定时状态更新出错", e);
                    }
                }
            }
        });
        thread.start();
    }
}
