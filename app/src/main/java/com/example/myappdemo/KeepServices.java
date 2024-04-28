package com.example.myappdemo;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
//import android.app.smdt.SmdtManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.List;


public class KeepServices extends Service {

    private NotificationManager notificationManager;
    private String notificationId = "serviceid";
    private String notificationName = "servicename";
    Context context;
    //    private SmdtManager smdtManager;
    // 用于保存服务的唯一实例
    private static KeepServices instance;
    private static final String keepPackagename = "com.example.myappdemo";//com.qtapp

    public static KeepServices getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        instance = this;
//        smdtManager = SmdtManager.create(this);
//        if (smdtManager != null) {
//            smdtManager.smdtSetGpioDirection(2, 0, 1);
//        }


        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //创建NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationId, notificationName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        startForeground(1, getNotification());
        Log.d("KeepServices", "startForeground");
    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this).setSmallIcon(R.mipmap.ic_launcher).setContentTitle("title").setContentText("text");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(notificationId);
        }
        Notification notification = builder.build();
        return notification;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (instance != null && this != instance) {
            // 如果已经有一个实例在运行，则停止当前服务
            Log.d("KeepServices", "Another instance is already running. Stopping service...");
            stopSelf();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // Log.e("GPIO:","0");
                        // Log.d("KeepServices","====保活服务===数据支持===="+Keep());
                        if (!Keep()) {
                            // Log.d("KeepServices","====拉起===com.qtapp");
                            openAppWithPackageName(keepPackagename);//启动应用
                        }
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();

//        startServiceOne();

        return super.onStartCommand(intent, flags, startId);
//        return START_REDELIVER_INTENT;
    }

    private void startServiceOne() {
        boolean b = ServiceMangerUtils.isServiceWorked(KeepServices.this, "KeepOne");
        if (!b) {
            Intent service = new Intent(KeepServices.this, KeepOne.class);
            startService(service);
            Log.d("KeepServices", "Start ServiceOne");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    // 判断是否在前台
    private boolean Keep() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();

        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (String activeProcess : processInfo.pkgList) {
                    if (activeProcess.equals(keepPackagename)) {
                        // 应用程序正在运行
                        return true;
                    }
                }
            }
        }
// 应用程序不在运行
        return false;
    }

    public void openAppWithPackageName(String packagename) {
// 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        PackageInfo packageinfo = null;
        try {
            packageinfo = getPackageManager().getPackageInfo(packagename, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            return;
        }
        // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(packageinfo.packageName);
        // 通过getPackageManager()的queryIntentActivities方法遍历
        List<ResolveInfo> resolveinfoList = getPackageManager().queryIntentActivities(resolveIntent, 0);
        if (!resolveinfoList.iterator().hasNext()) {
            return;
        }
        ResolveInfo resolveinfo = resolveinfoList.iterator().next();
        if (resolveinfo != null) {
            // packagename = 参数packname
            String packageName = resolveinfo.activityInfo.packageName;
            // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
            String className = resolveinfo.activityInfo.name; // LAUNCHER Intent
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);//重点是加这个
            // 设置ComponentName参数1:packagename参数2:MainActivity路径
            ComponentName cn = new ComponentName(packageName, className);
            intent.setComponent(cn);
            startActivity(intent);
        }
    }

    @SuppressLint({"HardwareIds", "MissingPermission"})
    public String readSIMCard() {


        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        StringBuffer sb = new StringBuffer();
        switch (tm.getSimState()) { //getSimState()取得sim的状态 有下面6中状态
            case TelephonyManager.SIM_STATE_ABSENT:
                sb.append("卡状态：无卡");
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                sb.append("卡状态：未知状态");
                break;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                sb.append("卡状态：需要NetworkPIN解锁");
                break;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                sb.append("卡状态：需要PIN解锁");
                break;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                sb.append("卡状态：需要PUK解锁");
                break;
            case TelephonyManager.SIM_STATE_READY:
                sb.append("卡状态：良好");
                break;
        }

//        if(tm.getLine1Number()!=null){
//            sb.append("@SIM卡号码：" + tm.getLine1Number().toString());
//        }else{
//            sb.append("@无法取得SIM卡号");
//        }

        if (tm.getSimOperator().equals("")) {
            sb.append("@无法取得供货商代码");
        } else {
            sb.append("@卡供货商代码：" + tm.getSimOperator().toString());
        }

        if (tm.getSimOperatorName().equals("")) {
            sb.append("@无法取得供货商");
        } else {
            sb.append("@供货商：" + tm.getSimOperatorName().toString());
        }

        if (tm.getSimCountryIso().equals("")) {
            sb.append("@无法取得国籍");
        } else {
            sb.append("@国籍：" + tm.getSimCountryIso().toString());
        }

        if (tm.getNetworkOperator().equals("")) {
            sb.append("@无法取得网络运营商");
        } else {
            sb.append("@网络运营商：" + tm.getNetworkOperator());
        }
        if (tm.getNetworkOperatorName().equals("")) {
            sb.append("@无法取得网络运营商名称");
        } else {
            sb.append("@网络运营商名称：" + tm.getNetworkOperatorName());
        }
        if (tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            sb.append("@无法取得网络类型");
        } else {
            sb.append("@网络类型：" + tm.getNetworkType());
        }
        return sb.toString();
    }
}