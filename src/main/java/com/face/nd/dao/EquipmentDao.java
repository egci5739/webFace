package com.face.nd.dao;

import com.face.nd.entity.EquipmentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component
public interface EquipmentDao {
    List<EquipmentEntity> getAllEquipment();
}
