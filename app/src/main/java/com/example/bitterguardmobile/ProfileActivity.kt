package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log

class ProfileActivity : BaseActivity() {
    
    private lateinit var editProfileAction: LinearLayout
    private lateinit var changePasswordAction: LinearLayout
    private lateinit var privacyAction: LinearLayout
    private lateinit var logoutAction: LinearLayout
    private lateinit var historyManager: HistoryManager
    private lateinit var authManager: SupabaseAuthManager
    
    // Profile display views
    private lateinit var userNameText: TextView
    private lateinit var userPhoneText: TextView
    private lateinit var userLocationText: TextView
    private lateinit var totalScansText: TextView
    private lateinit var diseasesFoundText: TextView
    private lateinit var recentActivityList: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        // Initialize Supabase Auth
        authManager = SupabaseAuthManager(this)
        
        // Initialize views and data
        initializeViews()
        loadUserData()
        
        // Set up toolbar with back button
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Set up click listeners
        setupClickListeners()
    }
    
    private fun initializeViews() {
        editProfileAction = findViewById(R.id.editProfileAction)
        changePasswordAction = findViewById(R.id.changePasswordAction)
        privacyAction = findViewById(R.id.privacyAction)
        logoutAction = findViewById(R.id.logoutAction)
        historyManager = HistoryManager(this)
        
        // Initialize profile display views
        userNameText = findViewById(R.id.userNameText)
        userPhoneText = findViewById(R.id.userPhoneText)
        userLocationText = findViewById(R.id.userLocationText)
        totalScansText = findViewById(R.id.totalScansText)
        diseasesFoundText = findViewById(R.id.diseasesFoundText)
        recentActivityList = findViewById(R.id.recentActivityList)
        recentActivityList.layoutManager = LinearLayoutManager(this)
    }
    
    private fun loadUserData() {
        val currentUser = authManager.getCurrentUser()
        if (currentUser != null) {
            // Update UI with user data from Supabase
            userNameText.text = currentUser.name ?: "User"
            userPhoneText.text = currentUser.phone ?: "Phone not set"
            
            // Load location using shared service
            loadUserLocation()
            
            // Load scan statistics
            loadScanStatistics()
            loadRecentActivity()
        } else {
            // User not logged in, redirect to landing
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    private fun loadUserLocation() {
        lifecycleScope.launchWhenStarted {
            try {
                val locationString = LocationService.getLocationString(this@ProfileActivity)
                userLocationText.text = "📍 $locationString"
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error loading location: ${e.message}")
                userLocationText.text = "📍 Location not available"
            }
        }
    }
    
    // Firebase user document creation removed - now using Supabase
    
    private fun loadScanStatistics() {
        val scanHistory = historyManager.getScanHistory()
        val totalScans = scanHistory.size
        val diseasesFound = scanHistory.count { it.prediction != "Fresh Leaf" && it.prediction != "Healthy" }
        
        totalScansText.text = totalScans.toString()
        diseasesFoundText.text = diseasesFound.toString()
    }

    private fun loadRecentActivity() {
        val scanHistory = historyManager.getScanHistory()
        // Show last 10 items, newest first
        val items = scanHistory.takeLast(10).asReversed()
        recentActivityList.adapter = RecentActivityAdapter(items)
    }
    
    private fun setupClickListeners() {
        editProfileAction.setOnClickListener {
            showEditProfileDialog()
        }
        
        changePasswordAction.setOnClickListener {
            showChangePasswordDialog()
        }
        
        privacyAction.setOnClickListener {
            showPrivacySettingsDialog()
        }
        
        logoutAction.setOnClickListener {
            showLogoutDialog()
        }
    }
    
    private fun showEditProfileDialog() {
        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create a custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val fullNameInput = dialogView.findViewById<android.widget.EditText>(R.id.editFullName)
        val phoneInput = dialogView.findViewById<android.widget.EditText>(R.id.editPhone)
        val locationInput = dialogView.findViewById<android.widget.EditText>(R.id.editLocation)
        
        // Load current user data into the form
        val user = authManager.getCurrentUser()
        if (user != null) {
            fullNameInput.setText(user.name ?: "")
            phoneInput.setText(user.phone ?: "")
            locationInput.setText("") // Location not available in Supabase user model
        }
        
        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val fullName = fullNameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                val location = locationInput.text.toString().trim()
                
                if (fullName.isEmpty()) {
                    Toast.makeText(this, "Full name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                updateUserProfile(fullName, phone, location)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun updateUserProfile(fullName: String, phone: String, location: String) {
        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        // TODO: Implement Supabase user profile update when needed
        // For now, just update the UI with the entered values
        userNameText.text = fullName
        userPhoneText.text = phone.ifEmpty { "Phone not set" }
        userLocationText.text = "📍 $location"
        
        Toast.makeText(this, "Profile updated successfully! (Note: Supabase profile update coming soon)", Toast.LENGTH_SHORT).show()
    }
    
    private fun showChangePasswordDialog() {
        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create a custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val currentPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.currentPasswordInput)
        val newPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.newPasswordInput)
        val confirmPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.confirmPasswordInput)
        
        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change Password") { dialog, _ ->
                val currentPassword = currentPasswordInput.text.toString().trim()
                val newPassword = newPasswordInput.text.toString().trim()
                val confirmPassword = confirmPasswordInput.text.toString().trim()
                
                if (validatePasswordInputs(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun validatePasswordInputs(currentPassword: String, newPassword: String, confirmPassword: String): Boolean {
        when {
            currentPassword.isEmpty() -> {
                Toast.makeText(this, "Please enter your current password", Toast.LENGTH_SHORT).show()
                return false
            }
            newPassword.isEmpty() -> {
                Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show()
                return false
            }
            newPassword.length < 6 -> {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return false
            }
            newPassword == currentPassword -> {
                Toast.makeText(this, "New password must be different from current password", Toast.LENGTH_SHORT).show()
                return false
            }
            newPassword != confirmPassword -> {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return false
            }
            !isPasswordStrong(newPassword) -> {
                Toast.makeText(this, "Password should contain letters and numbers", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }
    
    private fun isPasswordStrong(password: String): Boolean {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }
    
    private fun changePassword(currentPassword: String, newPassword: String) {
        // Firebase password change removed - now using Supabase
        // TODO: Implement Supabase password change when needed
        Toast.makeText(this, "Password change feature coming soon with Supabase", Toast.LENGTH_SHORT).show()
    }
    
    private fun showPrivacySettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_privacy_settings, null)
        
        // (Simplified) No per-item privacy toggles
        
        // Set up click listeners
        dialogView.findViewById<android.widget.TextView>(R.id.clearAllDataButton).setOnClickListener {
            showClearAllDataDialog()
        }
        
        // Removed Export Data button per simplification
        
        dialogView.findViewById<android.widget.TextView>(R.id.viewPrivacyPolicyButton).setOnClickListener {
            showPrivacyPolicy()
        }
        
        AlertDialog.Builder(this)
            .setTitle("Privacy Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showClearAllDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Data")
            .setMessage("This will permanently delete all your scan history, settings, and profile data. This action cannot be undone.")
            .setPositiveButton("Clear All Data") { dialog, _ ->
                clearAllUserData()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun clearAllUserData() {
        val currentUser = authManager.getCurrentUser()
        if (currentUser != null) {
            // Clear scan history
            historyManager.clearHistory()
            
            // Clear local settings
            SettingsManager.resetToDefaults()
            
            Toast.makeText(this, "All local data cleared successfully", Toast.LENGTH_LONG).show()
            
            // Sign out and redirect to login
            lifecycleScope.launch {
                val success = authManager.signOut()
                if (success) {
                    val intent = Intent(this@ProfileActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@ProfileActivity, "Failed to sign out", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun exportUserData() {
        val currentUser = authManager.getCurrentUser()
        if (currentUser != null) {
            // Get user data from Supabase
            val scanHistory = historyManager.getScanHistory()
            val settings = SettingsManager.getAllSettings()
            
            val exportData = buildString {
                appendLine("=== BITTERGUARD MOBILE DATA EXPORT ===")
                appendLine("Export Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                appendLine()
                
                appendLine("=== USER PROFILE ===")
                appendLine("UID: ${currentUser.id ?: "N/A"}")
                appendLine("Full Name: ${currentUser.name ?: "N/A"}")
                appendLine("Email: ${currentUser.email ?: "N/A"}")
                appendLine("Phone: N/A") // Phone not available in Supabase user model
                appendLine("Location: N/A") // Location not available in Supabase user model
                appendLine("Created: N/A") // Created date not available in current user model
                appendLine()
                            
                appendLine("=== SCAN HISTORY (${scanHistory.size} records) ===")
                scanHistory.forEachIndexed { index, scan ->
                    appendLine("Scan ${index + 1}:")
                    appendLine("  Prediction: ${scan.prediction}")
                    appendLine("  Confidence: ${scan.confidence}")
                    appendLine("  Date: ${scan.date} ${scan.time}")
                    appendLine("  Location: ${scan.location}")
                    appendLine()
                }
                
                appendLine("=== SETTINGS ===")
                settings.forEach { (key, value) ->
                    appendLine("$key: $value")
                }
            }
            
            // Save to file (in a real app, you might want to save to external storage)
            val fileName = "bitterguard_export_${System.currentTimeMillis()}.txt"
            
            try {
                val file = java.io.File(getExternalFilesDir(null), fileName)
                file.writeText(exportData)
                
                Toast.makeText(this, "Data exported to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to export data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showPrivacyPolicy() {
        AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage("""
                BitterGuard Mobile Privacy Policy
                
                Data Collection:
                • We collect scan images for disease detection
                • Location data (optional) for geographic analysis
                • Usage analytics to improve the app
                
                Data Usage:
                • Images are processed locally on your device
                • No personal data is shared without consent
                • Scan results help improve our AI model
                
                Data Security:
                • All data is encrypted and secure
                • You can export or delete your data anytime
                • We comply with data protection regulations
                
                Contact: support@bitterguard.com
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { dialog, _ ->
                logout()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun logout() {
        lifecycleScope.launch {
            val success = authManager.signOut()
            if (success) {
                Toast.makeText(this@ProfileActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()
                
                // Navigate to landing screen
                val intent = Intent(this@ProfileActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this@ProfileActivity, "Logout failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 

class RecentActivityAdapter(private val scans: List<ScanResult>) : RecyclerView.Adapter<RecentActivityAdapter.VH>() {
    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(android.R.id.text1)
        val subtitle: TextView = itemView.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val container = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
        }
        val title = TextView(ctx).apply { id = android.R.id.text1; textSize = 14f; setTextColor(0xFF333333.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD) }
        val subtitle = TextView(ctx).apply { id = android.R.id.text2; textSize = 12f; setTextColor(0xFF666666.toInt()) }
        container.addView(title)
        container.addView(subtitle)
        return VH(container)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = scans[position]
        val status = s.prediction
        holder.title.text = status
        holder.subtitle.text = "${s.date} ${s.time} • ${s.location} • ${s.confidence}"
    }

    override fun getItemCount(): Int = scans.size
}