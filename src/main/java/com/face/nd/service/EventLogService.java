package com.face.nd.service;

import com.face.nd.dao.EventLogDao;
import com.face.nd.entity.EventLogEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventLogService {
    @Autowired
    private EventLogDao eventLogDao;

    public void addEventLog(EventLogEntity eventLogEntity) {
        eventLogDao.addEventLog(eventLogEntity);
    }
}
