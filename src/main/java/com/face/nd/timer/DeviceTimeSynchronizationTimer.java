package com.face.nd.timer;


import com.face.nd.task.DeviceTimeSynchronizationTaskService;

import java.util.Timer;

public class DeviceTimeSynchronizationTimer extends Thread {
    @Override
    public void run() {
        Timer timer = new Timer();
        DeviceTimeSynchronizationTaskService deviceTimeSynchronizationTaskService = new DeviceTimeSynchronizationTaskService();
        timer.schedule(deviceTimeSynchronizationTaskService, 480000, 3600000);
    }
}
