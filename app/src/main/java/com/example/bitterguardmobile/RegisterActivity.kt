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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import android.Manifest

class RegisterActivity : AppCompatActivity() {

	private var authManager: SupabaseAuthManager? = null
    private lateinit var requestLocationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_register)

		authManager = if (SupabaseConfig.isConfigured()) SupabaseAuthManager(this) else null

        // Prepare location permission launcher (asked after successful registration)
        requestLocationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
                // We navigate to Home regardless; Home will show real location if granted, placeholders otherwise
                navigateToHome()
            }

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
                        // Clear offline flag on successful registration
                        getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("offline_mode", false).apply()
                        Toast.makeText(this@RegisterActivity, "Account created!", Toast.LENGTH_SHORT).show()
                        // Upsert into users/profiles on background thread
                        val manager = authManager
                        val accessToken = manager?.getAccessToken()
                        if (accessToken != null) {
                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val client = SimpleSupabaseClient()
                                // Upsert to users table (source of truth)
                                run {
                                    val jsonUsers = kotlinx.serialization.json.buildJsonObject {
                                        // Align with schema: use auth_user_id as foreign key to auth.uid()
                                        put("auth_user_id", kotlinx.serialization.json.JsonPrimitive(result.user!!.id))
                                        put("display_name", kotlinx.serialization.json.JsonPrimitive(name))
                                        put("full_name", kotlinx.serialization.json.JsonPrimitive(name))
                                        put("phone", kotlinx.serialization.json.JsonPrimitive(phone))
                                        if (emailInput.isNotEmpty()) put("email", kotlinx.serialization.json.JsonPrimitive(emailInput))
                                    }
                                    android.util.Log.d("RegisterActivity", "Attempting to upsert to users table with data: ${jsonUsers}")
                                    val upsertRes = client.postWithUserAuth(
                                        table = "users",
                                        data = jsonUsers,
                                        authToken = accessToken,
                                        // Upsert by auth_user_id so we don't fight the primary id
                                        onConflict = "auth_user_id",
                                        prefer = "resolution=merge-duplicates,return=minimal"
                                    )
                                    if (!upsertRes.isSuccess) {
                                        android.util.Log.e("RegisterActivity", "Users upsert failed: ${upsertRes.exceptionOrNull()?.message}")
                                        android.util.Log.e("RegisterActivity", "Response body: ${upsertRes.exceptionOrNull()?.message}")
                                        // Fallback: attempt PATCH on users by id
                                        val patchRes = client.patch(
                                            com.example.bitterguardmobile.SupabaseConfig.REST_URL + "users?auth_user_id=eq.${result.user!!.id}",
                                            jsonUsers,
                                            accessToken
                                        )
                                        if (!patchRes.isSuccess) {
                                            android.util.Log.e("RegisterActivity", "Users patch failed: ${patchRes.exceptionOrNull()?.message}")
                                        } else {
                                            android.util.Log.d("RegisterActivity", "Users patch succeeded for display_name")
                                        }
                                    } else {
                                        android.util.Log.d("RegisterActivity", "Users upsert succeeded for display_name")
                                    }
                                }
                                // Optional: keep profiles in sync if you still use it elsewhere
                                run {
                                    val jsonProfiles = kotlinx.serialization.json.buildJsonObject {
                                        put("id", kotlinx.serialization.json.JsonPrimitive(result.user!!.id))
                                        put("display_name", kotlinx.serialization.json.JsonPrimitive(name))
                                        put("phone", kotlinx.serialization.json.JsonPrimitive(phone))
                                        if (emailInput.isNotEmpty()) put("email", kotlinx.serialization.json.JsonPrimitive(emailInput))
                                    }
                                    val profileRes = client.postWithUserAuth(
                                        table = "profiles",
                                        data = jsonProfiles,
                                        authToken = accessToken,
                                        onConflict = "id",
                                        prefer = "resolution=merge-duplicates,return=minimal"
                                    )
                                    if (!profileRes.isSuccess) {
                                        android.util.Log.w("RegisterActivity", "Profiles upsert failed (non-fatal): ${profileRes.exceptionOrNull()?.message}")
                                    }
                                }
                            }
                        }

                        // Ask for location permission on first run to personalize weather/location
                        promptForLocationPermissionThenNavigate()
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
			getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("offline_mode", true).apply()
			startActivity(Intent(this, HomeActivity::class.java))
			finish()
		}
	}

	private fun promptForLocationPermissionThenNavigate() {
		AlertDialog.Builder(this)
			.setTitle("Enable Location?")
			.setMessage("Allow location access to show your city and real-time temperature. You can change this later in Settings.")
			.setPositiveButton("Allow") { _, _ ->
				requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
			}
			.setNegativeButton("Not now") { _, _ ->
				navigateToHome()
			}
			.setCancelable(false)
			.show()
	}

	private fun navigateToHome() {
		startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
		finish()
	}

	private fun synthesizeEmailFromPhone(phone: String): String {
		val normalized = phone.replace("[^0-9+]".toRegex(), "")
		return "$normalized@bitterguard.local"
	}
}


