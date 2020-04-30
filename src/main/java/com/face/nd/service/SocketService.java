package com.face.nd.service;

import com.alibaba.fastjson.JSON;
import com.face.nd.controller.EgciController;
import com.face.nd.entity.EquipmentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.List;

import static com.face.nd.controller.EgciController.equipmentEntitySet;

@Service
public class SocketService extends Thread {
    private Logger logger = LoggerFactory.getLogger(SocketService.class);
    private Socket socketInfo;
    private ModeService modeService;
    private StatusService statusService;

    public void setSocketInfo(Socket socketInfo) {
        this.socketInfo = socketInfo;
    }

    public SocketService() {
        //初始化设备状态
        statusService = new StatusService();
        //更改设备模式
        modeService = new ModeService();
    }

    /*
     * 数据处理
     * */
    @Override
    public void run() {
        //读取客户端发送来的信息
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(socketInfo.getInputStream()));
        } catch (IOException e) {
            logger.error("获取客户端消息失败：", e);
        }
        try {
            assert br != null;
            String mess = br.readLine();
            logger.info("客户端发来的消息：" + mess);
            String operationCode = mess.substring(0, 1);
            switch (Integer.parseInt(operationCode)) {
                case 3://获取设备状态
                    if (equipmentEntitySet.size() == 0) {
                        sendToClient(socketInfo, br, JSON.toJSONString(equipmentEntitySet));
                        logger.info("得到的状态是：" + JSON.toJSONString(equipmentEntitySet));
                        break;
                    }
                    //返回消息给客户端
                    sendToClient(socketInfo, br, JSON.toJSONString(equipmentEntitySet));
//                    logger.info("查询的设备状态：" + JSON.toJSONString(equipmentEntitySet));
                    break;
                case 4://设置一体机的通行模式
                    LoginService loginService = new LoginService();
                    String[] info = mess.split("#");
                    loginService.login(info[1], EgciController.devicePort, EgciController.deviceName, EgciController.devicePass);
                    //卡+人脸
                    if (info[2].equals("0")) {
                        modeService.changeMode(loginService.getlUserID(), (byte) 13);
                        //返回消息给客户端
                        sendToClient(socketInfo, br, "success");
                    }
                    //人脸
                    if (info[2].equals("1")) {
                        modeService.changeMode(loginService.getlUserID(), (byte) 14);
                        //返回消息给客户端
                        sendToClient(socketInfo, br, "success");
                    }
                    loginService.logout();
                    break;
                case 5://设置切换器模式:0是关闭人脸识别，1是开启人脸识别
                    String[] info5 = mess.split("#");
                    changeSwitchMode(info5[1], Integer.parseInt(info5[2]));
                    sendToClient(socketInfo, br, "success");
                    break;
                case 8://实时监控消息推送
                    String[] info8 = mess.split("#");
                    //判断客户端IP地址是否布防过：如果有，则先清除布防信息，防止多次推送消息
                    if (EgciController.producerServiceMap.get(socketInfo.getInetAddress().getHostAddress()) != null) {
                        EgciController.producerMonitorOneServices.remove(EgciController.producerServiceMap.get(socketInfo.getInetAddress().getHostAddress()));
                        EgciController.producerMonitorTwoServices.remove(EgciController.producerServiceMap.get(socketInfo.getInetAddress().getHostAddress()));
                        EgciController.producerMonitorThreeServices.remove(EgciController.producerServiceMap.get(socketInfo.getInetAddress().getHostAddress()));
                        EgciController.producerServiceMap.get(socketInfo.getInetAddress().getHostAddress()).deleteQueue();
                        Thread.sleep(3000);
                    }
                    ProducerService producerService8 = new ProducerService("push:" + socketInfo.getInetAddress().getHostAddress(), EgciController.configEntity.getQueueIp());
                    CustomerMonitorService customerMonitorService8 = new CustomerMonitorService("push:" + socketInfo.getInetAddress().getHostAddress(), producerService8.getChannel(), socketInfo);
                    customerMonitorService8.start();
                    if (info8[1].equals("1")) {
                        EgciController.producerMonitorOneServices.add(producerService8);
                    }
                    if (info8[2].equals("1")) {
                        EgciController.producerMonitorTwoServices.add(producerService8);
                    }
                    if (info8[3].equals("1")) {
                        EgciController.producerMonitorThreeServices.add(producerService8);
                    }
                    EgciController.producerServiceMap.put(socketInfo.getInetAddress().getHostAddress(), producerService8);
                    OutputStream os = socketInfo.getOutputStream();
                    os.write(("success\r\n").getBytes());
                    os.flush();
                    break;
                case 9://同步单台设备
                    String[] info9 = mess.split("#");
                    ImportStaffToSingleEquipmentService importStaffToSingleEquipmentService = new ImportStaffToSingleEquipmentService();
                    importStaffToSingleEquipmentService.setEquipmentIp(info9[1]);
                    importStaffToSingleEquipmentService.start();
                    sendToClient(socketInfo, br, "success");
                    break;
                case 0://设备时间与NTP同步
                    NTPTimeSynchronizationService ntpTimeSynchronizationService = new NTPTimeSynchronizationService();
                    ntpTimeSynchronizationService.setTime();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("socket数据处理出错：", e);
            sendToClient(socketInfo, br, "error");
        }
    }

    /*
     *返回消息到客户端
     * */
    private void sendToClient(Socket socket, BufferedReader br, String message) {
        try {
            OutputStream os = socket.getOutputStream();
            os.write((message + "\r\n").getBytes());
            os.flush();
            br.close();
            os.close();
            socket.close();
        } catch (IOException e) {
            logger.error("返回消息到客户端出错：" + e);
        }
    }

    /*
     * 获取设备状态
     * */
    private List<EquipmentEntity> getStatus(List<EquipmentEntity> equipmentEntityList, int permission) {
        logger.info("开始查询设备状态" + new Date());
//        LoginService loginService = new LoginService();
        NetStateService netStateService = new NetStateService();
        for (EquipmentEntity equipmentEntity : equipmentEntityList) {
            try {
                if (equipmentEntity.getEquipmentPermission() == permission || permission == 0) {
                    //判断是否在线
                    if (equipmentEntity.getEquipmentType() == 1) {//判断一体机
                        if (netStateService.ping(equipmentEntity.getEquipmentIp(), 3000)) {
//                            loginService.login(equipmentEntity.getEquipmentIp(), Egci.devicePort, Egci.deviceName, Egci.devicePass);
//                            if (loginService.getlUserID().longValue() > -1) {
//                                equipmentEntity.setIsLogin(1);
//                                equipmentEntity.setCardNumber(Integer.parseInt(statusService.getWorkStatus(loginService.getlUserID()).getCardNumber()));
//                                equipmentEntity.setPassMode(Integer.parseInt(statusService.getWorkStatus(loginService.getlUserID()).getPassMode()));
//                                loginService.logout();
//                            } else {
//                                equipmentEntity.setIsLogin(0);
//                                equipmentEntity.setCardNumber(0);
//                                equipmentEntity.setPassMode(0);
//                            }
                            equipmentEntity.setIsLogin(1);
                            equipmentEntity.setCardNumber(0);
                            equipmentEntity.setPassMode(0);
                        } else {
                            equipmentEntity.setIsLogin(0);
                            equipmentEntity.setCardNumber(0);
                            equipmentEntity.setPassMode(0);
                        }
                        /*
                         * 判断切换器,将Validity用来判断切换器状态:
                         * 0是离线
                         * 2:自动、蓝灯亮、启用人脸系统
                         * 3:自动、绿灯亮、停用人脸系统
                         * 4:手动、蓝灯亮、启用人脸系统
                         * 5:手动、绿灯亮、停用人脸系统
                         * */
                        //1.判断是否在线
                        try {
                            if (netStateService.ping(equipmentEntity.getEquipmentIp(), 3000)) {
                                equipmentEntity.setEquipmentValidity(1);
                                //2.判断模式
                                DatagramSocket socket = new DatagramSocket();
                                byte[] buf;
                                DatagramPacket packet;
                                buf = new byte[]{(byte) 0xA5, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A};
                                packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(equipmentEntity.getEquipmentIp()), 20108);
                                socket.send(packet);
                                byte[] bufReceive = new byte[buf.length];
                                DatagramPacket dp = new DatagramPacket(bufReceive, bufReceive.length);
                                socket.receive(dp);
                                logger.info("接收的消息为：" + dp.getData()[3] + dp.getData()[4] + dp.getData()[5]);
                                if (dp.getData()[3] == 1 && dp.getData()[4] == 0 && dp.getData()[5] == 1) {
                                    equipmentEntity.setEquipmentValidity(2);//2:自动、蓝灯亮、启用人脸系统
                                } else if (dp.getData()[3] == 0 && dp.getData()[4] == 1 && dp.getData()[5] == 1) {
                                    equipmentEntity.setEquipmentValidity(3);//3:自动、绿灯亮、停用人脸系统
                                } else if (dp.getData()[3] == 1 && dp.getData()[4] == 0 && dp.getData()[5] == 0) {
                                    equipmentEntity.setEquipmentValidity(4);//4:手动、蓝灯亮、启用人脸系统
                                } else if (dp.getData()[3] == 0 && dp.getData()[4] == 1 && dp.getData()[5] == 0) {
                                    equipmentEntity.setEquipmentValidity(5);//5:手动、绿灯亮、停用人脸系统
                                }
                            } else {
                                equipmentEntity.setEquipmentValidity(0);//切换器不在线
                            }
                        } catch (Exception e) {
                            logger.error("查询切换器状态出错", e);
                            equipmentEntity.setEquipmentValidity(0);//切换器不在线
                        }
                    } else {//判断其他设备
                        if (netStateService.ping(equipmentEntity.getEquipmentIp(), 3000)) {
                            equipmentEntity.setIsLogin(1);
                        } else {
                            equipmentEntity.setIsLogin(0);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("获取状态出错", e);
                equipmentEntity.setIsLogin(0);
                equipmentEntity.setCardNumber(0);
                equipmentEntity.setPassMode(0);
            }
        }
        logger.info("结束查询设备状态" + new Date());
        return equipmentEntityList;
    }

    /*
     * 更改切换器状态
     * */
    private void changeSwitchMode(String ip, int status) throws Exception {
        /*
         * 切换器指令
         * 绿灯亮（停用人脸）：(byte) 0xA5, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A
         * 绿灯灭（启用人脸）：(byte) 0xA5, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A
         * 蓝灯亮（启用人脸）：(byte) 0xA5, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A
         * 蓝灯灭（停用人脸）：(byte) 0xA5, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A
         * */
        DatagramSocket socket = new DatagramSocket();
        byte[] buf;
        DatagramPacket packet;
        if (status == 0) {//关闭人脸识别
            buf = new byte[]{(byte) 0xA5, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A};
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 20108);
            socket.send(packet);

            byte[] bufReceive = new byte[buf.length];
            DatagramPacket dp = new DatagramPacket(bufReceive, bufReceive.length);
            socket.receive(dp);
            logger.info("接收的消息为：" + dp.getData().length);

            Thread.sleep(1000);
            buf = new byte[]{(byte) 0xA5, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A};
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 20108);
            socket.send(packet);
            socket.close();
        } else {//启用人脸识别
            buf = new byte[]{(byte) 0xA5, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A};
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 20108);
            socket.send(packet);
            Thread.sleep(1000);
            buf = new byte[]{(byte) 0xA5, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5A};
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 20108);
            socket.send(packet);
            socket.close();
        }
    }
}
