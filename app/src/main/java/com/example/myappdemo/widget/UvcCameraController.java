package com.example.myappdemo.widget;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcapp.ImageCapture;
import com.herohan.uvcapp.VideoCapture;
import com.serenegiant.usb.Size;
import com.serenegiant.widget.AspectRatioSurfaceView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UvcCameraController {
    private static final boolean DEBUG = true;
    final String TAG = UvcCameraController.class.getSimpleName();
    Context mContext;
    private ICameraHelper mCameraHelper;
    List<UsbDevice> devList = new ArrayList<>();
    private UsbDevice mUsbDevice;
    private AspectRatioSurfaceView mCameraViewMain;
    Boolean isRecording = false;

    public UvcCameraController(Context context) {
        mContext = context;
    }

    // 初始化相机
    public void setupCamera(AspectRatioSurfaceView aspectRatioSurfaceView) {
        if (aspectRatioSurfaceView != null) {
            mCameraViewMain = aspectRatioSurfaceView;
            initSurfaceView();
        }
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(mStateListener);
        }
    }

    private void initSurfaceView() {
        mCameraViewMain.setAspectRatio(1920, 1080);
        mCameraViewMain.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null) {
                    mCameraHelper.addSurface(holder.getSurface(), false);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null) {
                    mCameraHelper.removeSurface(holder.getSurface());
                }
            }
        });
    }

    // 检测并连接摄像头
    private final ICameraHelper.StateCallback mStateListener = new ICameraHelper.StateCallback() {
        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG)
                Log.d(TAG, "onAttach: " + "检测到摄像头设备 device:" + device.getProductName());
            devList.add(device); //把USB设备添加到集合
//            Log.d("DeviceName", device.getDeviceName());
//            Log.d("SerialNumber", device.getSerialNumber());
//            Log.d("ProductName", device.getProductName());
//            selectDevice(devList.get(0));//设置第一个USB设备
//            if(device.getProductName().equals("RGB-TS01"))//指定名称连接选择摄像头
            attachNewDevice(device); //选择设备

        }

        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            if (DEBUG)
                Log.d(TAG, "onDeviceOpen: 已打开摄像头设备 device: " + device.getProductName());
            mCameraHelper.openCamera(); // 连接摄像头设备
        }

        @Override
        public void onCameraOpen(UsbDevice device) {
            if (DEBUG) Log.d(TAG, "onCameraOpen: 连接摄像头成功");

            mCameraHelper.startPreview();

            List<Size> sizes = mCameraHelper.getSupportedSizeList();
            for (Size Size : sizes) {//打印所有摄像头支持的分辨率
                Log.i(TAG, "UvcSize Width: " + Size.width + " Height: " + Size.height);
            }

            // 添加相机预览
            if (mCameraViewMain != null) {
                Size size = mCameraHelper.getPreviewSize();
                if (size != null) {
                    int width = size.width;
                    int height = size.height;
                    //auto aspect ratio
                    mCameraViewMain.setAspectRatio(width, height);
                }
                mCameraHelper.addSurface(mCameraViewMain.getHolder().getSurface(), false);
            }
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraClose:");

            if (mCameraHelper != null && mCameraViewMain != null) {
                mCameraHelper.removeSurface(mCameraViewMain.getHolder().getSurface());
            }
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDeviceClose:");
        }

        @Override
        public void onDetach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDetach: 检测到摄像头拔出 device: " + device.getProductName());
            if (device.equals(mUsbDevice)) {
                mUsbDevice = null;
            }
        }

        @Override
        public void onCancel(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCancel: 检测到摄像头取消 device: " + device.getProductName());
            if (device.equals(mUsbDevice)) {
                mUsbDevice = null;
            }
        }

    };

    private void attachNewDevice(UsbDevice device) {
        if (devList.isEmpty()) return;
        //如果第一个集合等于当前设备
        if (devList.get(0).equals(device)) {
            mUsbDevice = device;
        }
        // 通过UsbDevice对象，尝试获取设备权限
        if (mCameraHelper != null && mUsbDevice != null) {
            mCameraHelper.selectDevice(device);
        }
    }

    // 开始录制
    public void startRecording() {
        if (mCameraHelper == null || mUsbDevice == null) return;

        mCameraHelper.setVideoCaptureConfig(
                mCameraHelper.getVideoCaptureConfig()
                        .setAudioCaptureEnable(true) // true:有音频;false:没有音频(默认为true)
                        .setAudioChannelCount(MediaRecorder.AudioSource.MIC)
                        .setVideoFrameRate(25)
                        .setIFrameInterval(1));

        Log.d(TAG, "startVideo: ");

//        String name = new SimpleDateFormat("yyyy-MM-dd HH;mm;ss", Locale.getDefault()).format(System.currentTimeMillis()) + ".mp4";
//        File outFile = new File(getFileDirs() + File.separator + name);

        File outFile = createVideoFilename(mContext);
        if (outFile == null) {
            return;
        }

        VideoCapture.OutputFileOptions build = new VideoCapture.OutputFileOptions
                .Builder(outFile)
                .build();

        mCameraHelper.startRecording(build, new VideoCapture.OnVideoCaptureCallback() {
            @Override
            public void onStart() {
                Log.d(TAG, "开始录制");
            }

            @Override
            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                Log.d(TAG, "视频保存成功: " + outputFileResults.getSavedUri());
                isRecording = false;
            }

            @Override
            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                Log.e(TAG, "出现异常 message: " + message);
                isRecording = false;
            }
        });
        isRecording = true;
    }

    public void stopRecording() {
        if (mCameraHelper == null) return;
        mCameraHelper.stopRecording();
    }

    public void takePhoto() {
        try {
            File file = new File(getFileDirs() + File.separator + System.currentTimeMillis() + ".jpg");
            ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(file).build();
            //进行拍照
            mCameraHelper.takePicture(options, new ImageCapture.OnImageCaptureCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                    String msg = "Photo capture succeeded: " + output.getSavedUri();
                    Log.d(TAG, msg);
                }

                @Override
                public void onError(int imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                    Log.e(TAG, "Photo capture failed: " + message);
                }
            });
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage(), e);
        }
    }


    public boolean getRecordingState() {
        return isRecording;
    }

    private String getFileDirs() {
        String FILE_PATH = Environment.getExternalStorageDirectory() + File.separator + "videolog";
        if (!new File(FILE_PATH).exists()) {
            new File(FILE_PATH).mkdirs();
        }
        return FILE_PATH;
    }

    private File createVideoFilename(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String filename = "MP4_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        String usbPath = getUsbExternalPath(context);
        if (usbPath != null) {
            // 有u盘则使用u盘文件夹
            storageDir = new File(usbPath + File.separator + "videoRecorder");
        }

        if (storageDir == null) {
            return null;
        }
        if (!storageDir.exists()) {
            boolean bool = storageDir.mkdirs();
            if (!bool) {
                return null;
            }
        }

        try {
            return File.createTempFile(
                    filename,  /* 前缀 */
                    ".mp4",         /* 后缀 */
                    storageDir      /* 目录 */);
        } catch (IOException ignored) {
        }
        return null;
    }

    public String getUsbExternalPath(Context context) {
        File[] paths = ContextCompat.getExternalFilesDirs(context, null);
        // /storage/00D6-4AAA/Android/data/com.example.xxx/files
        return paths.length <= 1 ? null : paths[1].getAbsolutePath();
    }
}
