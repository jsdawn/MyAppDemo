package com.example.myappdemo.ui.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.myappdemo.R;


public class AudioMonitor extends Fragment {

    View view;

    private final BroadcastReceiver volumeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("AudioMonitor", intent.getAction().toString());
            setVolumeText();

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.monitor_audio, container, false);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        getActivity().registerReceiver(volumeChangeReceiver, filter);

        setVolumeText();

        return view;
    }


    public void setVolumeText() {
        // 获取AudioManager服务
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        // 获取当前音量大小
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        TextView textView = view.findViewById(R.id.volume_text);
        textView.setText("音量：" + currentVolume);
    }

}