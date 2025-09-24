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
        
        // Apply language settings
        val languageCode = LanguageManager.getLanguage(this)
        LanguageManager.setLanguage(this, languageCode)
    }
} 