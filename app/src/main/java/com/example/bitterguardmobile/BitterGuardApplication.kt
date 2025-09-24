package com.example.bitterguardmobile

import android.app.Application

class BitterGuardApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize theme on app start
        ThemeManager.applySavedTheme(this)
        
        // Initialize other managers
        AppConfig.initialize(this)
        SettingsManager.initialize(this)
    }
}
