package com.face.nd.service;

import java.net.InetAddress;

public class NetStateService {
    public Boolean ping(String ip, int timeOut) {
        //超时应该在3钞以上,这里改为5s
        // 当返回值是true时，说明host是可用的，false则不可。
        try {
            boolean status = InetAddress.getByName(ip).isReachable(timeOut);
            return status;
        } catch (Exception e) {
            return false;
        }
    }
}
