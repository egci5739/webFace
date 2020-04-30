package com.face.nd.service;

import com.face.nd.dao.StaffDao;
import com.face.nd.entity.StaffEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImportStaffToSingleEquipmentService extends Thread {
    @Autowired
    private StaffDao staffDao;

    private Logger logger = LoggerFactory.getLogger(ImportStaffToSingleEquipmentService.class);

    private String equipmentIp;//一体机ip

    public void setEquipmentIp(String equipmentIp) {
        this.equipmentIp = equipmentIp;
    }


    @Override
    public void run() {
        int result = 0;
        //第一步：获取全部人员信息
        List<StaffEntity> staffTableEntityList;
        staffTableEntityList = staffDao.getAllStaff();
        //登陆设备
        LoginService loginService = new LoginService();
        loginService.login(equipmentIp, (short) 8000, "admin", "hik12345");
        CardService cardService = new CardService();
        FaceService faceService = new FaceService();
        for (StaffEntity staffTableEntity : staffTableEntityList) {
            try {
                if (cardService.setCardInfo(loginService.getlUserID(), staffTableEntity.getStaffCardNumber(), staffTableEntity.getStaffName(), "666666", null)) {
                    faceService.setFaceInfo(staffTableEntity.getStaffCardNumber(), staffTableEntity.getStaffImage(), loginService.getlUserID());
                }
            } catch (Exception e) {
                logger.error("单台设备下发出错", e);
            }
        }
        logger.info("单台设备：" + equipmentIp + "：下发完成");
        loginService.logout();
    }
}
