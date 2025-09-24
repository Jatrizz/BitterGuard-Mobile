package com.example.bitterguardmobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // Go to OnboardingActivity
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        }, 2500);
    }
}
