package com.face.nd.service;

import com.face.nd.dao.ProcessDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProcessService {
    @Autowired
    private ProcessDao processDao;

    public void setProcessId(int id) {
        processDao.setProcessId(id);
    }

    public void setMonitorStatus() {
        processDao.setMonitorStatus();
    }
}
