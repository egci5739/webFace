package com.face.nd.task;


import com.face.nd.controller.EgciController;
import com.face.nd.dao.StaffDao;
import com.face.nd.entity.EquipmentEntity;
import com.face.nd.service.LoginService;
import com.face.nd.service.SynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;


@Service
public class SynchronizationTaskService extends TimerTask {
    @Autowired
    private StaffDao staffDao;

    @Autowired
    private SynchronizationService synchronizationService;

    private Logger logger = LoggerFactory.getLogger(SynchronizationTaskService.class);
    private List<String> dataBaseCards = new ArrayList<String>();//数据库人员卡号信息

    public void run() {
        try {
            dataBaseCards.clear();
            Thread.sleep(3000);
            dataBaseCards = staffDao.getAllStaffCard();
            logger.info("数据库中的总卡数：" + dataBaseCards.size());
            if (dataBaseCards.size() > 5000) {
                logger.info("夜间同步开始");
                for (EquipmentEntity equipmentEntity : EgciController.equipmentEntityList) {
                    //每次同步单台设备
                    if (equipmentEntity.getEquipmentType() == 1) {
                        LoginService loginService = new LoginService();
                        loginService.login(equipmentEntity.getEquipmentIp(), EgciController.configEntity.getDevicePort(), EgciController.configEntity.getDeviceName(), EgciController.configEntity.getDevicePass());
                        if (loginService.getlUserID().longValue() > -1) {
                            logger.info("同步开始：" + equipmentEntity.getEquipmentName() + "-" + equipmentEntity.getEquipmentIp());
                            synchronizationService.init(equipmentEntity, loginService, EgciController.configEntity, dataBaseCards);
                            synchronizationService.run();
                            logger.info("同步结束：" + equipmentEntity.getEquipmentName() + "-" + equipmentEntity.getEquipmentIp());
                            Thread.sleep(EgciController.configEntity.getDataBaseTime());//避免同时查询数据库，这里用作每台同步的间隔时间
                        } else {
                            logger.info(equipmentEntity.getEquipmentName() + "-" + equipmentEntity.getEquipmentIp() + "：同步失败：设备不在线或网络异常");
                        }
                    }
                }
                logger.info("夜间同步结束");
            } else {
                logger.info("获取数据库卡数量异常");
            }
        } catch (Exception e) {
            logger.error("同步出错", e);
        }
    }
}