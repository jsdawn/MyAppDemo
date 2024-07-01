package com.example.myappdemo.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.myappdemo.ui.MainActivity;

public class StartupOnBootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("StartupOnBootUpReceiver: ", " " + intent.getAction());

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent activityIntent = new Intent(context, MainActivity.class);
            // 高版本无效（未知）
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        }
    }
}