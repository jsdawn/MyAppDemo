package com.example.myappdemo.ui.recorder;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.myappdemo.R;
import com.example.myappdemo.widget.AudioRecorder;

public class RecorderAudio extends Fragment {

    AudioRecorder audioRecorder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioRecorder = new AudioRecorder();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.recorder_audio, container, false);

        Button startBtn = view.findViewById(R.id.start);
        startBtn.setOnClickListener(v -> {
            if (!AudioRecorder.checkPermission(getActivity())) {
                return;
            }
            audioRecorder.start();
        });

        Button endBtn = view.findViewById(R.id.end);
        endBtn.setOnClickListener(v -> {
            audioRecorder.stop();
        });

        Button delBtn = view.findViewById(R.id.del);
        delBtn.setOnClickListener(v -> {
            audioRecorder.deleteLastFile();
        });

        return view;
    }
}