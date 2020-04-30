package com.face.nd.handler;

import com.face.nd.HCNetSDK;
import com.face.nd.service.CallBack4AlarmService;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlarmHandler implements com.face.nd.HCNetSDK.FMSGCallBack_V31 {
    @Autowired
    private CallBack4AlarmService callBack4AlarmService;

    private Logger logger = LoggerFactory.getLogger(this.getClass());


//    public AlarmHandler() {
//        try {
//            callBack4AlarmService = new CallBack4AlarmService();
//        } catch (Exception e) {
//            logger.error("报警回调函数出错", e);
//        }
//    }

    @Override
    public boolean invoke(NativeLong lCommand,
                          com.face.nd.HCNetSDK.NET_DVR_ALARMER equipmentInfo,
                          Pointer alarmInfo,
                          int dwBufLen,
                          Pointer pUser) {
        try {
            callBack4AlarmService.alarmNotice(lCommand, equipmentInfo, alarmInfo, dwBufLen, pUser);
            Thread.sleep(300);//延迟
            return true;
        } catch (Exception e) {
            logger.error("获取一体机通行信息出错", e);
            return true;
        }
    }
}
