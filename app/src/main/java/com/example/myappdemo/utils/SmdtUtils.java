package com.example.myappdemo.utils;

import android.app.smdt.SmdtManager;
import android.content.Context;

/**
 * 处理smdt各类api问题
 */
public class SmdtUtils {

    // 允许安装/卸载应用
    public static void installHandler(Context context) {
        try {
            SmdtManager smdt = SmdtManager.create(context);
            smdt.setAllowUninstall(true);
            smdt.setAllowinstall(true);
        } catch (Exception ignored) {

        }
    }
}
