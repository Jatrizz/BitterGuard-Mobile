package com.example.bitterguardmobile

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val THEME_PREF = "theme_pref"
    private const val THEME_KEY = "selected_theme"
    
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    
    fun setTheme(context: Context, theme: String) {
        val sharedPrefs = context.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(THEME_KEY, theme).apply()
        
        applyTheme(theme)
        
        // Restart current activity to apply theme changes smoothly
        if (context is AppCompatActivity) {
            context.recreate()
        }
    }
    
    fun getCurrentTheme(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE)
        return sharedPrefs.getString(THEME_KEY, THEME_LIGHT) ?: THEME_LIGHT
    }
    
    fun applyTheme(theme: String) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    fun applySavedTheme(context: Context) {
        val savedTheme = getCurrentTheme(context)
        applyTheme(savedTheme)
    }
    
    fun isDarkTheme(context: Context): Boolean {
        return getCurrentTheme(context) == THEME_DARK
    }
}
