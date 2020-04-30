package com.face.nd.dao;

import com.face.nd.entity.AlarmEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface AlarmDao {
    void addAlarm(AlarmEntity alarmEntity);
}
