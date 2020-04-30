package com.face.nd.controller;


import com.face.nd.HCNetSDK;
import com.face.nd.entity.ConfigEntity;
import com.face.nd.entity.EquipmentEntity;
import com.face.nd.handler.AlarmHandler;
import com.face.nd.service.*;
import com.face.nd.timer.AlarmTimer;
import com.face.nd.timer.DeviceTimeSynchronizationTimer;
import com.face.nd.timer.SynchronizationTimer;
import com.face.nd.timer.SystemResetTimer;
import com.face.nd.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.*;

@Controller
public class EgciController {
    @Autowired
    private ConfigService configService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private AlarmHandler alarmHandler;

    @Autowired
    private SynchronizationTimer synchronizationTimer;

    @Autowired
    private EquipmentStatusService equipmentStatusService;

    //配置文件
    public static ConfigEntity configEntity;
    //一体机变量
    public static short devicePort;
    public static String deviceName;
    public static String devicePass;
    //全局变量
    private Logger Elogger = LoggerFactory.getLogger(EgciController.class);
    public static List<EquipmentEntity> equipmentEntityList = new ArrayList<>();//所有设备信息,LIST
    public static Set<EquipmentEntity> equipmentEntitySet = new HashSet<>();//所有设备信息,SET,用来做设备状态
    public static Set<EquipmentEntity> equipmentEntitySetOnline = new HashSet<>();//在线设备
    public static Set<EquipmentEntity> equipmentEntitySetOffline = new HashSet<>();//离线设备
    public static Set<EquipmentEntity> equipmentEntitySetAlarmFailure = new HashSet<>();//布防失败的设备
    public static Map<String, EquipmentEntity> equipmentMaps = new HashMap<>();//所有一体机设备的信息，包含设备名称
    //初始化静态对象
    public static HCNetSDK hcNetSDK;
    //监控推送服务的生产者合集
    public static List<ProducerService> producerMonitorOneServices = new ArrayList<>();//监听一核设备
    public static List<ProducerService> producerMonitorTwoServices = new ArrayList<>();//监听二核设备
    public static List<ProducerService> producerMonitorThreeServices = new ArrayList<>();//监听三核设备
    //推送服务的生产者对象数组，用来解决异常推送问题
    public static Map<String, ProducerService> producerServiceMap = new HashMap<>();
    //防止手动查询设备状态未完成时出错:0-未完成；1-完成
    public static int equipmentStatus = 0;

    //    @Autowired
//    private SynchronizationTaskService synchronizationTaskService;
//
    /*
     * 初始化函数
     * */
    public void initServer() {
        Elogger.info(System.getProperty("user.dir"));
        Elogger.info("进程id：" + Tool.getProcessID());
        /*
         * 查看系统资源状态
         * */
        Elogger.info("Runtime max: " + mb(Runtime.getRuntime().maxMemory()));
        MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        Elogger.info("Non-heap: " + mb(m.getNonHeapMemoryUsage().getMax()));
        Elogger.info("Heap: " + mb(m.getHeapMemoryUsage().getMax()));
        for (MemoryPoolMXBean mp : ManagementFactory.getMemoryPoolMXBeans()) {
            Elogger.info("Pool: " + mp.getName() + " (type " + mp.getType() + ")" + " = " + mb(mp.getUsage().getMax()));
        }
        //将pid写入数据库
        processService.setProcessId(Tool.getProcessID());
        //初始化SDK静态对象
        try {
            hcNetSDK = HCNetSDK.INSTANCE;
        } catch (Exception e) {
            Elogger.error("初始化SDK静态对象，失败", e);
        }
        //初始化SDK
        if (!hcNetSDK.NET_DVR_Init()) {
            Elogger.error("SDK初始化失败");
            return;
        }
        //读取配置文件
        try {
            configEntity = configService.getConfig();
        } catch (Exception e) {
            Elogger.error("读取配置信息出错", e);
            Tool.showMessage(e.getMessage(), "连接数据库出错", 0);
            return;
        }
        //一体机参数配置
        devicePort = configEntity.getDevicePort();
        deviceName = configEntity.getDeviceName();
        devicePass = configEntity.getDevicePass();

        //初始化设备信息
        equipmentService.initEquipmentInfo();
        //获取全部设备状态
        equipmentStatusService.start();
        //获取设备网络状态
        int count = 0;
        for (EquipmentEntity equipmentEntity : equipmentEntitySet) {//已经不包括采集设备
            count += 1;
            try {
                NetStateService netStateService = new NetStateService();
                if (netStateService.ping(equipmentEntity.getEquipmentIp(), 4000)) {
                    equipmentEntitySetOnline.add(equipmentEntity);
                }
                Elogger.info("第 " + count + " 个" + equipmentEntity.getEquipmentIp() + "：获取网络状态成功");
            } catch (Exception e) {
                Elogger.error("获取在线/离线设备出错", e);
            }
        }
        //设置报警回调函数
        if (!HCNetSDK.INSTANCE.NET_DVR_SetDVRMessageCallBack_V31(alarmHandler, null)) {
            Elogger.info("设置回调函数失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
        }
        //对所有一体机设备进行布防
        equipmentService.initEquipmentAlarm();
        //开启自动布防重连定时任务
        AlarmTimer.open();
        //启动同步操作:0表示不启用；1表示单台；2表示全部
        if (!configEntity.getSynchronization().equals("0")) {
            synchronizationTimer.open();
            Elogger.info("开启自动同步功能");
        } else {
            Elogger.info("关闭自动同步功能");
        }
        //一体机设备一个小时同步一次
        DeviceTimeSynchronizationTimer deviceTimeSynchronizationTimer = new DeviceTimeSynchronizationTimer();
        deviceTimeSynchronizationTimer.start();
        //系统重置
        SystemResetTimer systemResetTimer = new SystemResetTimer();
        systemResetTimer.open();
        //获取系统默认编码
        Elogger.info("系统默认编码：" + System.getProperty("file.encoding")); //查询结果GBK
        //系统默认字符编码
        Elogger.info("系统默认字符编码：" + Charset.defaultCharset()); //查询结果GBK
        //操作系统用户使用的语言
        Elogger.info("系统默认语言：" + System.getProperty("user.language")); //查询结果zh

        /*
         * 测试获取一体机中的人员数量
         *
         * 记得删掉
         * */
//        synchronizationTaskService.run();
//        System.exit(0);


        //启用socket服务
        try {
            Elogger.info("本机IP地址" + InetAddress.getLocalHost());
            ServerSocket serverSocket = new ServerSocket(configEntity.getSocketMonitorPort());
            serverSocket.setSoTimeout(0);
            serverSocket.setReuseAddress(true);
            Elogger.info("等待客户端连接..............................................................................");
            //启动成功
            processService.setMonitorStatus();
            while (true) {
                Socket socket = serverSocket.accept();
                socket.setReuseAddress(true);
                SocketService socketService = new SocketService();
                socketService.setSocketInfo(socket);
                socketService.start();
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            Elogger.error("开启socket服务失败：", e);
        }
    }

    static String mb(long s) {
        return String.format("%d (%.2f M)", s, (double) s / (1024 * 1024));
    }
}
