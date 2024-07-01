package com.example.myappdemo.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myappdemo.R;
import com.google.android.material.navigation.NavigationView;
import com.october.lib.logger.LogUtils;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    DrawerLayout drawerLayout;
    Toolbar toolbar;
    NavigationView navigationView; // 菜单
    NavController navController; // 导航控制器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LogUtils.d("MainActivity", "onCreate MainActivity");

        drawerLayout = findViewById(R.id.drawerLayout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.navigation_view);
        // fragment宿主，导航控制器
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentHostView);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }


        toolbar.setNavigationOnClickListener(v -> {
            drawerLayout.open();
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_item1) {
                navController.navigate(R.id.loginFragment);
            } else if (item.getItemId() == R.id.nav_item2) {
                Bundle args = new Bundle();
                args.putString("title", "菜单跳转");
                navController.navigate(R.id.welcomeFragment, args);
            }
            drawerLayout.close();
            return true;
        });
    }


}