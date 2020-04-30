package com.face.nd.task;

import com.face.nd.service.NTPTimeSynchronizationService;

import java.util.TimerTask;

public class DeviceTimeSynchronizationTaskService extends TimerTask {
    private NTPTimeSynchronizationService ntpTimeSynchronizationService = new NTPTimeSynchronizationService();

    @Override
    public void run() {
        ntpTimeSynchronizationService.setTime();
    }
}
