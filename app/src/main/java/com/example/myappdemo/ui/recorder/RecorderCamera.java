package com.example.myappdemo.ui.recorder;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
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
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.RecordingStats;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
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
    Button videoCaptureButton;
    String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    ExecutorService cameraExecutor;
    ImageCapture imageCapture;
    VideoCapture<Recorder> videoCapture;
    Recording recording;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.recorder_camera, container, false);
        previewView = view.findViewById(R.id.viewFinder);
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

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
                        Log.e(TAG, "Photo capture failed: " + error.getMessage(), error);
                    }
                }
        );

    }

    private void captureVideo() {
        if (videoCapture == null) {
            return;
        }

        if (!checkPermission(getActivity(), permissions)) {
            return;
        }

        videoCaptureButton.setEnabled(false);
        if (recording != null) {
            // 存在录制，则结束该录制
            recording.stop();
            recording = null;
            return;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, getVideoFilename());
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

        // Create MediaStoreOutputOptions for our recorder
        MediaStoreOutputOptions outputOptions = new MediaStoreOutputOptions
                .Builder(getActivity().getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build();

        // 2. Configure Recorder and Start recording to the mediaStoreOutput.
        Recorder recorder = videoCapture.getOutput();
        recording = recorder
                .prepareRecording(getActivity(), outputOptions)
                .withAudioEnabled()
                .start(cameraExecutor, videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                        // Handle the start of a new active recording
                        videoCaptureButton.setText("Stop Capture");
                        videoCaptureButton.setEnabled(true);
                    } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        // 录制完成
                        VideoRecordEvent.Finalize finalizeEvent =
                                (VideoRecordEvent.Finalize) videoRecordEvent;
                        // Handles a finalize event for the active recording, checking Finalize.getError()
                        if (!finalizeEvent.hasError()) {
                            String msg = "Video capture succeeded: " + finalizeEvent.getOutputResults().getOutputUri();
//                        Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, msg);
                        } else {
                            if (recording != null) {
                                recording.close();
                            }
                            Log.e(TAG, "Video capture ends with error: " + finalizeEvent.getError());
                        }
                        videoCaptureButton.setText("Start Capture");
                        videoCaptureButton.setEnabled(true);
                    }
                    // All events, including VideoRecordEvent.Status, contain RecordingStats.
                    RecordingStats recordingStats = videoRecordEvent.getRecordingStats();
                    Log.d(TAG, "recordingStats：==》\n" + recordingStats.toString());
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
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 获取imageCapture配置
                imageCapture = new ImageCapture.Builder().build();

                // 获取videoCapture
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);


                // 获取相机镜头
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                try {
                    cameraProvider.unbindAll();
                    // 将 cameraSelector 和预览对象绑定到 cameraProvider
                    cameraProvider.bindToLifecycle(getActivity(), cameraSelector, preview, imageCapture, videoCapture);
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
                storageDir      /* 目录 */
        );
    }

    private String getVideoFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "MP4_" + timeStamp + "_" + System.currentTimeMillis();
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