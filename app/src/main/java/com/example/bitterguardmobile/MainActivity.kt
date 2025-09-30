package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Loads your XML layout

        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val btnCreateAccount = findViewById<TextView>(R.id.btnCreateAccount)
        val btnContinueOffline = findViewById<TextView>(R.id.btnContinueOffline)

        btnSignIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        btnCreateAccount.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnContinueOffline.setOnClickListener {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("offline_mode", true).apply()
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
