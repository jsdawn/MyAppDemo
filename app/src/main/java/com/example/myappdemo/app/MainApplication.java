package com.example.myappdemo.app;

import android.app.Application;

import com.example.myappdemo.utils.MyUtils;
import com.october.lib.logger.LogUtils;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MyUtils.initLogger();
        LogUtils.d("MainApplication", "onCreate MainApplication");
    }
}
