package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings

class SettingsActivity : BaseActivity() {
    
    private lateinit var languageSetting: LinearLayout
    private lateinit var currentLanguage: TextView
    
    private lateinit var aboutSetting: LinearLayout
    private lateinit var notificationsSetting: LinearLayout
    private lateinit var locationSetting: LinearLayout
    
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
        
        aboutSetting = findViewById(R.id.aboutSetting)
        notificationsSetting = findViewById(R.id.notificationsSetting)
        locationSetting = findViewById(R.id.locationSetting)
        
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
        
        
        
        
        // About setting click listener
        aboutSetting.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        
        
        
        
        // Help & Support click
        findViewById<LinearLayout>(R.id.helpSetting).setOnClickListener {
            val intent = Intent(this, HelpSupportActivity::class.java)
            startActivity(intent)
        }

        // Location settings
        locationSetting.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.unable_open_location_settings), Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set up switch listeners for main settings
        findViewById<android.widget.Switch>(R.id.notificationsSwitch).setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setPushNotificationsEnabled(isChecked)
            Toast.makeText(this, if (isChecked) getString(R.string.notifications_enabled) else getString(R.string.notifications_disabled), Toast.LENGTH_SHORT).show()
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
                // Recreate only this activity so UI strings refresh without logging out or leaving
                recreate()
                
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    
    
    
    
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 