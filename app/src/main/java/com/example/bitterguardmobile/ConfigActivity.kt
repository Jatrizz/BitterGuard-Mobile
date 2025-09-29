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
    
    /**
     * Simple adapter for configuration items
     */
    private inner class ConfigAdapter : RecyclerView.Adapter<ConfigAdapter.ConfigViewHolder>() {
        private val configItems = mutableListOf<ConfigItem>()
        
        init {
            loadConfigItems()
        }
        
        private fun loadConfigItems() {
            configItems.clear()
            configItems.addAll(listOf(
                ConfigItem("API Base URL", "API endpoint URL", AppConfig.getApiBaseUrl()),
                ConfigItem("Debug Mode", "Enable debug features", AppConfig.isDebugMode().toString()),
                ConfigItem("Model Version", "Current model version", AppConfig.getModelVersion()),
                ConfigItem("Max History Size", "Maximum scan history items", AppConfig.getMaxHistorySize().toString()),
                ConfigItem("Confidence Threshold", "Detection confidence threshold", AppConfig.getConfidenceThreshold().toString()),
                ConfigItem("Image Quality", "Image compression quality", AppConfig.getImageQuality().toString())
            ))
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfigViewHolder {
            val view = layoutInflater.inflate(R.layout.item_config, parent, false)
            return ConfigViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
            val item = configItems[position]
            holder.bind(item)
        }
        
        override fun getItemCount() = configItems.size
        
        fun saveAllChanges() {
            // Save logic would go here
        }
        
        inner class ConfigViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            private val keyText: TextView = itemView.findViewById(R.id.configKey)
            private val descText: TextView = itemView.findViewById(R.id.configDescription)
            private val valueEdit: EditText = itemView.findViewById(R.id.configValue)
            
            fun bind(item: ConfigItem) {
                keyText.text = item.key
                descText.text = item.description
                valueEdit.setText(item.value)
            }
        }
    }
    
    data class ConfigItem(
        val key: String,
        val description: String,
        val value: String
    )
}
