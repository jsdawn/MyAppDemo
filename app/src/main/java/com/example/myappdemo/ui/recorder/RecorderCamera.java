package com.example.myappdemo.ui.recorder;

import android.annotation.SuppressLint;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.Fragment;

import android.util.Log;
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

import java.util.Locale;
import java.util.Objects;

@SuppressLint("RestrictedApi")
public class RecorderCamera extends Fragment {
    final String TAG = RecorderCamera.class.getSimpleName();
    View view;
    PreviewView previewView;
    AspectRatioSurfaceView aspectRatioSurfaceView;
    Button videoCaptureButton;
    String[] permissions = CameraXController.PERMISSIONS;
    CameraXController cameraXController;

    UvcCameraController uvcCameraController;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("onCreate", "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.e("onCreateView", "onCreateView");
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.recorder_camera, container, false);

        previewView = view.findViewById(R.id.viewFinder);
        aspectRatioSurfaceView = view.findViewById(R.id.uvcViewFinder);

        // 检查权限
        if (MyUtils.checkPermission(getActivity(), permissions)) {
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

        return view;
    }

    private void startCamera() {
        UsbDevice usbDevice = MyUtils.getUsbCameraDevice(getContext());
        if (usbDevice != null) {
            aspectRatioSurfaceView.setVisibility(View.VISIBLE);
            previewView.setVisibility(View.GONE);
            uvcCameraController = new UvcCameraController(getContext());
            uvcCameraController.setupCamera(aspectRatioSurfaceView);
            Log.e("usbDevice", usbDevice.getProductName());
        } else {
            aspectRatioSurfaceView.setVisibility(View.GONE);
            previewView.setVisibility(View.VISIBLE);
            cameraXController = new CameraXController(getContext());
            cameraXController.setupCamera(previewView.getSurfaceProvider());
            Log.e("usbDevice", "没有usb摄像头");
        }


    }

    private void takePhoto() {
        if (uvcCameraController != null) {
            uvcCameraController.takePhoto();
            return;
        }
        cameraXController.takePhoto();
    }

    private void captureVideo() {
        if (uvcCameraController != null) {
            if (uvcCameraController.getRecordingState()) {
                uvcCameraController.stopRecording();
                videoCaptureButton.setText("Start Capture");
            } else {
                uvcCameraController.startRecording();
                videoCaptureButton.setText("Stop Capture");
            }
            return;
        }

        if (cameraXController.getRecordingState()) {
            cameraXController.stopRecording();
            videoCaptureButton.setText("Start Capture");
        } else {
            cameraXController.startRecording(() -> {
                videoCaptureButton.setText("Stop Capture");
            });
        }

    }


    // 申请权限回调
    public ActivityResultLauncher<String[]> getLauncher(ICallback callback) {
        ActivityResultLauncher<String[]> launcher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
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
        Log.e("onDestroy", "onDestroy");
        if (uvcCameraController != null) {
            uvcCameraController.releseAll();
        }
        if (cameraXController != null) {
            cameraXController.releseAll();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e("onDestroyView", "onDestroyView");
        if (uvcCameraController != null) {
            uvcCameraController.releseAll();
        }
        if (cameraXController != null) {
            cameraXController.releseAll();
        }
    }
}