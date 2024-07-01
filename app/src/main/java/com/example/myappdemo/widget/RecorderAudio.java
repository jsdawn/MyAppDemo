package com.example.myappdemo.widget;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecorderAudio {
    public String TAG = "RecorderAudio";
    public final int RECORDER_SAMPLERATE = 44100;
    public final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private MediaRecorder mediaRecorder;
    private File outFile;
    File outDir = new File(Environment.getExternalStorageDirectory(), AUDIO_RECORDER_FOLDER);

    public MediaRecorder getRecorder() {
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        outFile = new File(outDir, System.currentTimeMillis() + ".amr");
        Log.d(TAG, "录制路径：" + outFile.getAbsolutePath());

        // 创建一个MediaRecorder对象
        MediaRecorder recorder = new MediaRecorder();

        // 设置音频源为麦克风
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 设置音频输出格式
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        // 设置音频编码格式
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        // 设置输出文件路径
        recorder.setOutputFile(outFile.getAbsolutePath());

        return recorder;
    }

    // 检查所需权限
    public boolean checkPermission(Activity activity) {
        String[] permissions = new String[]{"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
        List<String> noPermissions = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                noPermissions.add(permission);
            }
        }

        if (!noPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, noPermissions.toArray(new String[noPermissions.size()]), 101);
            return false;
        }
        return true;
    }

    public void start() {
        if (mediaRecorder == null) {
            mediaRecorder = getRecorder();
        }
        try {
            // 准备录制，初始化 MediaRecorder 的各种状态，并根据配置的信息创建一个 MediaCodec 对象。
            mediaRecorder.prepare();
            // 开始录制，开始真正的录音工作。它会启动一个循环来从 MediaCodec 对象中取出编码后的音频数据，然后写入到指定的文件中。
            mediaRecorder.start();
            Log.d(TAG, "开始录制");
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        Log.d(TAG, "结束录制");
        if (mediaRecorder == null) {
            return;
        }
        // 停止录制，会让循环停止，并等待剩余的数据全部写入文件
        mediaRecorder.stop();
        // 释放掉所有的资源，包括`MediaRecorder`对象自身。
        mediaRecorder.release();
        mediaRecorder = null;
        Log.d(TAG, String.valueOf(outFile.exists()));
        if (outFile.exists()) {
            boolean bool = outFile.renameTo(new File(outDir, "录制已经完成001.amr"));
            if (bool) {
                Log.d(TAG, "改名完成");
            }
        }
    }
}
