package com.example.myappdemo.ui.home;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.myappdemo.R;
import com.google.android.material.textfield.TextInputEditText;


public class LoginFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        Button submitBtn = view.findViewById(R.id.submit);
        submitBtn.setOnClickListener(v -> {
            TextInputEditText username = view.findViewById(R.id.username);
            Bundle args = new Bundle();
            args.putString("title", String.valueOf(username.getText()));

            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_welcomeFragment, args);
            // 也可直接跳转到 R.id.welcomeFragment
        });

        return view;
    }
}