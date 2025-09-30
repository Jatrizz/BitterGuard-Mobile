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
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean shown = prefs.getBoolean("onboarding_shown", false);
            Class<?> next = shown ? MainActivity.class : OnboardingActivity.class;
            startActivity(new Intent(this, next));
            finish();
        }, 2500);
    }
}
