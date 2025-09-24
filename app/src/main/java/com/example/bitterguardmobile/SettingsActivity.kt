package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : BaseActivity() {
    
    private lateinit var languageSetting: LinearLayout
    private lateinit var currentLanguage: TextView
    private lateinit var themeSetting: LinearLayout
    private lateinit var configSetting: LinearLayout
    private lateinit var translationSetting: LinearLayout
    private lateinit var aboutSetting: LinearLayout
    private lateinit var notificationsSetting: LinearLayout
    private lateinit var autoScanSetting: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Set up toolbar with back button
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize views
        languageSetting = findViewById(R.id.languageSetting)
        currentLanguage = findViewById(R.id.currentLanguage)
        themeSetting = findViewById(R.id.themeSetting)
        configSetting = findViewById(R.id.configSetting)
        translationSetting = findViewById(R.id.translationSetting)
        aboutSetting = findViewById(R.id.aboutSetting)
        notificationsSetting = findViewById(R.id.notificationsSetting)
        autoScanSetting = findViewById(R.id.autoScanSetting)
        
        // Initialize SettingsManager
        SettingsManager.initialize(this)
        
        // Set current language display
        updateLanguageDisplay()
        
        // Load current switch states
        updateSwitchStates()
        
        // Language setting click listener
        languageSetting.setOnClickListener {
            showLanguageDialog()
        }
        
        // Theme setting click listener
        themeSetting.setOnClickListener {
            showThemeDialog()
        }
        
        // Config setting click listener (only show in debug mode)
        if (AppConfig.isDebugMode()) {
            configSetting.visibility = android.view.View.VISIBLE
            findViewById<android.view.View>(R.id.configDivider).visibility = android.view.View.VISIBLE
            configSetting.setOnClickListener {
                val intent = Intent(this, ConfigActivity::class.java)
                startActivity(intent)
            }
        } else {
            configSetting.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.configDivider).visibility = android.view.View.GONE
        }
        
        // Translation setting click listener (only show in debug mode)
        if (AppConfig.isDebugMode()) {
            translationSetting.visibility = android.view.View.VISIBLE
            translationSetting.setOnClickListener {
                val intent = Intent(this, TranslationActivity::class.java)
                startActivity(intent)
            }
        } else {
            translationSetting.visibility = android.view.View.GONE
        }
        
        // About setting click listener
        aboutSetting.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        // Notifications setting click listener
        notificationsSetting.setOnClickListener {
            showNotificationSettingsDialog()
        }
        
        // Auto-scan setting click listener
        autoScanSetting.setOnClickListener {
            showAutoScanSettingsDialog()
        }
        
        // Help & Support click
        findViewById<LinearLayout>(R.id.helpSetting).setOnClickListener {
            val intent = Intent(this, HelpSupportActivity::class.java)
            startActivity(intent)
        }
        
        // Set up switch listeners for main settings
        findViewById<android.widget.Switch>(R.id.notificationsSwitch).setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setPushNotificationsEnabled(isChecked)
            Toast.makeText(this, if (isChecked) "Notifications enabled" else "Notifications disabled", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<android.widget.Switch>(R.id.autoScanSwitch).setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setAutoScanModeEnabled(isChecked)
            Toast.makeText(this, if (isChecked) "Auto-scan enabled" else "Auto-scan disabled", Toast.LENGTH_SHORT).show()
        }

    }
    
    private fun updateLanguageDisplay() {
        val currentLanguageCode = LanguageManager.getLanguage(this)
        val languageText = when (currentLanguageCode) {
            "en" -> getString(R.string.english)
            "tl" -> getString(R.string.filipino)
            else -> getString(R.string.english)
        }
        currentLanguage.text = languageText
    }
    
    private fun updateSwitchStates() {
        // Update notification switch
        findViewById<android.widget.Switch>(R.id.notificationsSwitch).isChecked = SettingsManager.isPushNotificationsEnabled()
        
        // Update auto-scan switch
        findViewById<android.widget.Switch>(R.id.autoScanSwitch).isChecked = SettingsManager.isAutoScanModeEnabled()
    }
    
    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.english), getString(R.string.filipino))
        val currentLanguageCode = LanguageManager.getLanguage(this)
        val currentIndex = when (currentLanguageCode) {
            "en" -> 0
            "tl" -> 1
            else -> 0
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.language))
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val selectedLanguage = when (which) {
                    0 -> "en"
                    1 -> "tl"
                    else -> "en"
                }
                
                LanguageManager.setLanguage(this, selectedLanguage)
                updateLanguageDisplay()
                
                // Restart the app to apply language changes
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showThemeDialog() {
        val themes = arrayOf("Light", "Dark")
        val currentTheme = ThemeManager.getCurrentTheme(this)
        val currentIndex = when (currentTheme) {
            ThemeManager.THEME_LIGHT -> 0
            ThemeManager.THEME_DARK -> 1
            else -> 0
        }
        
        AlertDialog.Builder(this)
            .setTitle("Theme")
            .setSingleChoiceItems(themes, currentIndex) { dialog, which ->
                val selectedTheme = when (which) {
                    0 -> ThemeManager.THEME_LIGHT
                    1 -> ThemeManager.THEME_DARK
                    else -> ThemeManager.THEME_LIGHT
                }
                
                // Apply theme immediately without restart
                ThemeManager.setTheme(this, selectedTheme)
                Toast.makeText(this, "Theme changed to ${themes[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showNotificationSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_notification_settings, null)
        
        // Initialize switches with current values
        val pushNotificationsSwitch = dialogView.findViewById<android.widget.Switch>(R.id.pushNotificationsSwitch)
        val soundNotificationsSwitch = dialogView.findViewById<android.widget.Switch>(R.id.soundNotificationsSwitch)
        val vibrationSwitch = dialogView.findViewById<android.widget.Switch>(R.id.vibrationSwitch)
        val scanCompleteSwitch = dialogView.findViewById<android.widget.Switch>(R.id.scanCompleteSwitch)
        val diseaseAlertsSwitch = dialogView.findViewById<android.widget.Switch>(R.id.diseaseAlertsSwitch)
        val lowConfidenceSwitch = dialogView.findViewById<android.widget.Switch>(R.id.lowConfidenceSwitch)
        val forumRepliesSwitch = dialogView.findViewById<android.widget.Switch>(R.id.forumRepliesSwitch)
        val communityUpdatesSwitch = dialogView.findViewById<android.widget.Switch>(R.id.communityUpdatesSwitch)
        val expertTipsSwitch = dialogView.findViewById<android.widget.Switch>(R.id.expertTipsSwitch)
        val quietHoursSwitch = dialogView.findViewById<android.widget.Switch>(R.id.quietHoursSwitch)
        val weekendModeSwitch = dialogView.findViewById<android.widget.Switch>(R.id.weekendModeSwitch)
        
        // Load current settings
        pushNotificationsSwitch.isChecked = SettingsManager.isPushNotificationsEnabled()
        soundNotificationsSwitch.isChecked = SettingsManager.isSoundNotificationsEnabled()
        vibrationSwitch.isChecked = SettingsManager.isVibrationEnabled()
        scanCompleteSwitch.isChecked = SettingsManager.isScanCompleteEnabled()
        diseaseAlertsSwitch.isChecked = SettingsManager.isDiseaseAlertsEnabled()
        lowConfidenceSwitch.isChecked = SettingsManager.isLowConfidenceAlertsEnabled()
        forumRepliesSwitch.isChecked = SettingsManager.isForumRepliesEnabled()
        communityUpdatesSwitch.isChecked = SettingsManager.isCommunityUpdatesEnabled()
        expertTipsSwitch.isChecked = SettingsManager.isExpertTipsEnabled()
        quietHoursSwitch.isChecked = SettingsManager.isQuietHoursEnabled()
        weekendModeSwitch.isChecked = SettingsManager.isWeekendModeEnabled()
        
        // Set up frequency spinner
        val frequencySpinner = dialogView.findViewById<android.widget.Spinner>(R.id.frequencySpinner)
        val frequencies = arrayOf("High", "Normal", "Low", "Off")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, frequencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        frequencySpinner.adapter = adapter
        
        // Set current frequency
        val currentFrequency = SettingsManager.getNotificationFrequency()
        val currentIndex = frequencies.indexOf(currentFrequency)
        if (currentIndex >= 0) {
            frequencySpinner.setSelection(currentIndex)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Notification Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                // Save all settings
                SettingsManager.setPushNotificationsEnabled(pushNotificationsSwitch.isChecked)
                SettingsManager.setSoundNotificationsEnabled(soundNotificationsSwitch.isChecked)
                SettingsManager.setVibrationEnabled(vibrationSwitch.isChecked)
                SettingsManager.setScanCompleteEnabled(scanCompleteSwitch.isChecked)
                SettingsManager.setDiseaseAlertsEnabled(diseaseAlertsSwitch.isChecked)
                SettingsManager.setLowConfidenceAlertsEnabled(lowConfidenceSwitch.isChecked)
                SettingsManager.setForumRepliesEnabled(forumRepliesSwitch.isChecked)
                SettingsManager.setCommunityUpdatesEnabled(communityUpdatesSwitch.isChecked)
                SettingsManager.setExpertTipsEnabled(expertTipsSwitch.isChecked)
                SettingsManager.setQuietHoursEnabled(quietHoursSwitch.isChecked)
                SettingsManager.setWeekendModeEnabled(weekendModeSwitch.isChecked)
                SettingsManager.setNotificationFrequency(frequencies[frequencySpinner.selectedItemPosition])
                
                Toast.makeText(this, "Notification settings saved!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showAutoScanSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_auto_scan_settings, null)
        
        // Initialize switches with current values
        val autoScanSwitch = dialogView.findViewById<android.widget.Switch>(R.id.autoScanSwitch)
        val autoSaveSwitch = dialogView.findViewById<android.widget.Switch>(R.id.autoSaveSwitch)
        val autoShareSwitch = dialogView.findViewById<android.widget.Switch>(R.id.autoShareSwitch)
        
        // Load current settings
        autoScanSwitch.isChecked = SettingsManager.isAutoScanModeEnabled()
        autoSaveSwitch.isChecked = true // Default value
        autoShareSwitch.isChecked = false // Default value
        
        AlertDialog.Builder(this)
            .setTitle("Auto-Scan Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                // Save settings
                SettingsManager.setAutoScanModeEnabled(autoScanSwitch.isChecked)
                
                Toast.makeText(this, "Auto-scan settings saved!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 