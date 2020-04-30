package com.face.nd.timer;

import com.face.nd.entity.EquipmentEntity;
import com.face.nd.task.PingTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;

public class PingTimer extends Thread {
    private Logger logger = LoggerFactory.getLogger(PingTimer.class);
    private EquipmentEntity equipmentEntity;
    private int timeOut;

    public PingTimer(EquipmentEntity equipmentEntity, int timeOut) {
        this.equipmentEntity = equipmentEntity;
        this.timeOut = timeOut;
    }

    @Override
    public void run() {
        Timer timer = new Timer();
        PingTaskService pingTaskService = new PingTaskService(equipmentEntity);
        pingTaskService.setTimeOut(timeOut);
//        timer.schedule(pingTaskService, 480000, 20000);//attention
        timer.schedule(pingTaskService, 30000, 20000);
        logger.info(equipmentEntity.getEquipmentIp() + ":启用自动更新设备网络状态");
    }
}
