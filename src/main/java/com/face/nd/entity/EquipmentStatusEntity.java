package com.face.nd.entity;

import org.springframework.stereotype.Component;

@Component
public class EquipmentStatusEntity {
    private String cardNumber;
    private String deviceIp;
    private String isLogin;
    private String passMode;

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public String getIsLogin() {
        return isLogin;
    }

    public void setIsLogin(String isLogin) {
        this.isLogin = isLogin;
    }

    public String getPassMode() {
        return passMode;
    }

    public void setPassMode(String passMode) {
        this.passMode = passMode;
    }
}
