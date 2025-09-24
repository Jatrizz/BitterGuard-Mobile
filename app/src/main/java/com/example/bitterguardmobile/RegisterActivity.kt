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

class RegisterActivity : AppCompatActivity() {

	private var authManager: SupabaseAuthManager? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_register)

		authManager = if (SupabaseConfig.isConfigured()) SupabaseAuthManager(this) else null

		val inputFullName = findViewById<EditText>(R.id.inputFullName)
		val inputPhone = findViewById<EditText>(R.id.inputPhone)
		val inputEmail = findViewById<EditText>(R.id.inputEmail)
		val inputPassword = findViewById<EditText>(R.id.inputPassword)
		val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnSignIn = findViewById<TextView>(R.id.btnSignIn)
        val btnOffline = findViewById<TextView>(R.id.btnContinueOfflineRegister)

		btnRegister.setOnClickListener {
			val name = inputFullName.text.toString().trim()
			val phone = inputPhone.text.toString().trim()
			val emailInput = inputEmail.text.toString().trim()
			val password = inputPassword.text.toString().trim()

			if (name.isEmpty()) { Toast.makeText(this, "Enter full name", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
			if (phone.isEmpty()) { Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
			if (password.length < 6) { Toast.makeText(this, "Password must be 6+ chars", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

			if (authManager == null) {
				Toast.makeText(this, "Online signup required. Configure Supabase.", Toast.LENGTH_LONG).show()
				return@setOnClickListener
			}

			btnRegister.isEnabled = false
			// Always use phone-based synthetic email for auth
			val authEmail = synthesizeEmailFromPhone(phone)
			lifecycleScope.launch {
				val result = authManager!!.register(authEmail, password, name = name, phone = phone, realEmail = if (emailInput.isNotEmpty()) emailInput else null)
				runOnUiThread {
					btnRegister.isEnabled = true
					if (result.success) {
						Toast.makeText(this@RegisterActivity, "Account created!", Toast.LENGTH_SHORT).show()
						// Upsert into profiles table: id (auth.uid()), display_name, phone, email
						val manager = authManager
						val accessToken = manager?.getAccessToken()
						if (accessToken != null) {
							lifecycleScope.launch {
								val client = SimpleSupabaseClient()
                                val json = kotlinx.serialization.json.buildJsonObject {
                                    put("id", kotlinx.serialization.json.JsonPrimitive(result.user!!.id))
                                    put("display_name", kotlinx.serialization.json.JsonPrimitive(name))
                                    put("phone", kotlinx.serialization.json.JsonPrimitive(phone))
                                    if (emailInput.isNotEmpty()) put("email", kotlinx.serialization.json.JsonPrimitive(emailInput))
                                }
								client.postWithUserAuth(
									table = "profiles",
									data = json,
									authToken = accessToken,
									onConflict = "id",
									prefer = "resolution=merge-duplicates,return=minimal"
								)
							}
						}

						startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
						finish()
					} else {
						Toast.makeText(this@RegisterActivity, result.error ?: "Registration failed", Toast.LENGTH_LONG).show()
					}
				}
			}
		}

		btnSignIn.setOnClickListener {
			finish()
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


