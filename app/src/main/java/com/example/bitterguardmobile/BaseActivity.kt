package com.example.bitterguardmobile

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    
    override fun attachBaseContext(newBase: Context) {
        val languageCode = LanguageManager.getLanguage(newBase)
        val context = LanguageManager.updateResources(newBase, languageCode)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply theme settings
        ThemeManager.applySavedTheme(this)
        // Locale is already applied in attachBaseContext; avoid reapplying here to prevent
        // unnecessary activity recreations that can cause flicker during navigation.
    }

    override fun onResume() {
        super.onResume()
        // Suppress default activity transition animations to avoid flashing
        overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        // Suppress exit animation as well
        overridePendingTransition(0, 0)
    }
} 