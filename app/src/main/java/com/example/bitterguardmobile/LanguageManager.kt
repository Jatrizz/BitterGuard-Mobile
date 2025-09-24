package com.example.bitterguardmobile

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.*

object LanguageManager {
    private const val LANGUAGE_PREF = "language_pref"
    private const val LANGUAGE_KEY = "selected_language"
    
    fun setLanguage(context: Context, languageCode: String) {
        // Initialize TranslationManager if not already initialized
        TranslationManager.initialize(context)
        
        val locale = when (languageCode) {
            "tl" -> Locale("tl") // Filipino/Tagalog
            else -> Locale("en") // English
        }
        
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        context.createConfigurationContext(config)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        
        // Save language preference
        val sharedPrefs = context.getSharedPreferences(LANGUAGE_PREF, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(LANGUAGE_KEY, languageCode).apply()
        
        // Update TranslationManager
        TranslationManager.setCurrentLanguage(languageCode)
    }
    
    fun getLanguage(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(LANGUAGE_PREF, Context.MODE_PRIVATE)
        return sharedPrefs.getString(LANGUAGE_KEY, "en") ?: "en"
    }
    
    fun updateResources(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "tl" -> Locale("tl")
            else -> Locale("en")
        }
        
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
    
    fun getLocalizedResources(context: Context, languageCode: String): Resources {
        val locale = when (languageCode) {
            "tl" -> Locale("tl")
            else -> Locale("en")
        }
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config).resources
    }
    
    // New method to get translated string
    fun getString(context: Context, key: String, vararg args: Any): String {
        TranslationManager.initialize(context)
        return TranslationManager.getString(key, *args)
    }
} 