package com.face.nd.service;


import com.face.nd.HCNetSDK;
import com.face.nd.controller.EgciController;
import com.face.nd.handler.CardGetHandler;
import com.face.nd.handler.CardSendHandler;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CardService {
    private Logger logger = LoggerFactory.getLogger(CardService.class);
    private CardSendHandler cardSendHandler = new CardSendHandler();
    private CardGetHandler cardGetHandler = new CardGetHandler();

    /*
     * 卡号下发
     * */
    public Boolean setCardInfo(NativeLong lUserID, String cardNo, String cardName, String password, String queueName) {
        NativeLong cardSendFtpFlag = buildSendCardTcpCon(lUserID);
        if (cardSendFtpFlag.intValue() < 0) {
            logger.info("建立设置卡号数据长连接失败，错误号：" + EgciController.hcNetSDK.NET_DVR_GetLastError() + "；设备队列：" + queueName);
            return false;
        }
        try {
            // 设置卡参数
            com.face.nd.HCNetSDK.NET_DVR_CARD_CFG_V50 struCardInfo = new com.face.nd.HCNetSDK.NET_DVR_CARD_CFG_V50();
            struCardInfo.read();
            struCardInfo.dwSize = struCardInfo.size();
            struCardInfo.dwModifyParamType = 0x00000001 + 0x00000002 + 0x00000004 + 0x00000008 +
                    0x00000010 + 0x00000020 + 0x00000080 + 0x00000100 + 0x00000200 + 0x00000400 + 0x00000800;
            struCardInfo.byCardValid = 1;
            struCardInfo.wRoomNumber = 302;
            struCardInfo.byCardType = 1;
            struCardInfo.byLeaderCard = 0;
            struCardInfo.byDoorRight[0] = 1; //门1有权限
            //卡有效期
            struCardInfo.struValid.byEnable = 0;
            struCardInfo.dwMaxSwipeTime = 0; //无次数限制
            struCardInfo.dwSwipeTime = 0; //已刷卡次数
            struCardInfo.byCardPassword = password.getBytes();//密码；固定
            //设置卡号
            try {
                for (int i = 0; i < com.face.nd.HCNetSDK.ACS_CARD_NO_LEN; i++) {
                    struCardInfo.byCardNo[i] = 0;
                }
                byte[] cardNoBytes = cardNo.getBytes(); //卡号
                System.arraycopy(cardNoBytes, 0, struCardInfo.byCardNo, 0, cardNo.length());
            } catch (Exception e) {
                logger.error("设置卡号出错：", e);
                stopRemoteConfig(cardSendFtpFlag);
                Thread.sleep(500);
                return false;
            }
            // 设置卡片名称
            try {
                for (int i = 0; i < com.face.nd.HCNetSDK.NAME_LEN; i++) {
                    struCardInfo.byName[i] = 0;
                }
                byte[] nameBytes = cardName.trim().getBytes("GBK");
                System.arraycopy(nameBytes, 0, struCardInfo.byName, 0, nameBytes.length);
            } catch (Exception e) {
                logger.error("设置卡片名称出错 :" + e);
                stopRemoteConfig(cardSendFtpFlag);
                Thread.sleep(500);
                return false;
            }
            struCardInfo.write();
            Pointer pSendBufSet = struCardInfo.getPointer();
            // 发送卡信息
            if (!EgciController.hcNetSDK.NET_DVR_SendRemoteConfig(cardSendFtpFlag, 0x3, pSendBufSet, struCardInfo.size())) {
                logger.error("卡号下发失败，错误码：" + EgciController.hcNetSDK.NET_DVR_GetLastError());
                stopRemoteConfig(cardSendFtpFlag);
                Thread.sleep(500);
                stopRemoteConfig(cardSendFtpFlag);
                Thread.sleep(500);
                return false;
            } else {
                stopRemoteConfig(cardSendFtpFlag);
                Thread.sleep(500);
                return true;
            }
        } catch (Exception e) {
            logger.error("卡号下发出错", e);
            return false;
        }
    }

    /*
     * 卡号下发的长连接
     * */
    private NativeLong buildSendCardTcpCon(NativeLong lUserID) {
        try {
            com.face.nd.HCNetSDK.NET_DVR_CARD_CFG_COND m_struCardInputParamSet = new com.face.nd.HCNetSDK.NET_DVR_CARD_CFG_COND();
            m_struCardInputParamSet.read();
            m_struCardInputParamSet.dwSize = m_struCardInputParamSet.size();
            m_struCardInputParamSet.dwCardNum = 1;
            m_struCardInputParamSet.byCheckCardNo = 1;
            Pointer lpInBuffer = m_struCardInputParamSet.getPointer();
            m_struCardInputParamSet.write();
            Pointer pUserData = null;
            NativeLong conFlag = EgciController.hcNetSDK.NET_DVR_StartRemoteConfig(lUserID, com.face.nd.HCNetSDK.NET_DVR_SET_CARD_CFG_V50, lpInBuffer, m_struCardInputParamSet.size(), cardSendHandler, pUserData);
            return conFlag;
        } catch (Exception e) {
            logger.error("获取卡号下发长连接失败", e);
            return new NativeLong(-1);
        }
    }

    /**
     * 卡号信息获取
     */
    public Boolean getCardInfo(String cardNo, NativeLong lUserID, String queueName) {
        NativeLong cardGetFtpFlag = buildGetCardTcpCon(EgciController.hcNetSDK, lUserID);
        if (cardGetFtpFlag.intValue() < 0) {
            logger.info("建立获取卡号长连接失败，错误号：" + EgciController.hcNetSDK.NET_DVR_GetLastError() + "；设备队列：" + queueName);
            return false;
        }
        try {
            //查找指定卡号
            com.face.nd.HCNetSDK.NET_DVR_CARD_CFG_SEND_DATA m_struCardSendInputParam = new com.face.nd.HCNetSDK.NET_DVR_CARD_CFG_SEND_DATA();
            m_struCardSendInputParam.read();
            m_struCardSendInputParam.dwSize = m_struCardSendInputParam.size();
            m_struCardSendInputParam.byCardNo = cardNo.getBytes();
            m_struCardSendInputParam.byRes = "0".getBytes();
            Pointer pSendBuf = m_struCardSendInputParam.getPointer();
            m_struCardSendInputParam.write();
            if (!EgciController.hcNetSDK.NET_DVR_SendRemoteConfig(cardGetFtpFlag, 0x3, pSendBuf, m_struCardSendInputParam.size())) {
                Thread.sleep(1000);
                logger.error("查询卡号失败，错误号：" + EgciController.hcNetSDK.NET_DVR_GetLastError());
                stopRemoteConfig(cardGetFtpFlag);
                Thread.sleep(500);
                return false;
            } else {
                Thread.sleep(1000);
                if (cardGetHandler.getCardNumber().equals("none")) {
                    stopRemoteConfig(cardGetFtpFlag);
                    Thread.sleep(500);
                    return false;
                } else {
                    stopRemoteConfig(cardGetFtpFlag);
                    Thread.sleep(500);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("查找卡号失败", e);
            return false;
        }
    }

    /**
     * 创建卡号查询的长连接
     */
    private NativeLong buildGetCardTcpCon(com.face.nd.HCNetSDK hcNetSDK, NativeLong lUserID) {
        try {
            com.face.nd.HCNetSDK.NET_DVR_CARD_CFG_COND m_struCardInputParam = new com.face.nd.HCNetSDK.NET_DVR_CARD_CFG_COND();
            m_struCardInputParam.dwSize = m_struCardInputParam.size();
            m_struCardInputParam.dwCardNum = 1; //查找全部
            m_struCardInputParam.byCheckCardNo = 1;
            Pointer lpInBuffer = m_struCardInputParam.getPointer();
            m_struCardInputParam.write();
            Pointer pUserData = null;
            Thread.sleep(500);
            NativeLong nativeLong = hcNetSDK.NET_DVR_StartRemoteConfig(lUserID, com.face.nd.HCNetSDK.NET_DVR_GET_CARD_CFG_V50, lpInBuffer, m_struCardInputParam.size(), cardGetHandler, pUserData);
            Thread.sleep(500);
            return nativeLong;
        } catch (Exception e) {
            logger.error("设置获取卡号长连接失败", e);
            return new NativeLong(-1);
        }
    }

    /*
     * 删除卡号
     * */
    public Boolean delCardInfo(String cardNo, NativeLong lUserID, String queueName) {
        NativeLong cardDelFtpFlag = buildSendCardTcpCon(lUserID);
        if (cardDelFtpFlag.intValue() < 0) {
            logger.error("建立删除卡号的长连接失败，错误号：" + EgciController.hcNetSDK.NET_DVR_GetLastError() + "；设备队列：" + queueName);
            return false;
        }
        try {
            // 设置卡参数
            com.face.nd.HCNetSDK.NET_DVR_CARD_CFG_V50 struCardInfo = new com.face.nd.HCNetSDK.NET_DVR_CARD_CFG_V50(); //卡参数
            struCardInfo.read();
            struCardInfo.dwSize = struCardInfo.size();
            struCardInfo.dwModifyParamType = 0x00000001;
            System.arraycopy(cardNo.getBytes(), 0, struCardInfo.byCardNo, 0, cardNo.length());
            struCardInfo.byCardValid = 0;//设置为0，进行删除
            struCardInfo.write();
            Pointer pSendBufSet = struCardInfo.getPointer();
            // 发送卡信息
            if (!EgciController.hcNetSDK.NET_DVR_SendRemoteConfig(cardDelFtpFlag, 0x3, pSendBufSet, struCardInfo.size())) {
                logger.error("卡号信息删除请求下发失败，错误号：" + EgciController.hcNetSDK.NET_DVR_GetLastError());
                Thread.sleep(500);
                stopRemoteConfig(cardDelFtpFlag);
                return false;
            } else {
                Thread.sleep(500);
                stopRemoteConfig(cardDelFtpFlag);
                return true;
            }
        } catch (Exception e) {
            logger.error("删除卡号出错", e);
            return false;
        }
    }

    /*
     *
     * 断开长连接
     * */
    private Boolean stopRemoteConfig(NativeLong conFlag) {
        if (!EgciController.hcNetSDK.NET_DVR_StopRemoteConfig(conFlag)) {
            logger.info("断开卡号操作的长连接失败，错误号：" + EgciController.hcNetSDK.NET_DVR_GetLastError());
            return false;
        } else {
            return true;
        }
    }
}
