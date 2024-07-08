package com.example.myappdemo.widget;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.text.format.DateFormat;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.october.lib.logger.LogUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AudioRecorder {
    public String TAG = "AudioRecorder";
    private MediaRecorder mediaRecorder;
    private File outFile;
    private String filename;

    // 录制路径：/storage/emulated/0/AudioRecorder
    File outDir = new File(Environment.getExternalStorageDirectory(), "AudioRecorder");

    public MediaRecorder getRecorder() {
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        File[] files = outDir.listFiles();
        if (files != null) {
            for (File file : files) {
                // 删除未正常结束的录制文件
                if (file.getName().contains("temp")) {
                    file.delete();
                }
            }
        }

        String dateStr = (String) DateFormat.format("yyyy_MM_dd_HH_mm_ss", new Date());
        filename = "audio_" + dateStr;
        outFile = new File(outDir, filename + "_temp.amr");

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
    public static boolean checkPermission(Activity activity) {
        String[] permissions = new String[]{"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
        List<String> noPermissions = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                noPermissions.add(permission);
            }
        }

        if (!noPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, noPermissions.toArray(new String[noPermissions.size()]), 101);
            LogUtils.d("AudioRecorder", "noPermissions: " + noPermissions);
            return false;
        }
        return true;
    }

    public void start() {
        if (mediaRecorder != null) {
            // 重置
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }

        try {
            mediaRecorder = getRecorder();
            // 准备录制，初始化 MediaRecorder 的各种状态，并根据配置的信息创建一个 MediaCodec 对象。
            mediaRecorder.prepare();
            // 开始录制，开始真正的录音工作。它会启动一个循环来从 MediaCodec 对象中取出编码后的音频数据，然后写入到指定的文件中。
            mediaRecorder.start();
            LogUtils.d(TAG, "开始录制：" + outFile.getAbsolutePath());
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        LogUtils.d(TAG, "结束录制");
        if (mediaRecorder == null) {
            return;
        }
        // 停止录制，会让循环停止，并等待剩余的数据全部写入文件
        mediaRecorder.stop();
        // 释放掉所有的资源，包括`MediaRecorder`对象自身。
        mediaRecorder.release();
        mediaRecorder = null;

        if (outFile.exists()) {
            File renameFile = new File(outDir, filename + ".amr");
            boolean bool = outFile.renameTo(renameFile);
            if (bool) {
                outFile = renameFile;
            }
        }
    }

    public void deleteLastFile() {
        if (outFile != null && outFile.exists()) {
            outFile.delete();
        }
    }
}
