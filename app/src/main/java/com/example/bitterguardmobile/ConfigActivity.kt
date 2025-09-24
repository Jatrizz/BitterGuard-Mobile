package com.example.bitterguardmobile

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Configuration Management Activity
 * Allows easy modification of app settings (for debugging/testing)
 */
class ConfigActivity : BaseActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnReset: Button
    private lateinit var btnSave: Button
    private lateinit var configAdapter: ConfigAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        
        // Initialize configuration
        AppConfig.initialize(this)
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "App Configuration"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize views
        recyclerView = findViewById(R.id.configRecyclerView)
        btnReset = findViewById(R.id.btnResetConfig)
        btnSave = findViewById(R.id.btnSaveConfig)
        
        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        configAdapter = ConfigAdapter()
        recyclerView.adapter = configAdapter
        
        // Set up buttons
        btnReset.setOnClickListener {
            resetToDefaults()
        }
        
        btnSave.setOnClickListener {
            saveConfiguration()
        }
    }
    
    private fun resetToDefaults() {
        AppConfig.resetToDefaults()
        configAdapter.notifyDataSetChanged()
        Toast.makeText(this, "Configuration reset to defaults", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveConfiguration() {
        configAdapter.saveAllChanges()
        Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    inner class ConfigAdapter : RecyclerView.Adapter<ConfigAdapter.ConfigViewHolder>() {
        
        private val configItems = mutableListOf<ConfigItem>()
        
        init {
            loadConfigItems()
        }
        
        private fun loadConfigItems() {
            configItems.clear()
            configItems.addAll(listOf(
                ConfigItem("API Base URL", AppConfig.getApiBaseUrl(), ConfigItem.Type.TEXT),
                ConfigItem("Supabase Project URL", AppConfig.getSupabaseProjectUrl(), ConfigItem.Type.TEXT),
                ConfigItem("Debug Mode", AppConfig.isDebugMode().toString(), ConfigItem.Type.BOOLEAN),
                ConfigItem("Model Version", AppConfig.getModelVersion(), ConfigItem.Type.TEXT),
                ConfigItem("Max History Size", AppConfig.getMaxHistorySize().toString(), ConfigItem.Type.NUMBER),
                ConfigItem("Confidence Threshold", AppConfig.getConfidenceThreshold().toString(), ConfigItem.Type.NUMBER),
                ConfigItem("Image Quality", AppConfig.getImageQuality().toString(), ConfigItem.Type.NUMBER),
                ConfigItem("Support Email", AppConfig.getSupportEmail(), ConfigItem.Type.TEXT),
                ConfigItem("Support Phone", AppConfig.getSupportPhone(), ConfigItem.Type.TEXT),
                ConfigItem("Website URL", AppConfig.getWebsiteUrl(), ConfigItem.Type.TEXT),
                ConfigItem("App Version", AppConfig.getAppVersion(), ConfigItem.Type.TEXT),
                ConfigItem("Company Name", AppConfig.getCompanyName(), ConfigItem.Type.TEXT),
                ConfigItem("Test Email", AppConfig.getTestEmail(), ConfigItem.Type.TEXT),
                ConfigItem("Test Password", AppConfig.getTestPassword(), ConfigItem.Type.TEXT)
            ))
        }
        
        fun saveAllChanges() {
            // This would save all changes made in the UI
            // For now, we'll just reload the config
            loadConfigItems()
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ConfigViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_config, parent, false)
            return ConfigViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
            holder.bind(configItems[position])
        }
        
        override fun getItemCount(): Int = configItems.size
        
        inner class ConfigViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            private val configName: TextView = itemView.findViewById(R.id.configName)
            private val configValue: EditText = itemView.findViewById(R.id.configValue)
            private val configType: TextView = itemView.findViewById(R.id.configType)
            
            fun bind(item: ConfigItem) {
                configName.text = item.name
                configValue.setText(item.value)
                configType.text = item.type.name
                
                // Set input type based on config type
                when (item.type) {
                    ConfigItem.Type.NUMBER -> configValue.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    ConfigItem.Type.BOOLEAN -> {
                        configValue.inputType = android.text.InputType.TYPE_CLASS_TEXT
                        configValue.setText(if (item.value.toBoolean()) "true" else "false")
                    }
                    else -> configValue.inputType = android.text.InputType.TYPE_CLASS_TEXT
                }
                
                // Save changes when text changes
                configValue.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        item.value = configValue.text.toString()
                        saveConfigItem(item)
                    }
                }
            }
            
            private fun saveConfigItem(item: ConfigItem) {
                when (item.name) {
                    "API Base URL" -> AppConfig.setApiBaseUrl(item.value)
                    "Supabase Project URL" -> AppConfig.setSupabaseProjectUrl(item.value)
                    "Debug Mode" -> AppConfig.setDebugMode(item.value.toBoolean())
                    "Model Version" -> AppConfig.setModelVersion(item.value)
                    "Max History Size" -> AppConfig.setMaxHistorySize(item.value.toIntOrNull() ?: 100)
                    "Confidence Threshold" -> AppConfig.setConfidenceThreshold(item.value.toFloatOrNull() ?: 0.5f)
                    "Image Quality" -> AppConfig.setImageQuality(item.value.toIntOrNull() ?: 85)
                    "Support Email" -> AppConfig.setSupportEmail(item.value)
                    "Support Phone" -> AppConfig.setSupportPhone(item.value)
                    "Website URL" -> AppConfig.setWebsiteUrl(item.value)
                    "App Version" -> AppConfig.setAppVersion(item.value)
                    "Company Name" -> AppConfig.setCompanyName(item.value)
                    "Test Email" -> AppConfig.setTestEmail(item.value)
                    "Test Password" -> AppConfig.setTestPassword(item.value)
                }
            }
        }
    }
    
    data class ConfigItem(
        val name: String,
        var value: String,
        val type: Type
    ) {
        enum class Type {
            TEXT, NUMBER, BOOLEAN
        }
    }
}
