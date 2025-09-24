package com.example.bitterguardmobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DiseaseInfoActivity : BaseActivity() {
    
    private lateinit var searchEditText: EditText
    private lateinit var diseaseCard1: CardView
    private lateinit var diseaseCard2: CardView
    private lateinit var diseaseCard3: CardView
    private lateinit var commonDiseasesSection: TextView
    private lateinit var preventionTipsSection: TextView
    private lateinit var preventionCard: CardView
    
    // Disease data for search
    private val diseaseData = listOf(
        DiseaseInfo("Fusarium Wilt", "A fungal disease that causes wilting and yellowing of leaves", "High", R.drawable.fusarium),
        DiseaseInfo("Downy Mildew", "A fungal disease causing yellow spots and white growth on leaves", "Medium", R.drawable.downy),
        DiseaseInfo("Mosaic Virus", "A viral disease causing mottled patterns and stunted growth", "High", R.drawable.mosaic)
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease_info)

        // Initialize views
        initializeViews()

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.disease_information)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up search functionality
        setupSearch()
        
        // Set up disease card click listeners
        setupDiseaseCards()
    }
    
    private fun initializeViews() {
        searchEditText = findViewById(R.id.searchDiseases)
        diseaseCard1 = findViewById(R.id.diseaseCard1)
        diseaseCard2 = findViewById(R.id.diseaseCard2)
        diseaseCard3 = findViewById(R.id.diseaseCard3)
        // Note: These IDs don't exist in the layout, we'll handle search differently
    }

    private fun setupSearch() {
        searchEditText.setOnClickListener {
            // Enable focus and show keyboard when clicked
            searchEditText.isFocusable = true
            searchEditText.isFocusableInTouchMode = true
            searchEditText.requestFocus()
        }
        
        // Add text change listener for real-time search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterDiseases(s.toString())
            }
        })
    }
    
    private fun filterDiseases(query: String) {
        val searchQuery = query.lowercase().trim()
        
        if (searchQuery.isEmpty()) {
            // Show all diseases
            diseaseCard1.visibility = View.VISIBLE
            diseaseCard2.visibility = View.VISIBLE
            diseaseCard3.visibility = View.VISIBLE
            return
        }
        
        // Filter diseases based on search query
        val fusariumMatches = diseaseData[0].name.lowercase().contains(searchQuery) || 
                             diseaseData[0].description.lowercase().contains(searchQuery)
        val downyMatches = diseaseData[1].name.lowercase().contains(searchQuery) || 
                          diseaseData[1].description.lowercase().contains(searchQuery)
        val mosaicMatches = diseaseData[2].name.lowercase().contains(searchQuery) || 
                           diseaseData[2].description.lowercase().contains(searchQuery)
        
        diseaseCard1.visibility = if (fusariumMatches) View.VISIBLE else View.GONE
        diseaseCard2.visibility = if (downyMatches) View.VISIBLE else View.GONE
        diseaseCard3.visibility = if (mosaicMatches) View.VISIBLE else View.GONE
        
        // Show message if no results found
        if (!fusariumMatches && !downyMatches && !mosaicMatches) {
            android.widget.Toast.makeText(this, "No diseases found matching '$query'", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDiseaseCards() {
        // Disease Card 1 - Fusarium Wilt
        diseaseCard1.setOnClickListener {
            showDiseaseDetails(
                getString(R.string.fusarium_wilt),
                getString(R.string.fusarium_wilt_desc),
                "• Yellowing leaves\n• Wilting during day\n• Brown vascular tissue\n• Stunted growth",
                getString(R.string.severity_high)
            )
        }

        // Disease Card 2 - Downy Mildew
        diseaseCard2.setOnClickListener {
            showDiseaseDetails(
                getString(R.string.downy_mildew),
                getString(R.string.downy_mildew_desc),
                "• Yellow spots on leaves\n• White/gray growth underneath\n• Leaf curling\n• Reduced yield",
                getString(R.string.severity_medium)
            )
        }

        // Disease Card 3 - Mosaic Virus
        diseaseCard3.setOnClickListener {
            showDiseaseDetails(
                getString(R.string.mosaic_virus),
                getString(R.string.mosaic_virus_desc),
                "• Mottled leaf patterns\n• Leaf distortion\n• Stunted growth\n• Reduced fruit quality",
                getString(R.string.severity_high)
            )
        }
    }

    private fun showDiseaseDetails(name: String, description: String, symptoms: String, severity: String) {
        val message = """
            $name
            
            Description:
            $description
            
            Symptoms:
            $symptoms
            
            $severity
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(name)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { _, _ -> }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    data class DiseaseInfo(
        val name: String,
        val description: String,
        val severity: String,
        val imageResId: Int
    )
} 