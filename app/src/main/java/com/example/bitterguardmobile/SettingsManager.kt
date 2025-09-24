package com.example.bitterguardmobile

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Settings Manager - Handles all app settings and preferences
 */
object SettingsManager {
    
    private const val TAG = "SettingsManager"
    private const val PREF_NAME = "app_settings"
    
    // Privacy Settings Keys
    private const val KEY_LOCATION_DATA = "location_data_enabled"
    private const val KEY_ANALYTICS_DATA = "analytics_data_enabled"
    private const val KEY_PROFILE_VISIBILITY = "profile_visibility_enabled"
    private const val KEY_SCAN_HISTORY_SHARING = "scan_history_sharing_enabled"
    private const val KEY_DATA_RETENTION = "data_retention_enabled"
    
    // Notification Settings Keys
    private const val KEY_PUSH_NOTIFICATIONS = "push_notifications_enabled"
    private const val KEY_SOUND_NOTIFICATIONS = "sound_notifications_enabled"
    private const val KEY_VIBRATION = "vibration_enabled"
    private const val KEY_SCAN_COMPLETE = "scan_complete_notifications"
    private const val KEY_DISEASE_ALERTS = "disease_alerts_enabled"
    private const val KEY_LOW_CONFIDENCE_ALERTS = "low_confidence_alerts_enabled"
    private const val KEY_FORUM_REPLIES = "forum_replies_enabled"
    private const val KEY_COMMUNITY_UPDATES = "community_updates_enabled"
    private const val KEY_EXPERT_TIPS = "expert_tips_enabled"
    private const val KEY_QUIET_HOURS = "quiet_hours_enabled"
    private const val KEY_WEEKEND_MODE = "weekend_mode_enabled"
    private const val KEY_NOTIFICATION_FREQUENCY = "notification_frequency"
    
    // Advanced Settings Keys
    private const val KEY_AUTO_SCAN_MODE = "auto_scan_mode_enabled"
    private const val KEY_DEBUG_MODE = "debug_mode_enabled"
    private const val KEY_MODEL_VERSION = "model_version_override"
    private const val KEY_CONFIDENCE_THRESHOLD = "confidence_threshold_override"
    private const val KEY_IMAGE_QUALITY = "image_quality_override"
    
    // Default Values
    private const val DEFAULT_LOCATION_DATA = true
    private const val DEFAULT_ANALYTICS_DATA = true
    private const val DEFAULT_PROFILE_VISIBILITY = true
    private const val DEFAULT_SCAN_HISTORY_SHARING = false
    private const val DEFAULT_DATA_RETENTION = true
    private const val DEFAULT_PUSH_NOTIFICATIONS = true
    private const val DEFAULT_SOUND_NOTIFICATIONS = true
    private const val DEFAULT_VIBRATION = true
    private const val DEFAULT_SCAN_COMPLETE = true
    private const val DEFAULT_DISEASE_ALERTS = true
    private const val DEFAULT_LOW_CONFIDENCE_ALERTS = true
    private const val DEFAULT_FORUM_REPLIES = true
    private const val DEFAULT_COMMUNITY_UPDATES = true
    private const val DEFAULT_EXPERT_TIPS = true
    private const val DEFAULT_QUIET_HOURS = false
    private const val DEFAULT_WEEKEND_MODE = false
    private const val DEFAULT_NOTIFICATION_FREQUENCY = "Normal" // High, Normal, Low, Off
    private const val DEFAULT_AUTO_SCAN_MODE = false
    private const val DEFAULT_DEBUG_MODE = false
    
    private lateinit var sharedPreferences: SharedPreferences
    
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        Log.d(TAG, "SettingsManager initialized")
    }
    
    // Privacy Settings
    fun isLocationDataEnabled(): Boolean = sharedPreferences.getBoolean(KEY_LOCATION_DATA, DEFAULT_LOCATION_DATA)
    fun setLocationDataEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_LOCATION_DATA, enabled).apply()
    
    fun isAnalyticsDataEnabled(): Boolean = sharedPreferences.getBoolean(KEY_ANALYTICS_DATA, DEFAULT_ANALYTICS_DATA)
    fun setAnalyticsDataEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_ANALYTICS_DATA, enabled).apply()
    
    fun isProfileVisibilityEnabled(): Boolean = sharedPreferences.getBoolean(KEY_PROFILE_VISIBILITY, DEFAULT_PROFILE_VISIBILITY)
    fun setProfileVisibilityEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_PROFILE_VISIBILITY, enabled).apply()
    
    fun isScanHistorySharingEnabled(): Boolean = sharedPreferences.getBoolean(KEY_SCAN_HISTORY_SHARING, DEFAULT_SCAN_HISTORY_SHARING)
    fun setScanHistorySharingEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_SCAN_HISTORY_SHARING, enabled).apply()
    
    fun isDataRetentionEnabled(): Boolean = sharedPreferences.getBoolean(KEY_DATA_RETENTION, DEFAULT_DATA_RETENTION)
    fun setDataRetentionEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_DATA_RETENTION, enabled).apply()
    
    // Notification Settings
    fun isPushNotificationsEnabled(): Boolean = sharedPreferences.getBoolean(KEY_PUSH_NOTIFICATIONS, DEFAULT_PUSH_NOTIFICATIONS)
    fun setPushNotificationsEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_PUSH_NOTIFICATIONS, enabled).apply()
    
    fun isSoundNotificationsEnabled(): Boolean = sharedPreferences.getBoolean(KEY_SOUND_NOTIFICATIONS, DEFAULT_SOUND_NOTIFICATIONS)
    fun setSoundNotificationsEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_SOUND_NOTIFICATIONS, enabled).apply()
    
    fun isVibrationEnabled(): Boolean = sharedPreferences.getBoolean(KEY_VIBRATION, DEFAULT_VIBRATION)
    fun setVibrationEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_VIBRATION, enabled).apply()
    
    fun isScanCompleteEnabled(): Boolean = sharedPreferences.getBoolean(KEY_SCAN_COMPLETE, DEFAULT_SCAN_COMPLETE)
    fun setScanCompleteEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_SCAN_COMPLETE, enabled).apply()
    
    fun isDiseaseAlertsEnabled(): Boolean = sharedPreferences.getBoolean(KEY_DISEASE_ALERTS, DEFAULT_DISEASE_ALERTS)
    fun setDiseaseAlertsEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_DISEASE_ALERTS, enabled).apply()
    
    fun isLowConfidenceAlertsEnabled(): Boolean = sharedPreferences.getBoolean(KEY_LOW_CONFIDENCE_ALERTS, DEFAULT_LOW_CONFIDENCE_ALERTS)
    fun setLowConfidenceAlertsEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_LOW_CONFIDENCE_ALERTS, enabled).apply()
    
    fun isForumRepliesEnabled(): Boolean = sharedPreferences.getBoolean(KEY_FORUM_REPLIES, DEFAULT_FORUM_REPLIES)
    fun setForumRepliesEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_FORUM_REPLIES, enabled).apply()
    
    fun isCommunityUpdatesEnabled(): Boolean = sharedPreferences.getBoolean(KEY_COMMUNITY_UPDATES, DEFAULT_COMMUNITY_UPDATES)
    fun setCommunityUpdatesEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_COMMUNITY_UPDATES, enabled).apply()
    
    fun isExpertTipsEnabled(): Boolean = sharedPreferences.getBoolean(KEY_EXPERT_TIPS, DEFAULT_EXPERT_TIPS)
    fun setExpertTipsEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_EXPERT_TIPS, enabled).apply()
    
    fun isQuietHoursEnabled(): Boolean = sharedPreferences.getBoolean(KEY_QUIET_HOURS, DEFAULT_QUIET_HOURS)
    fun setQuietHoursEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_QUIET_HOURS, enabled).apply()
    
    fun isWeekendModeEnabled(): Boolean = sharedPreferences.getBoolean(KEY_WEEKEND_MODE, DEFAULT_WEEKEND_MODE)
    fun setWeekendModeEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_WEEKEND_MODE, enabled).apply()
    
    fun getNotificationFrequency(): String = sharedPreferences.getString(KEY_NOTIFICATION_FREQUENCY, DEFAULT_NOTIFICATION_FREQUENCY) ?: DEFAULT_NOTIFICATION_FREQUENCY
    fun setNotificationFrequency(frequency: String) = sharedPreferences.edit().putString(KEY_NOTIFICATION_FREQUENCY, frequency).apply()
    
    // Advanced Settings
    fun isAutoScanModeEnabled(): Boolean = sharedPreferences.getBoolean(KEY_AUTO_SCAN_MODE, DEFAULT_AUTO_SCAN_MODE)
    fun setAutoScanModeEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_AUTO_SCAN_MODE, enabled).apply()
    
    fun isDebugModeEnabled(): Boolean = sharedPreferences.getBoolean(KEY_DEBUG_MODE, DEFAULT_DEBUG_MODE)
    fun setDebugModeEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean(KEY_DEBUG_MODE, enabled).apply()
    
    fun getModelVersionOverride(): String? = sharedPreferences.getString(KEY_MODEL_VERSION, null)
    fun setModelVersionOverride(version: String?) = sharedPreferences.edit().putString(KEY_MODEL_VERSION, version).apply()
    
    fun getConfidenceThresholdOverride(): Float = sharedPreferences.getFloat(KEY_CONFIDENCE_THRESHOLD, -1f)
    fun setConfidenceThresholdOverride(threshold: Float) = sharedPreferences.edit().putFloat(KEY_CONFIDENCE_THRESHOLD, threshold).apply()
    
    fun getImageQualityOverride(): Int = sharedPreferences.getInt(KEY_IMAGE_QUALITY, -1)
    fun setImageQualityOverride(quality: Int) = sharedPreferences.edit().putInt(KEY_IMAGE_QUALITY, quality).apply()
    
    // Utility Methods
    fun getAllSettings(): Map<String, Any> {
        return mapOf(
            // Privacy Settings
            "location_data_enabled" to isLocationDataEnabled(),
            "analytics_data_enabled" to isAnalyticsDataEnabled(),
            "profile_visibility_enabled" to isProfileVisibilityEnabled(),
            "scan_history_sharing_enabled" to isScanHistorySharingEnabled(),
            "data_retention_enabled" to isDataRetentionEnabled(),
            
            // Notification Settings
            "push_notifications_enabled" to isPushNotificationsEnabled(),
            "sound_notifications_enabled" to isSoundNotificationsEnabled(),
            "vibration_enabled" to isVibrationEnabled(),
            "scan_complete_notifications" to isScanCompleteEnabled(),
            "disease_alerts_enabled" to isDiseaseAlertsEnabled(),
            "low_confidence_alerts_enabled" to isLowConfidenceAlertsEnabled(),
            "forum_replies_enabled" to isForumRepliesEnabled(),
            "community_updates_enabled" to isCommunityUpdatesEnabled(),
            "expert_tips_enabled" to isExpertTipsEnabled(),
            "quiet_hours_enabled" to isQuietHoursEnabled(),
            "weekend_mode_enabled" to isWeekendModeEnabled(),
            "notification_frequency" to getNotificationFrequency(),
            
            // Advanced Settings
            "auto_scan_mode_enabled" to isAutoScanModeEnabled(),
            "debug_mode_enabled" to isDebugModeEnabled(),
            "model_version_override" to (getModelVersionOverride() ?: ""),
            "confidence_threshold_override" to getConfidenceThresholdOverride(),
            "image_quality_override" to getImageQualityOverride()
        )
    }
    
    fun resetToDefaults() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        Log.d(TAG, "All settings reset to defaults")
    }
    
    fun exportSettings(): String {
        val settings = getAllSettings()
        return settings.entries.joinToString("\n") { "${it.key} = ${it.value}" }
    }
    
    fun importSettings(settingsData: String) {
        try {
            val lines = settingsData.split("\n")
            val editor = sharedPreferences.edit()
            
            for (line in lines) {
                if (line.contains("=")) {
                    val parts = line.split("=", limit = 2)
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    
                    when (key) {
                        "location_data_enabled", "analytics_data_enabled", "profile_visibility_enabled",
                        "scan_history_sharing_enabled", "data_retention_enabled", "push_notifications_enabled",
                        "sound_notifications_enabled", "vibration_enabled", "scan_complete_notifications",
                        "disease_alerts_enabled", "low_confidence_alerts_enabled", "forum_replies_enabled",
                        "community_updates_enabled", "expert_tips_enabled", "quiet_hours_enabled",
                        "weekend_mode_enabled", "auto_scan_mode_enabled", "debug_mode_enabled" -> {
                            editor.putBoolean(key, value.toBoolean())
                        }
                        "confidence_threshold_override" -> {
                            editor.putFloat(key, value.toFloat())
                        }
                        "image_quality_override" -> {
                            editor.putInt(key, value.toInt())
                        }
                        else -> {
                            editor.putString(key, value)
                        }
                    }
                }
            }
            editor.apply()
            Log.d(TAG, "Settings imported successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error importing settings: ${e.message}")
        }
    }
}
