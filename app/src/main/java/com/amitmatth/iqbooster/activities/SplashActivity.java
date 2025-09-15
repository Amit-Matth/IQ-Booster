package com.amitmatth.iqbooster.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.databinding.ActivitySplashBinding;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private static final int SPLASH_TIME_OUT = 3000;
    Intent intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        Animation iconAnim = AnimationUtils.loadAnimation(this, R.anim.splash_iconimg_anim);
        Animation boosterAnim = AnimationUtils.loadAnimation(this, R.anim.splash_boosterimg_anim);

        binding.iconimg.startAnimation(iconAnim);
        binding.boosterimg.startAnimation(boosterAnim);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
            boolean check = prefs.getBoolean("flag", false);

            if (check) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                intent = new Intent(SplashActivity.this, SignInActivity.class);
                startActivity(intent);
                finish();
            }

        }, SPLASH_TIME_OUT);
    }
}