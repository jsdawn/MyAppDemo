package com.example.myappdemo.widget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.example.myappdemo.utils.ICallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.october.lib.logger.LogUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("RestrictedApi")
public class CameraXController {
    final String TAG = CameraXController.class.getSimpleName();
    Context mContext;
    public final static String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    ExecutorService cameraExecutor;
    Preview previewCapture;
    ImageCapture imageCapture;
    VideoCapture videoCapture;
    ProcessCameraProvider cameraProvider;
    Boolean isRecording = false;

    public CameraXController(Context context) {
        mContext = context;
    }

    public void setupCamera(Preview.SurfaceProvider surfaceProvider) {
        releseAll();
        // 创建单线程，仅供用例使用
        cameraExecutor = Executors.newSingleThreadExecutor();
        // 创建 ProcessCameraProvider 的实例
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(mContext);

        // cameraProviderFuture 监听器
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // 获取preview配置，并关联view
                previewCapture = new Preview.Builder().build();
                previewCapture.setSurfaceProvider(surfaceProvider);

                // 获取imageCapture配置
                imageCapture = new ImageCapture.Builder().build();

                // 获取videoCapture
                videoCapture = new VideoCapture.Builder()
                        .setAudioChannelCount(MediaRecorder.AudioSource.MIC)
                        .setBitRate(3 * 1024 * 1024)
                        .setVideoFrameRate(30)
                        .build();

                // 获取相机镜头
                CameraSelector cameraSelector = new CameraSelector
                        .Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();


                try {
                    cameraProvider.unbindAll();
                    // 将 cameraSelector 和预览对象绑定到 cameraProvider
                    Camera camera = cameraProvider.bindToLifecycle(
                            (LifecycleOwner) mContext,
                            cameraSelector,
                            imageCapture,
                            videoCapture,
                            previewCapture);

                    camera.getCameraInfo().getCameraState().observe(
                            (LifecycleOwner) mContext,
                            new Observer<CameraState>() {
                                @Override
                                public void onChanged(CameraState cameraState) {
                                    if (cameraState.getType() == CameraState.Type.OPEN) {
                                        Log.i(TAG, "Camera device is actually ready for use.");
                                    }
                                }
                            });

                } catch (Exception e) {
                    Log.e(TAG, "CameraProvider Use case binding failed", e);
                }

            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, ContextCompat.getMainExecutor(mContext));
    }

    @SuppressLint("MissingPermission")
    public void startRecording(ICallback callback) {
        if (videoCapture == null) {
            return;
        }

        File outFile = createVideoFilename(mContext);
        if (outFile == null) {
            return;
        }
        VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture
                .OutputFileOptions
                .Builder(outFile)
                .build();

        videoCapture.startRecording(
                outputFileOptions,
                cameraExecutor,
                new VideoCapture.OnVideoSavedCallback() {
                    @Override
                    public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                        Log.d(TAG, "视频保存成功: " + outputFileResults.getSavedUri());
                        isRecording = false;
                        callback.call();
                    }

                    @Override
                    public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                        Log.e(TAG, "出现异常 message: " + message);
                        isRecording = false;
                    }
                });

        isRecording = true;
    }

    public boolean getRecordingState() {
        return isRecording;
    }

    public void stopRecording() {
        if (videoCapture == null) {
            return;
        }
        videoCapture.stopRecording();
    }

    public void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        File outFile = createImageFile(mContext);
        if (outFile == null) {
            return;
        }
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(outFile).build();

        imageCapture.takePicture(outputFileOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults output) {
                        String msg = "Photo capture succeeded: " + output.getSavedUri();
                        Log.d(TAG, msg);

                    }

                    @Override
                    public void onError(ImageCaptureException error) {
                        Log.e(TAG, "Photo capture failed: " + error.getMessage(), error);
                    }
                });
    }

    public void releseAll() {
        if (videoCapture != null) {
            videoCapture.stopRecording();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider.shutdown();
            cameraProvider = null;
        }
    }

    private File createImageFile(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String filename = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile(
                    filename,  /* 前缀 */
                    ".jpg",         /* 后缀 */
                    storageDir      /* 目录 */);

        } catch (IOException ignored) {
        }
        return null;
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
