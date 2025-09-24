package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

/**
 * Translation Management Activity
 * Allows adding, editing, and managing translations dynamically
 */
class TranslationActivity : BaseActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddLanguage: Button
    private lateinit var btnExport: Button
    private lateinit var btnImport: Button
    private lateinit var btnReset: Button
    private lateinit var languageSpinner: Spinner
    private lateinit var translationAdapter: TranslationAdapter
    
    private var currentLanguage = "en"
    private val availableLanguages = mutableListOf<String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translation)
        
        // Initialize configuration and translation manager
        AppConfig.initialize(this)
        TranslationManager.initialize(this)
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Translation Management"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize views
        recyclerView = findViewById(R.id.translationRecyclerView)
        btnAddLanguage = findViewById(R.id.btnAddLanguage)
        btnExport = findViewById(R.id.btnExportTranslations)
        btnImport = findViewById(R.id.btnImportTranslations)
        btnReset = findViewById(R.id.btnResetTranslations)
        languageSpinner = findViewById(R.id.languageSpinner)
        
        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        translationAdapter = TranslationAdapter()
        recyclerView.adapter = translationAdapter
        
        // Load available languages
        loadAvailableLanguages()
        setupLanguageSpinner()
        
        // Set up buttons
        btnAddLanguage.setOnClickListener {
            showAddLanguageDialog()
        }
        
        btnExport.setOnClickListener {
            exportTranslations()
        }
        
        btnImport.setOnClickListener {
            showImportDialog()
        }
        
        btnReset.setOnClickListener {
            resetTranslations()
        }
        
        // Set up language spinner
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                currentLanguage = availableLanguages[position]
                translationAdapter.loadTranslationsForLanguage(currentLanguage)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun loadAvailableLanguages() {
        availableLanguages.clear()
        availableLanguages.addAll(TranslationManager.getAvailableLanguages())
    }
    
    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableLanguages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter
    }
    
    private fun showAddLanguageDialog() {
        val editText = EditText(this)
        editText.hint = "Language code (e.g., 'es' for Spanish)"
        
        AlertDialog.Builder(this)
            .setTitle("Add New Language")
            .setMessage("Enter a language code to add a new language:")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val languageCode = editText.text.toString().trim()
                if (languageCode.isNotEmpty()) {
                    addNewLanguage(languageCode)
                } else {
                    Toast.makeText(this, "Please enter a language code", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addNewLanguage(languageCode: String) {
        if (availableLanguages.contains(languageCode)) {
            Toast.makeText(this, "Language already exists", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create empty translations for the new language
        val emptyTranslations = emptyMap<String, String>()
        TranslationManager.addNewLanguage(languageCode, emptyTranslations)
        
        // Refresh UI
        loadAvailableLanguages()
        setupLanguageSpinner()
        translationAdapter.loadTranslationsForLanguage(languageCode)
        
        Toast.makeText(this, "Language '$languageCode' added successfully", Toast.LENGTH_SHORT).show()
    }
    
    private fun exportTranslations() {
        val translationsJson = TranslationManager.exportTranslations()
        
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, translationsJson)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BitterGuard Translations Export")
        
        startActivity(Intent.createChooser(shareIntent, "Export Translations"))
    }
    
    private fun showImportDialog() {
        val editText = EditText(this)
        editText.hint = "Paste JSON translations here"
        editText.minLines = 5
        
        AlertDialog.Builder(this)
            .setTitle("Import Translations")
            .setMessage("Paste the JSON translations data:")
            .setView(editText)
            .setPositiveButton("Import") { _, _ ->
                val jsonString = editText.text.toString().trim()
                if (jsonString.isNotEmpty()) {
                    if (TranslationManager.importTranslations(jsonString)) {
                        Toast.makeText(this, "Translations imported successfully", Toast.LENGTH_SHORT).show()
                        loadAvailableLanguages()
                        setupLanguageSpinner()
                        translationAdapter.loadTranslationsForLanguage(currentLanguage)
                    } else {
                        Toast.makeText(this, "Invalid JSON format", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter JSON data", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun resetTranslations() {
        AlertDialog.Builder(this)
            .setTitle("Reset Translations")
            .setMessage("Are you sure you want to reset all custom translations to defaults?")
            .setPositiveButton("Reset") { _, _ ->
                TranslationManager.resetToDefaults()
                loadAvailableLanguages()
                setupLanguageSpinner()
                translationAdapter.loadTranslationsForLanguage(currentLanguage)
                Toast.makeText(this, "Translations reset to defaults", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    inner class TranslationAdapter : RecyclerView.Adapter<TranslationAdapter.TranslationViewHolder>() {
        
        private val translations = mutableListOf<TranslationItem>()
        
        fun loadTranslationsForLanguage(language: String) {
            translations.clear()
            val languageTranslations = TranslationManager.getTranslationsForLanguage(language)
            languageTranslations.forEach { (key, value) ->
                translations.add(TranslationItem(key, value))
            }
            translations.sortBy { it.key }
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): TranslationViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_translation, parent, false)
            return TranslationViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: TranslationViewHolder, position: Int) {
            holder.bind(translations[position])
        }
        
        override fun getItemCount(): Int = translations.size
        
        inner class TranslationViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            private val keyText: TextView = itemView.findViewById(R.id.translationKey)
            private val valueEdit: EditText = itemView.findViewById(R.id.translationValue)
            private val btnSave: Button = itemView.findViewById(R.id.btnSaveTranslation)
            private val btnDelete: Button = itemView.findViewById(R.id.btnDeleteTranslation)
            
            fun bind(item: TranslationItem) {
                keyText.text = item.key
                valueEdit.setText(item.value)
                
                btnSave.setOnClickListener {
                    val newValue = valueEdit.text.toString().trim()
                    if (newValue.isNotEmpty()) {
                        TranslationManager.addCustomTranslation(currentLanguage, item.key, newValue)
                        item.value = newValue
                        Toast.makeText(this@TranslationActivity, "Translation saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@TranslationActivity, "Translation cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
                
                btnDelete.setOnClickListener {
                    AlertDialog.Builder(this@TranslationActivity)
                        .setTitle("Delete Translation")
                        .setMessage("Are you sure you want to delete this translation?")
                        .setPositiveButton("Delete") { _, _ ->
                            TranslationManager.removeCustomTranslation(currentLanguage, item.key)
                            translations.removeAt(adapterPosition)
                            notifyItemRemoved(adapterPosition)
                            Toast.makeText(this@TranslationActivity, "Translation deleted", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }
    
    data class TranslationItem(
        val key: String,
        var value: String
    )
}
