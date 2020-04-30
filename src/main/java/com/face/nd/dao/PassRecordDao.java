package com.face.nd.dao;

import com.face.nd.entity.PassRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface PassRecordDao {
    void insertAlarm(PassRecordEntity passRecordEntity);
}
