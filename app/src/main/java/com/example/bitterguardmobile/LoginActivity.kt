package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

	private var authManager: SupabaseAuthManager? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_login)

		authManager = if (SupabaseConfig.isConfigured()) SupabaseAuthManager(this) else null

		val inputPhoneOrEmail = findViewById<EditText>(R.id.inputPhoneOrEmail)
		val inputPassword = findViewById<EditText>(R.id.inputPassword)
		val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignUp = findViewById<TextView>(R.id.btnSignUp)
        val btnOffline = findViewById<TextView>(R.id.btnContinueOfflineLogin)

		btnLogin.setOnClickListener {
			val identifier = inputPhoneOrEmail.text.toString().trim()
			val password = inputPassword.text.toString().trim()

			if (identifier.isEmpty() || password.length < 6) {
				Toast.makeText(this, "Enter phone/email and 6+ char password", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}

			val email = if (identifier.contains("@")) identifier else synthesizeEmailFromPhone(identifier)

			if (authManager == null) {
				Toast.makeText(this, "Online sign-in required. Configure Supabase.", Toast.LENGTH_LONG).show()
				return@setOnClickListener
			}

			btnLogin.isEnabled = false
			lifecycleScope.launch {
				val result = authManager!!.signIn(email, password)
				runOnUiThread {
					btnLogin.isEnabled = true
					if (result.success) {
						// Clear offline mode on successful login
						getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("offline_mode", false).apply()
						startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
						finish()
					} else {
						Toast.makeText(this@LoginActivity, result.error ?: "Invalid credentials", Toast.LENGTH_LONG).show()
					}
				}
			}
		}

		btnSignUp.setOnClickListener {
			startActivity(Intent(this, RegisterActivity::class.java))
		}

        btnOffline.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
	}

	private fun synthesizeEmailFromPhone(phone: String): String {
		val normalized = phone.replace("[^0-9+]".toRegex(), "")
		return "$normalized@bitterguard.local"
	}
}


