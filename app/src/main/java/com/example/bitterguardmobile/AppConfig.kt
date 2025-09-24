package com.example.bitterguardmobile

import android.content.Context
import android.content.SharedPreferences

/**
 * Application Configuration Manager
 * Centralizes all configurable values to avoid hardcoding
 */
object AppConfig {
    
    // Configuration keys
    private const val PREF_NAME = "app_config"
    private const val KEY_API_BASE_URL = "api_base_url"
    private const val KEY_SUPABASE_PROJECT_URL = "supabase_project_url"
    private const val KEY_DEBUG_MODE = "debug_mode"
    private const val KEY_MODEL_VERSION = "model_version"
    private const val KEY_MAX_HISTORY_SIZE = "max_history_size"
    private const val KEY_CONFIDENCE_THRESHOLD = "confidence_threshold"
    private const val KEY_IMAGE_QUALITY = "image_quality"
    private const val KEY_SUPPORT_EMAIL = "support_email"
    private const val KEY_SUPPORT_PHONE = "support_phone"
    private const val KEY_WEBSITE_URL = "website_url"
    private const val KEY_APP_VERSION = "app_version"
    private const val KEY_COMPANY_NAME = "company_name"
    
    // Default values
    private const val DEFAULT_API_BASE_URL = "https://api.bitterguard.com"
    private const val DEFAULT_FIREBASE_PROJECT_ID = "bitterguard-50c58"
    private const val DEFAULT_DEBUG_MODE = true
    private const val DEFAULT_MODEL_VERSION = "1.0.0"
    private const val DEFAULT_MAX_HISTORY_SIZE = 100
    private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.5f
    private const val DEFAULT_IMAGE_QUALITY = 85
    private const val DEFAULT_SUPPORT_EMAIL = "support@bitterguard.com"
    private const val DEFAULT_SUPPORT_PHONE = "+63 912 345 6789"
    private const val DEFAULT_WEBSITE_URL = "www.bitterguard.com"
    private const val DEFAULT_APP_VERSION = "1.0.0"
    private const val DEFAULT_COMPANY_NAME = "BitterGuard Mobile"
    
    // Test user credentials (should be configurable)
    private const val KEY_TEST_EMAIL = "test_email"
    private const val KEY_TEST_PASSWORD = "test_password"
    private const val DEFAULT_TEST_EMAIL = "test@bitterguard.com"
    private const val DEFAULT_TEST_PASSWORD = "123456"
    
    // Disease detection classes (configurable)
    private const val KEY_DISEASE_CLASSES = "disease_classes"
    private val DEFAULT_DISEASE_CLASSES = arrayOf(
        "Downey Mildew",
        "Fresh Leaf", 
        "Fusarium Wilt",
        "Mosaic Virus"
    )
    
    // Model configuration
    private const val KEY_MODEL_FILENAME = "model_filename"
    private const val KEY_INPUT_SIZE = "input_size"
    private const val DEFAULT_MODEL_FILENAME = "best.ptl"
    private const val DEFAULT_INPUT_SIZE = 224
    
    private lateinit var sharedPreferences: SharedPreferences
    
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    // API Configuration
    fun getApiBaseUrl(): String = sharedPreferences.getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL) ?: DEFAULT_API_BASE_URL
    fun setApiBaseUrl(url: String) = sharedPreferences.edit().putString(KEY_API_BASE_URL, url).apply()
    
    // Supabase Configuration
    fun getSupabaseProjectUrl(): String = sharedPreferences.getString(KEY_SUPABASE_PROJECT_URL, "https://bihiognjjtaztmepehtv.supabase.co") ?: "https://bihiognjjtaztmepehtv.supabase.co"
    fun setSupabaseProjectUrl(projectUrl: String) = sharedPreferences.edit().putString(KEY_SUPABASE_PROJECT_URL, projectUrl).apply()
    
    // Debug Configuration
    fun isDebugMode(): Boolean = sharedPreferences.getBoolean(KEY_DEBUG_MODE, DEFAULT_DEBUG_MODE)
    fun setDebugMode(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_DEBUG_MODE, enabled).apply()
    
    // Model Configuration
    fun getModelVersion(): String = sharedPreferences.getString(KEY_MODEL_VERSION, DEFAULT_MODEL_VERSION) ?: DEFAULT_MODEL_VERSION
    fun setModelVersion(version: String) = sharedPreferences.edit().putString(KEY_MODEL_VERSION, version).apply()
    
    fun getModelFilename(): String = sharedPreferences.getString(KEY_MODEL_FILENAME, DEFAULT_MODEL_FILENAME) ?: DEFAULT_MODEL_FILENAME
    fun setModelFilename(filename: String) = sharedPreferences.edit().putString(KEY_MODEL_FILENAME, filename).apply()
    
    fun getInputSize(): Int = sharedPreferences.getInt(KEY_INPUT_SIZE, DEFAULT_INPUT_SIZE)
    fun setInputSize(size: Int) = sharedPreferences.edit().putInt(KEY_INPUT_SIZE, size).apply()
    
    // History Configuration
    fun getMaxHistorySize(): Int = sharedPreferences.getInt(KEY_MAX_HISTORY_SIZE, DEFAULT_MAX_HISTORY_SIZE)
    fun setMaxHistorySize(size: Int) = sharedPreferences.edit().putInt(KEY_MAX_HISTORY_SIZE, size).apply()
    
    // Detection Configuration
    fun getConfidenceThreshold(): Float = sharedPreferences.getFloat(KEY_CONFIDENCE_THRESHOLD, DEFAULT_CONFIDENCE_THRESHOLD)
    fun setConfidenceThreshold(threshold: Float) = sharedPreferences.edit().putFloat(KEY_CONFIDENCE_THRESHOLD, threshold).apply()
    
    fun getDiseaseClasses(): Array<String> {
        val classesString = sharedPreferences.getString(KEY_DISEASE_CLASSES, DEFAULT_DISEASE_CLASSES.joinToString(","))
        return classesString?.split(",")?.toTypedArray() ?: DEFAULT_DISEASE_CLASSES
    }
    fun setDiseaseClasses(classes: Array<String>) = sharedPreferences.edit().putString(KEY_DISEASE_CLASSES, classes.joinToString(",")).apply()
    
    // Image Configuration
    fun getImageQuality(): Int = sharedPreferences.getInt(KEY_IMAGE_QUALITY, DEFAULT_IMAGE_QUALITY)
    fun setImageQuality(quality: Int) = sharedPreferences.edit().putInt(KEY_IMAGE_QUALITY, quality).apply()
    
    // Contact Information
    fun getSupportEmail(): String = sharedPreferences.getString(KEY_SUPPORT_EMAIL, DEFAULT_SUPPORT_EMAIL) ?: DEFAULT_SUPPORT_EMAIL
    fun setSupportEmail(email: String) = sharedPreferences.edit().putString(KEY_SUPPORT_EMAIL, email).apply()
    
    fun getSupportPhone(): String = sharedPreferences.getString(KEY_SUPPORT_PHONE, DEFAULT_SUPPORT_PHONE) ?: DEFAULT_SUPPORT_PHONE
    fun setSupportPhone(phone: String) = sharedPreferences.edit().putString(KEY_SUPPORT_PHONE, phone).apply()
    
    fun getWebsiteUrl(): String = sharedPreferences.getString(KEY_WEBSITE_URL, DEFAULT_WEBSITE_URL) ?: DEFAULT_WEBSITE_URL
    fun setWebsiteUrl(url: String) = sharedPreferences.edit().putString(KEY_WEBSITE_URL, url).apply()
    
    // App Information
    fun getAppVersion(): String = sharedPreferences.getString(KEY_APP_VERSION, DEFAULT_APP_VERSION) ?: DEFAULT_APP_VERSION
    fun setAppVersion(version: String) = sharedPreferences.edit().putString(KEY_APP_VERSION, version).apply()
    
    fun getCompanyName(): String = sharedPreferences.getString(KEY_COMPANY_NAME, DEFAULT_COMPANY_NAME) ?: DEFAULT_COMPANY_NAME
    fun setCompanyName(name: String) = sharedPreferences.edit().putString(KEY_COMPANY_NAME, name).apply()
    
    // Test User Configuration
    fun getTestEmail(): String = sharedPreferences.getString(KEY_TEST_EMAIL, DEFAULT_TEST_EMAIL) ?: DEFAULT_TEST_EMAIL
    fun setTestEmail(email: String) = sharedPreferences.edit().putString(KEY_TEST_EMAIL, email).apply()
    
    fun getTestPassword(): String = sharedPreferences.getString(KEY_TEST_PASSWORD, DEFAULT_TEST_PASSWORD) ?: DEFAULT_TEST_PASSWORD
    fun setTestPassword(password: String) = sharedPreferences.edit().putString(KEY_TEST_PASSWORD, password).apply()
    
    // Reset to defaults
    fun resetToDefaults() {
        sharedPreferences.edit().clear().apply()
    }
    
    // Get all configuration as a map (for debugging)
    fun getAllConfig(): Map<String, Any> {
        return mapOf(
            "api_base_url" to getApiBaseUrl(),
            "supabase_project_url" to getSupabaseProjectUrl(),
            "debug_mode" to isDebugMode(),
            "model_version" to getModelVersion(),
            "max_history_size" to getMaxHistorySize(),
            "confidence_threshold" to getConfidenceThreshold(),
            "image_quality" to getImageQuality(),
            "support_email" to getSupportEmail(),
            "support_phone" to getSupportPhone(),
            "website_url" to getWebsiteUrl(),
            "app_version" to getAppVersion(),
            "company_name" to getCompanyName(),
            "test_email" to getTestEmail(),
            "disease_classes" to getDiseaseClasses().joinToString(",")
        )
    }
}
