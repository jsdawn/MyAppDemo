package com.example.myappdemo.ui.recorder;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.myappdemo.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.october.lib.logger.LogUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RecorderCamera extends Fragment {
    final String TAG = RecorderCamera.class.getSimpleName();
    View view;
    PreviewView previewView;
    String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    ExecutorService cameraExecutor;
    ImageCapture imageCapture;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.recorder_camera, container, false);
        previewView = view.findViewById(R.id.viewFinder);

        if (checkPermission(getActivity(), permissions)) {
            startCamera();
        } else {
            launcher.launch(permissions);
        }

        view.findViewById(R.id.image_capture_button).setOnClickListener(v -> {
            takePhoto();
        });
        view.findViewById(R.id.video_capture_button).setOnClickListener(v -> {
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

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(file).build();

        imageCapture.takePicture(outputFileOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults output) {
                        String msg = "Photo capture succeeded: " + output.getSavedUri();
//                        Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);

                    }

                    @Override
                    public void onError(ImageCaptureException error) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", error);
                    }
                }
        );

    }

    private void captureVideo() {
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
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 获取imageCapture配置
                imageCapture = new ImageCapture.Builder().build();


                // 获取相机镜头
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                try {
                    cameraProvider.unbindAll();
                    // 将 cameraSelector 和预览对象绑定到 cameraProvider
                    cameraProvider.bindToLifecycle(getActivity(), cameraSelector, preview, imageCapture);
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
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* 前缀 */
                ".jpg",         /* 后缀 */
                storageDir      /* 目录 */
        );
        return image;
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

    // 申请权限回调
    ActivityResultLauncher<String[]> launcher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        Log.d(TAG, result.toString());
        boolean isPermisOk = true;
        for (String key : result.keySet()) {
            if (!Objects.equals(result.get(key), true)) {
                isPermisOk = false;
            }
        }
        if (isPermisOk) {
            startCamera();
        } else {
            Toast.makeText(getActivity(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}