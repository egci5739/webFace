package com.face.nd.dao;

import com.face.nd.entity.EventLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface EventLogDao {
    void addEventLog(EventLogEntity eventLogEntity);
}
