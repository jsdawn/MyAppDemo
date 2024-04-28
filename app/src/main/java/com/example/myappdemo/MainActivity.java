package com.example.myappdemo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnEnd = findViewById(R.id.button);
        btnEnd.setOnClickListener(v -> {
            Log.d("setOnClickListener", "setOnClickListener");
            Integer.parseInt("wyh");

        });

        Log.d("KeepServices", "MainActivity onCreate");
        Intent intent = new Intent(this, KeepServices.class);
        startService(intent);

        Intent intent2 = new Intent(this, KeepOne.class);
        startService(intent2);
    }

    @Override
    protected void onStart() {
        super.onStart();
//
//        IntentFilter filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
//        registerReceiver(new StartupOnBootUpReceiver(), filter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isAppOnForeground()) {
            Log.d("onResume", "应用在前台");
        } else {
            Log.d("onResume", "应用在后台");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isAppOnForeground()) {
            Log.d("onPause", "应用在前台");
        } else {
            Log.d("onPause", "应用在后台");
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isAppOnForeground()) {
            Log.d("onStop", "应用在前台");
        } else {
            Log.d("onStop", "应用在后台");
            // app进入后台，调用打开第三方应用方法（新打开，多次会打开多个）
            // Intent intent = mContext.getPackageManager().getLaunchIntentForPackage("com.example.myappdemo");
            // mContext.startActivity(intent);

//            Intent intent = new Intent(mContext, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP); // You need this if starting
//            //  the activity from a service
//            intent.setAction(Intent.ACTION_MAIN);
//            intent.addCategory(Intent.CATEGORY_LAUNCHER);
//            startActivity(intent);

//            PackageManager packageManager = mContext.getPackageManager();
//            Intent intent = packageManager.getLaunchIntentForPackage(mContext.getPackageName());
//            if (intent != null) {
//                //模拟点击桌面图标的启动参数
//                intent.setPackage(null);
//                // intent.setSourceBounds(new Rect(804,378, 1068, 657));
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//                mContext.startActivity(intent);
//            }

//            Intent intent = new Intent(mContext, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
        }
    }

    /**
     * 判断app是否处于前台
     *
     * @return
     */
    public boolean isAppOnForeground() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = getApplicationContext().getPackageName();
        // 获取Android设备中所有正在运行的App
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) return false;
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            // The name of the process that this object is associated with.
            if (appProcess.processName.equals(packageName) && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }


}