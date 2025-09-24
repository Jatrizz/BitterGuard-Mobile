package com.example.bitterguardmobile

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : BaseActivity() {
    
    private lateinit var btnBack: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        
        // Initialize configuration
        AppConfig.initialize(this)
        
        // Initialize views
        btnBack = findViewById(R.id.btnBack)
        
        // Back Button
        btnBack.setOnClickListener {
            finish()
        }
        
        // Set app version
        val versionText = findViewById<TextView>(R.id.versionText)
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = getString(R.string.version_format, packageInfo.versionName)
        } catch (e: Exception) {
            versionText.text = getString(R.string.version_format, "1.0.0")
        }
    }
} 