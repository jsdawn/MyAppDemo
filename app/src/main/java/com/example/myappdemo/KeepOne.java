package com.example.myappdemo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class KeepOne extends Service {

    public void KeeOne() {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("KeepOne", "onStartCommand");
        startServiceTwo();
        return START_REDELIVER_INTENT;
    }

    private void startServiceTwo() {
        boolean b = ServiceMangerUtils.isServiceWorked(KeepOne.this, "KeepServices");
        if (!b) {
            Intent service = new Intent(KeepOne.this, KeepServices.class);
            startService(service);
            Log.d("KeepOne", "Start ServiceTwo");
        }
    }

}
