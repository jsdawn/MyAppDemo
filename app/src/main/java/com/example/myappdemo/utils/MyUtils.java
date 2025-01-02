package com.example.myappdemo.utils;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.october.lib.logger.LogLevel;
import com.october.lib.logger.LogUtils;
import com.october.lib.logger.Logger;
import com.october.lib.logger.compress.ZipLogCompressStrategy;
import com.october.lib.logger.crash.DefaultCrashStrategyImpl;
import com.october.lib.logger.disk.BaseLogDiskStrategy;
import com.october.lib.logger.disk.TimeLogDiskStrategyImpl;
import com.october.lib.logger.format.LogTxtDefaultFormatStrategy;
import com.october.lib.logger.print.BaseLogTxtPrinter;
import com.october.lib.logger.print.LogTxtDefaultPrinter;
import com.october.lib.logger.print.LogcatDefaultPrinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MyUtils {
    private final String TAG = MyUtils.class.getSimpleName();

    // 初始化日志
    public static void initLogger() {
        // 设置磁盘策略：按时间管理日志
        BaseLogDiskStrategy diskStrategy = new TimeLogDiskStrategyImpl();
        // 设置压缩策略：创建新文件时，压缩旧文件
        diskStrategy.setLogCompressStrategy(new ZipLogCompressStrategy());
        // 设置打印日志到磁盘 defaultPath: storage/emulated/0/Android/data/packageName/files/log
        BaseLogTxtPrinter logTxtPrinter = new LogTxtDefaultPrinter(true, LogLevel.V, new LogTxtDefaultFormatStrategy(), diskStrategy);

        Logger logger = new Logger.Builder().setLogcatPrinter(new LogcatDefaultPrinter())   //设置 Logcat printer
                .setLogTxtPrinter(logTxtPrinter)   //设置 LogTxt printer
                .setCrashStrategy(new DefaultCrashStrategyImpl()) //设置 crash ,捕获异常日志
                .build();
        LogUtils.setLogger(logger);
    }

    // 打开设置
    public static void openSystemSettings(Context context) {
        Intent intent = new Intent("/");
        ComponentName cm = new ComponentName("com.android.settings", "com.android.settings.Settings");
        intent.setComponent(cm);
        intent.setAction("android.intent.action.VIEW");
        startActivity(context, intent, null);
        ServiceMangerUtils.isDebugMode = true;
    }

    // 是否处于前台
    public static boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = context.getApplicationContext().getPackageName();
        // 获取Android设备中所有正在运行的App
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
        if (runningProcesses == null) return false;
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            // The name of the process that this object is associated with.
            if (processInfo.processName.equals(packageName) && processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkPermission(Activity activity, @NonNull String[] permissions) {
        List<String> noPermissions = new ArrayList<>();
        for (String permission : permissions) {
            // 检查权限
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                noPermissions.add(permission);
            }
        }

        if (!noPermissions.isEmpty()) {
            LogUtils.d("MyUtils", "noPermissions: " + noPermissions);
            return false;
        }
        return true;
    }

    // 是否是模拟器
    public static boolean isEmulator() {
        // 指纹信息和型号
        if (Build.FINGERPRINT.startsWith("generic/sdk_google")
                || Build.FINGERPRINT.toLowerCase().contains("vbox")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")) {

            // Log.d(TAG, Build.FINGERPRINT + ":" + Build.MODEL);
            return true;
        }
        // 其他false
        return false;
    }

    /**
     * 异或校验
     *
     * @param data 十六进制串
     * @return checkData  十六进制串
     */
    public static String checkXor(String data) {
        int checkData = 0;
        for (int i = 0; i < data.length(); i = i + 2) {
            //将十六进制字符串转成十进制
            int start = Integer.parseInt(data.substring(i, i + 2), 16);
            //进行异或运算
            checkData = start ^ checkData;
        }
        return integerToHexString(checkData);
    }

    /**
     * 将十进制整数转为十六进制数，并补位
     */
    public static String integerToHexString(int s) {
        String ss = Integer.toHexString(s);
        if (ss.length() % 2 != 0) {
            ss = "0" + ss;//0F格式
        }
        return ss.toUpperCase();
    }


    public static UsbDevice getUsbCameraDevice(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        for (UsbDevice device : deviceList.values()) {
            Log.d("getDeviceName", device.getProductName());
            // 检查设备是否为摄像头
            if (isUsbCamera(device)) {
                Log.e("isUsbCamera", device.getProductName());
                // 设备是摄像头
                // 你可以在这里添加你的逻辑，比如打开摄像头等
                return device;
            }
        }
        return null;
    }

    private static boolean isUsbCamera(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface usbInterface = device.getInterface(i);
            // 检查接口是否为视频类接口
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VIDEO) {
                return true;
            }
        }
        return false;
    }
}
