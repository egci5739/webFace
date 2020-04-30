package com.face.nd.task;


import com.face.nd.controller.EgciController;
import com.face.nd.entity.EquipmentEntity;
import com.face.nd.service.AlarmService;
import com.face.nd.service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

public class AlarmTaskService extends TimerTask {
    private Logger logger = LoggerFactory.getLogger(AlarmTaskService.class);

    @Override
    public void run() {
        for (EquipmentEntity equipmentEntity : EgciController.equipmentEntitySetAlarmFailure) {
            logger.info(equipmentEntity.getEquipmentIp() + "：正在重新布防");
            if (equipmentEntity.getEquipmentType() == 1) {
                LoginService loginService = new LoginService();
                if (loginService.login(equipmentEntity.getEquipmentIp(), EgciController.configEntity.getDevicePort(), EgciController.configEntity.getDeviceName(), EgciController.configEntity.getDevicePass())) {
                    AlarmService alarmService = new AlarmService();
                    if (alarmService.setupAlarmChan(loginService.getlUserID())) {
                        if (EgciController.equipmentEntitySetAlarmFailure.contains(equipmentEntity)) {
                            EgciController.equipmentEntitySetAlarmFailure.remove(equipmentEntity);
                        }
                    } else {
                        loginService.logout();
                    }
                }
            }
        }
    }
}
