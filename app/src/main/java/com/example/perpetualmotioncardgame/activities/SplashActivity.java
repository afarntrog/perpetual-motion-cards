package com.example.perpetualmotioncardgame.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Night mode. Handle older sdks as well
        AppCompatDelegate.setDefaultNightMode(
                Build.VERSION.SDK_INT < 28 ? AppCompatDelegate.MODE_NIGHT_AUTO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
//        After setting night mode immediatly launch the main activity class
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();

    }

}
