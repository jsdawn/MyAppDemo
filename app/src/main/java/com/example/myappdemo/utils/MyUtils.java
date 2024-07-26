package com.example.myappdemo.utils;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

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


}
