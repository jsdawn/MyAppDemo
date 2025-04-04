package com.example.myappdemo.widget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.example.myappdemo.utils.ICallback;
import com.example.myappdemo.utils.MyUtils;
import com.google.common.util.concurrent.ListenableFuture;

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
    public final static String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    ExecutorService cameraExecutor;
    Preview previewCapture;
    ImageCapture imageCapture;
    VideoCapture<Recorder> videoCapture;
    ProcessCameraProvider cameraProvider;
    Boolean isRecording = false;

    Recording mRecording;

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
                if (surfaceProvider != null) {
                    previewCapture.setSurfaceProvider(surfaceProvider);
                }

                // 获取imageCapture配置
                imageCapture = new ImageCapture.Builder()
                        .build();

                // 1. 创建Recorder（关键步骤）
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build();
                // 2. 获取videoCapture
                videoCapture = VideoCapture.withOutput(recorder);

                // 获取相机镜头
                CameraSelector cameraSelector = new CameraSelector.Builder()
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

                    camera.getCameraInfo().getCameraState().observe((LifecycleOwner) mContext, new Observer<CameraState>() {
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

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        String filename = "MP4" + "_T" + timeStamp + "_N1001";
        File outFile = MyUtils.createVideoFile(filename + "_temp.mp4", mContext);
        if (outFile == null) {
            return;
        }

        FileOutputOptions outputOptions = new FileOutputOptions.Builder(outFile).build();

        PendingRecording pendingRecording = videoCapture.getOutput()
                .prepareRecording(mContext, outputOptions);

//        pendingRecording.withAudioEnabled(); // 启用录制音频（无则不录音）

        mRecording = pendingRecording.start(cameraExecutor, videoRecordEvent -> {
            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                isRecording = true;
                // 录制开始
                Log.d(TAG, "录制开始");
            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                VideoRecordEvent.Finalize event = (VideoRecordEvent.Finalize) videoRecordEvent;
                isRecording = false;
                if (!event.hasError()) {
                    // 录制成功
                    Log.d(TAG, "录制成功: " + event.getOutputResults().getOutputUri().getPath());
                    MyUtils.renameTempFile(event.getOutputResults().getOutputUri().getPath());
                } else {
                    if (mRecording != null) {
                        mRecording.close();
                        mRecording = null;
                    }
                    Log.e(TAG, "录制错误: " + event.getError());
                }
            }
        });


//        VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture.OutputFileOptions.Builder(outFile).build();
//
//        videoCapture.startRecording(outputFileOptions, cameraExecutor, new VideoCapture.OnVideoSavedCallback() {
//            @Override
//            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
//                Log.d(TAG, "视频保存成功: " + outputFileResults.getSavedUri());
//                isRecording = false;
//                callback.call();
//            }
//
//            @Override
//            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
//                Log.e(TAG, "出现异常 message: " + message);
//                isRecording = false;
//            }
//        });
//
//        isRecording = true;
    }

    public boolean getRecordingState() {
        return isRecording;
    }

    public void stopRecording() {
        if (videoCapture == null || mRecording == null) {
            return;
        }
//        videoCapture.stopRecording();
        mRecording.stop();
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

        imageCapture.takePicture(outputFileOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
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
        if (videoCapture != null && mRecording != null) {
//            videoCapture.stopRecording();
            mRecording.stop();
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
            return File.createTempFile(filename,  /* 前缀 */
                    ".jpg",         /* 后缀 */
                    storageDir      /* 目录 */);

        } catch (IOException ignored) {
        }
        return null;
    }


}
