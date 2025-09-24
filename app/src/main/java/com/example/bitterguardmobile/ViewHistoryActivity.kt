package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast

class ViewHistoryActivity : AppCompatActivity() {
    
    private lateinit var historyManager: HistoryManager
    private lateinit var historyAdapter: HistoryAdapter
    private var allHistoryItems: List<HistoryItem> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_history)

        // Initialize history manager
        historyManager = HistoryManager(this)

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Scan History"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Add clear history button to toolbar
        supportActionBar?.setDisplayShowCustomEnabled(true)
        val clearButton = android.widget.Button(this)
        clearButton.text = "Clear"
        clearButton.setTextColor(resources.getColor(android.R.color.white))
        clearButton.setOnClickListener {
            clearHistory()
        }
        supportActionBar?.customView = clearButton

        // Set up search functionality
        setupSearch()

        // Set up RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Load and display history
        loadHistory()
        
        historyAdapter = HistoryAdapter(allHistoryItems) { historyItem ->
            // Navigate to scan result when item is clicked
            navigateToScanResult(historyItem)
        }
        recyclerView.adapter = historyAdapter
    }
    
    private fun loadHistory() {
        val scanResults = historyManager.getScanHistory()
        allHistoryItems = scanResults.map { scanResult ->
            HistoryItem(
                imageByteArray = scanResult.imageByteArray,
                prediction = scanResult.prediction,
                confidence = scanResult.confidence,
                time = scanResult.time,
                date = scanResult.date,
                place = scanResult.location
            )
        }
    }

    private fun setupSearch() {
        val searchEditText = findViewById<EditText>(R.id.searchHistory)
        searchEditText.setOnClickListener {
            // Enable focus and show keyboard when clicked
            searchEditText.isFocusable = true
            searchEditText.isFocusableInTouchMode = true
            searchEditText.requestFocus()
        }
        
        // Add text change listener for search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterHistory(s.toString())
            }
        })
    }
    
    private fun filterHistory(query: String) {
        val filteredItems = if (query.isBlank()) {
            allHistoryItems
        } else {
            allHistoryItems.filter { item ->
                item.prediction.contains(query, ignoreCase = true) ||
                item.confidence.contains(query, ignoreCase = true) ||
                item.place.contains(query, ignoreCase = true)
            }
        }
        historyAdapter.updateItems(filteredItems)
    }

    private fun clearHistory() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Are you sure you want to clear all scan history?")
            .setPositiveButton("Clear") { _, _ ->
                historyManager.clearHistory()
                loadHistory()
                historyAdapter.updateItems(allHistoryItems)
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToScanResult(historyItem: HistoryItem) {
        // Convert HistoryItem back to ScanResult for navigation
        val scanResult = ScanResult(
            prediction = historyItem.prediction,
            confidence = historyItem.confidence,
            imageByteArray = historyItem.imageByteArray,
            imageUriString = null, // History items store byte arrays, not URIs
            timestamp = System.currentTimeMillis(), // We don't have original timestamp
            location = historyItem.place
        )
        
        val intent = Intent(this, ScanResultActivity::class.java).apply {
            putExtra("scan_result", scanResult)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    data class HistoryItem(
        val imageByteArray: ByteArray?,
        val prediction: String,
        val confidence: String,
        val time: String,
        val date: String,
        val place: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HistoryItem

            if (prediction != other.prediction) return false
            if (confidence != other.confidence) return false
            if (time != other.time) return false
            if (date != other.date) return false
            if (place != other.place) return false

            return true
        }

        override fun hashCode(): Int {
            var result = prediction.hashCode()
            result = 31 * result + confidence.hashCode()
            result = 31 * result + time.hashCode()
            result = 31 * result + date.hashCode()
            result = 31 * result + place.hashCode()
            return result
        }
    }

    class HistoryAdapter(
        private var historyItems: List<HistoryItem>,
        private val onItemClick: (HistoryItem) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        fun updateItems(newItems: List<HistoryItem>) {
            historyItems = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return HistoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            holder.bind(historyItems[position], onItemClick)
        }

        override fun getItemCount(): Int = historyItems.size

        class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView.findViewById(R.id.historyImage)
            private val predictionText: TextView = itemView.findViewById(R.id.predictionText)
            private val confidenceText: TextView = itemView.findViewById(R.id.confidenceText)
            private val timeText: TextView = itemView.findViewById(R.id.timeText)
            private val dateText: TextView = itemView.findViewById(R.id.dateText)
            private val placeText: TextView = itemView.findViewById(R.id.placeText)

            fun bind(item: HistoryItem, onItemClick: (HistoryItem) -> Unit) {
                // Set image: use bitmap if present, else fallback
                if (item.imageByteArray != null && item.imageByteArray.isNotEmpty()) {
                    try {
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                            item.imageByteArray, 0, item.imageByteArray.size
                        )
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                        } else {
                            imageView.setImageResource(R.drawable.camera)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ViewHistoryActivity", "Error decoding image: ${e.message}")
                        imageView.setImageResource(R.drawable.camera)
                    }
                } else {
                    imageView.setImageResource(R.drawable.camera)
                }
                
                predictionText.text = item.prediction
                confidenceText.text = "Confidence: ${item.confidence}"
                timeText.text = item.time
                dateText.text = item.date
                placeText.text = item.place
                
                // Set click listener for the entire item
                itemView.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }
} 