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
import android.os.Environment;
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

import java.io.File;
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
            // 检查设备是否为摄像头
            if (isUsbCamera(device)) {
                // 设备是摄像头
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

    /**
     * 重命名文件，移除文件名中最后一次出现的 "_temp"
     *
     * @param filePath 原文件路径
     * @return 重命名后的路径，不成功则返回原路径
     */
    public static String renameTempFile(String filePath) {
        File oldFile = new File(filePath);
        if (!oldFile.exists()) {
            Log.e("MyUtils", "文件不存在: " + filePath);
            return "";
        }

        String fileName = oldFile.getName();
        int lastIndex = fileName.lastIndexOf("_temp");
        if (lastIndex == -1) {
            return filePath;
        }

        // 构建新文件名（去掉最后一次出现的 "_temp"）
        String newName = fileName.substring(0, lastIndex) + fileName.substring(lastIndex + 5);
        File newFile = new File(oldFile.getParent(), newName);

        // 执行重命名
        boolean success = oldFile.renameTo(newFile);
        if (success) {
            Log.d("FileUtils", "重命名成功: " + newFile.getAbsolutePath());
            return newFile.getAbsolutePath();
        } else {
            Log.e("FileUtils", "重命名失败，原因: " + (newFile.exists() ? "目标文件已存在" : "未知错误"));
        }
        return filePath;
    }


    public static String getUsbExternalPath(Context context) {
        File[] paths = ContextCompat.getExternalFilesDirs(context, null);
        // /storage/00D6-4AAA/Android/data/com.example.xxx/files
        return paths.length <= 1 ? null : paths[1].getAbsolutePath();
    }

    /**
     * 创建录制视频的文件
     *
     * @param filename 文件名（包含后缀）
     * @param context
     * @return file
     */
    public static File createVideoFile(String filename, Context context) {
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        String usbPath = getUsbExternalPath(context);
        if (usbPath != null) {
            // 有u盘则使用u盘文件夹
            storageDir = new File(usbPath + File.separator + "videoRecorder");
        }
        if (storageDir == null) return null;

        if (!storageDir.exists()) {
            boolean bool = storageDir.mkdirs();
            if (!bool) return null;
        }

        try {
            return new File(storageDir, filename);
        } catch (NullPointerException ignored) {
        }
        return null;
    }
}
