package com.example.bitterguardmobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TreatmentGuideActivity : BaseActivity() {
    
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_treatment_guide)
        
        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.treatment_guide)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TreatmentAdapter(getTreatmentData())
    }
    
    private fun getTreatmentData(): List<TreatmentItem> {
        return listOf(
            TreatmentItem(
                disease = getString(R.string.fusarium_wilt),
                treatment = "• Remove and destroy infected plants\n• Use disease-resistant varieties\n• Apply fungicides containing thiophanate-methyl\n• Improve soil drainage\n• Rotate crops",
                prevention = "• Use certified disease-free seeds\n• Maintain proper plant spacing\n• Avoid overwatering\n• Monitor plants regularly",
                severity = getString(R.string.severity_high),
                color = "#F44336"
            ),
            TreatmentItem(
                disease = getString(R.string.downy_mildew),
                treatment = "• Apply copper-based fungicides\n• Remove infected leaves\n• Improve air circulation\n• Use neem oil spray\n• Apply mancozeb fungicide",
                prevention = "• Water at the base, avoid wetting leaves\n• Maintain proper spacing\n• Use resistant varieties\n• Monitor humidity levels",
                severity = getString(R.string.severity_medium),
                color = "#FF9800"
            ),
            TreatmentItem(
                disease = getString(R.string.mosaic_virus),
                treatment = "• Remove and destroy infected plants\n• Control aphid populations\n• Use virus-free seeds\n• Apply insecticidal soap\n• Remove weed hosts",
                prevention = "• Use virus-free planting material\n• Control insect vectors\n• Maintain clean garden tools\n• Remove infected plants immediately",
                severity = getString(R.string.severity_high),
                color = "#9C27B0"
            )
        )
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    data class TreatmentItem(
        val disease: String,
        val treatment: String,
        val prevention: String,
        val severity: String,
        val color: String
    )
    
    class TreatmentAdapter(private val treatmentItems: List<TreatmentItem>) : 
        RecyclerView.Adapter<TreatmentAdapter.TreatmentViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreatmentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_treatment_guide, parent, false)
            return TreatmentViewHolder(view)
        }

        override fun onBindViewHolder(holder: TreatmentViewHolder, position: Int) {
            holder.bind(treatmentItems[position])
        }

        override fun getItemCount(): Int = treatmentItems.size

        class TreatmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val diseaseText: TextView = itemView.findViewById(R.id.diseaseName)
            private val treatmentText: TextView = itemView.findViewById(R.id.treatmentText)
            private val preventionText: TextView = itemView.findViewById(R.id.preventionText)
            private val severityText: TextView = itemView.findViewById(R.id.severityText)
            private val severityIndicator: View = itemView.findViewById(R.id.severityIndicator)

            fun bind(item: TreatmentItem) {
                diseaseText.text = item.disease
                treatmentText.text = item.treatment
                preventionText.text = item.prevention
                severityText.text = item.severity
                severityIndicator.setBackgroundColor(android.graphics.Color.parseColor(item.color))
            }
        }
    }
} 