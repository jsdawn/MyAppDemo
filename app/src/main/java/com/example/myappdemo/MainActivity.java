package com.example.myappdemo;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.ActivityManager;
import android.content.ComponentName;
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

import com.october.lib.logger.LogLevel;
import com.october.lib.logger.LogUtils;
import com.october.lib.logger.Logger;
import com.october.lib.logger.compress.BaseLogCompressStrategy;
import com.october.lib.logger.compress.ZipLogCompressStrategy;
import com.october.lib.logger.disk.BaseLogDiskStrategy;
import com.october.lib.logger.disk.TimeLogDiskStrategyImpl;
import com.october.lib.logger.format.LogTxtDefaultFormatStrategy;
import com.october.lib.logger.print.BaseLogTxtPrinter;
import com.october.lib.logger.print.LogTxtDefaultPrinter;
import com.october.lib.logger.print.LogcatDefaultPrinter;
import com.october.lib.logger.crash.DefaultCrashStrategyImpl;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
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

        //----------------------  测试按钮
        Button btnEnd = findViewById(R.id.button);
        btnEnd.setOnClickListener(v -> {
            LogUtils.w(TAG, "setOnClickListener warning");
//            Integer.parseInt("ssss");
            // openSystemSettings();
        });
        //----------------------

        //----------------------  注册keep守护服务
        /*Intent keepIntent = new Intent(this, KeepOne.class);
        startService(keepIntent);*/
        //----------------------

        //----------------------- 初始化日志
        initLogger();
        //-----------------------

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
        ServiceMangerUtils.isDebugMode = false;
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
        }
    }

    /**
     * 判断app是否处于前台
     *
     * @return
     */
    public boolean isAppOnForeground() {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = getApplicationContext().getPackageName();
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

    public void openSystemSettings() {
        Intent intent = new Intent("/");
        ComponentName cm = new ComponentName("com.android.settings", "com.android.settings.Settings");
        intent.setComponent(cm);
        intent.setAction("android.intent.action.VIEW");
        startActivity(intent);
        ServiceMangerUtils.isDebugMode = true;
        Log.d("KeepServices1", "hhh:" + ServiceMangerUtils.isDebugMode);
    }

    // 初始化日志
    public void initLogger() {
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

}