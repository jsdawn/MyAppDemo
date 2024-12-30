package com.example.myappdemo.ui.recorder;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.myappdemo.R;
import com.example.myappdemo.utils.ICallback;
import com.example.myappdemo.utils.MyUtils;
import com.example.myappdemo.widget.CameraXController;
import com.example.myappdemo.widget.UvcCameraController;
import com.serenegiant.widget.AspectRatioSurfaceView;

import java.util.Objects;

@SuppressLint("RestrictedApi")
public class RecorderCamera extends Fragment {
    final String TAG = RecorderCamera.class.getSimpleName();
    View view;
    PreviewView viewFinder;
    AspectRatioSurfaceView aspectRatioSurfaceView;
    Button videoCaptureButton;
    String[] permissions = CameraXController.PERMISSIONS;
    CameraXController cameraXController;

    UvcCameraController uvcCameraController;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.recorder_camera, container, false);

//        viewFinder = view.findViewById(R.id.viewFinder);
//        viewFinder.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        aspectRatioSurfaceView = view.findViewById(R.id.uvcViewFinder);

        // 检查权限
        if (MyUtils.checkPermission(getActivity(), permissions)) {
            startCamera();
        } else {
            getLauncher(this::startCamera).launch(permissions);
        }

        view.findViewById(R.id.image_capture_button)
                .setOnClickListener(v -> {
                    takePhoto();
                });

        videoCaptureButton = view.findViewById(R.id.video_capture_button);
        videoCaptureButton.setOnClickListener(v -> {
            captureVideo();
        });

        return view;
    }

    private void startCamera() {
//        cameraXController = new CameraXController(getContext());
//        cameraXController.setupCamera(viewFinder.getSurfaceProvider());
        uvcCameraController = new UvcCameraController(getContext());
        uvcCameraController.setupCamera(null);
    }

    private void takePhoto() {
//        cameraXController.takePhoto();
        uvcCameraController.takePhoto();
    }

    private void captureVideo() {
//        if (cameraXController.getRecordingState()) {
//            cameraXController.stopRecording();
//        } else {
//            videoCaptureButton.setText("Stop Capture");
//            cameraXController.startRecording(() -> {
//                videoCaptureButton.setText("Start Capture");
//            });
//        }

        if (uvcCameraController.getRecordingState()) {
            uvcCameraController.stopRecording();
        } else {
            videoCaptureButton.setText("Stop Capture");
            uvcCameraController.startRecording();
            videoCaptureButton.setText("Start Capture");
        }

    }


    // 申请权限回调
    public ActivityResultLauncher<String[]> getLauncher(ICallback callback) {
        ActivityResultLauncher<String[]> launcher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
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
        if (cameraXController != null) {
            cameraXController.releseAll();
        }

    }
}