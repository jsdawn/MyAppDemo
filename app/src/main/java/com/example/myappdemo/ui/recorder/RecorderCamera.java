package com.example.myappdemo.ui.recorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.myappdemo.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.october.lib.logger.LogUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressLint("RestrictedApi")
public class RecorderCamera extends Fragment {
    final String TAG = RecorderCamera.class.getSimpleName();
    View view;
    PreviewView viewFinder;
    Button videoCaptureButton;
    String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    ExecutorService cameraExecutor;
    ImageCapture imageCapture;
    VideoCapture videoCapture;
    Boolean isRecording = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.recorder_camera, container, false);
        viewFinder = view.findViewById(R.id.viewFinder);
        viewFinder.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        // 检查权限
        if (checkPermission(getActivity(), permissions)) {
            startCamera();
        } else {
            getLauncher(this::startCamera).launch(permissions);
        }

        view.findViewById(R.id.image_capture_button).setOnClickListener(v -> {
            takePhoto();
        });

        videoCaptureButton = view.findViewById(R.id.video_capture_button);
        videoCaptureButton.setOnClickListener(v -> {
            captureVideo();
        });

        cameraExecutor = Executors.newSingleThreadExecutor();

        return view;
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        File file = null;
        try {
            file = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(ImageCapture.OutputFileResults output) {
                String msg = "Photo capture succeeded: " + output.getSavedUri();
//                        Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                Log.d(TAG, msg);

            }

            @Override
            public void onError(ImageCaptureException error) {
                Log.e(TAG, "Photo capture failed: " + error.getMessage(), error);
            }
        });

    }

    private void captureVideo() {
        if (videoCapture == null) {
            return;
        }

        if (!checkPermission(getActivity(), permissions)) {
            return;
        }

        if (isRecording) {
            videoCapture.stopRecording();
            isRecording = false;
            return;
        }


        String name = "CameraX-recording-" + System.currentTimeMillis() + ".mp4";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

        File file = null;
        try {
            file = createVideoFilename();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture.OutputFileOptions.Builder(file).build();


        isRecording = true;
        videoCaptureButton.setText("Stop Capture");

        videoCapture.startRecording(outputFileOptions, ContextCompat.getMainExecutor(getActivity()), new VideoCapture.OnVideoSavedCallback() {
            @Override
            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                Log.d(TAG, "视频保存成功: " + outputFileResults.getSavedUri());
                isRecording = false;
                videoCaptureButton.setText("Start Capture");
            }

            @Override
            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                Log.e(TAG, "出现异常 message: " + message);
                isRecording = false;
                videoCaptureButton.setText("Start Capture");
            }
        });

    }


    private void startCamera() {
        // 创建 ProcessCameraProvider 的实例
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getActivity());
        // cameraProviderFuture 监听器
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 获取preview配置，并关联view
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 获取imageCapture配置
                imageCapture = new ImageCapture.Builder().build();

                // 获取videoCapture
//                recorder = new Recorder.Builder().build();
//                videoCapture = VideoCapture.withOutput(recorder);
                videoCapture = new VideoCapture.Builder().build();

                // 获取相机镜头
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

                try {
                    cameraProvider.unbindAll();
                    // 将 cameraSelector 和预览对象绑定到 cameraProvider
                    cameraProvider.bindToLifecycle(getActivity(), cameraSelector, preview, videoCapture);
                } catch (Exception e) {
                    Log.e(TAG, "CameraProvider Use case binding failed", e);
                }

            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, ContextCompat.getMainExecutor(getActivity()));
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String filename = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                filename,  /* 前缀 */
                ".jpg",         /* 后缀 */
                storageDir      /* 目录 */);
    }

    private File createVideoFilename() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String filename = "MP4_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        String usbPath = getUsbExternalPath();
        if (usbPath != null) {
            // 有u盘则使用u盘文件夹
            storageDir = new File(usbPath + File.separator + "videoRecorder");
        }

        if (!storageDir.exists()) {
            boolean bool = storageDir.mkdirs();
            if (!bool) {
                return null;
            }
        }

        return File.createTempFile(
                filename,  /* 前缀 */
                ".mp4",         /* 后缀 */
                storageDir      /* 目录 */);
    }

    public static boolean checkPermission(Activity activity, @NonNull String[] permissions) {
        List<String> noPermissions = new ArrayList<>();
        for (String permission : permissions) {
            // 检查权限
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                noPermissions.add(permission);
            }
        }

        if (!noPermissions.isEmpty()) {
            LogUtils.d("AudioRecorder", "noPermissions: " + noPermissions);
            return false;
        }
        return true;
    }

    public String getUsbPath() {
        try {
            StorageManager sm = (StorageManager) getContext().getSystemService(Context.STORAGE_SERVICE);
            Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths", null);
            String[] paths = (String[]) getVolumePathsMethod.invoke(sm, null);
            return paths.length <= 1 ? null : paths[1];
        } catch (Exception e) {
            Log.e(TAG, "---getUsbPath() failed" + e);
        }
        return null;
    }

    public String getUsbExternalPath() {
        File[] paths = ContextCompat.getExternalFilesDirs(getActivity(), null);
        // /storage/00D6-4AAA/Android/data/com.example.xxx/files
        return paths.length <= 1 ? null : paths[1].getAbsolutePath();
    }


    public interface Callback {
        void call();
    }

    // 申请权限回调
    public ActivityResultLauncher<String[]> getLauncher(Callback callback) {
        ActivityResultLauncher<String[]> launcher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Log.d(TAG, result.toString());
            boolean isPermisOk = true;
            for (String key : result.keySet()) {
                if (!Objects.equals(result.get(key), true)) {
                    isPermisOk = false;
                }
            }
            if (isPermisOk) {
                callback.call();
            } else {
                Toast.makeText(getActivity(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            }
        });
        return launcher;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();

    }
}