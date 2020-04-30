package com.face.nd.service;

import com.face.nd.HCNetSDK;
import com.face.nd.controller.EgciController;
import com.face.nd.dao.AlarmDao;
import com.face.nd.entity.AlarmEntity;
import com.sun.jna.NativeLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class AlarmService {
    @Autowired
    private AlarmDao alarmDao;

    //布防标识符
    private NativeLong lAlarmHandleFlag = new NativeLong(-1);
    private Logger logger = LoggerFactory.getLogger(AlarmService.class);

    /**
     * 布防
     *
     * @param lUserID 海康注册成功后返回的userId
     * @return
     */
    public Boolean setupAlarmChan(NativeLong lUserID) {
        Boolean status;
        try {
            if (lUserID.intValue() == -1) {
                logger.info("请先注册！");
            }
            if (lAlarmHandleFlag.intValue() >= 0) {
                logger.info("已经布防过了！");
            }
            HCNetSDK.NET_DVR_SETUPALARM_PARAM strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
            strAlarmInfo.dwSize = strAlarmInfo.size();
            strAlarmInfo.byLevel = 1;
            strAlarmInfo.byAlarmInfoType = 1;
            strAlarmInfo.byDeployType = 0;
            strAlarmInfo.write();
            lAlarmHandleFlag = EgciController.hcNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, strAlarmInfo);
            if (lAlarmHandleFlag.intValue() == -1) {
                if (EgciController.hcNetSDK.NET_DVR_GetLastError() == 52) {
                    status = true;
                } else {
                    logger.info("布防失败，错误码：" + EgciController.hcNetSDK.NET_DVR_GetLastError());
                    status = false;
                }
            } else {
                logger.info("布防成功!");
                status = true;
            }
        } catch (Exception e) {
            logger.error("error", e);
            status = false;
        }
        return status;
    }

    /*
     * 存入报警信息并推送到设备
     * */
    public void addAlarm(AlarmEntity alarmEntity) {
        alarmDao.addAlarm(alarmEntity);
    }
}
