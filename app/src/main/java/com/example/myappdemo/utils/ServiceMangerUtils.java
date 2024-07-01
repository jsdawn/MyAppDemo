package com.example.myappdemo.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.util.ArrayList;

public class ServiceMangerUtils {
    public static boolean isDebugMode = false;

    public static boolean getDebugMode() {
        return isDebugMode;
    }

    /**
     * 判断服务是否运行
     *
     * @param context
     * @param serviceName
     * @return
     */
    public static boolean isServiceWorked(Context context, String serviceName) {
        ActivityManager myManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        //获取当前所有服务
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager.getRunningServices(Integer.MAX_VALUE);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }
}
