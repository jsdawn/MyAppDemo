package com.example.myappdemo.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.myappdemo.utils.ServiceMangerUtils;

public class KeepOne extends Service {

    private static KeepOne instance;

    public void KeeOne() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (instance != null && this != instance) {
            // 如果已经有一个实例在运行，则停止当前服务
            Log.d("KeepOne", "KeepOne instance is already running. Stopping service...");
            stopSelf();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        startServiceTwo();
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();

        return START_REDELIVER_INTENT;
    }

    private void startServiceTwo() {
        boolean b = ServiceMangerUtils.isServiceWorked(KeepOne.this, "com.example.myappdemo.service.KeepServices");
        if (!b) {
            Log.d("KeepOne", "没有keep服务");
            Intent service = new Intent(KeepOne.this, KeepServices.class);
            startService(service);
        } else {
            Log.d("KeepOne", "已有keep服务");
        }
    }

}
