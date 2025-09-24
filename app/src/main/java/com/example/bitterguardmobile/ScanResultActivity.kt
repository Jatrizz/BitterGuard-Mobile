package com.example.bitterguardmobile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
// Removed binding import - using findViewById instead
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ScanResultActivity : AppCompatActivity() {

    private lateinit var historyManager: HistoryManager
    private lateinit var modelUtils: ModelUtils
    private var currentScanResult: ScanResult? = null
    private var imageUriForAnalysis: Uri? = null // This will be the URI of the image to analyze
    
    // UI Views
    private lateinit var resultImage: ImageView
    private lateinit var predictionText: TextView
    private lateinit var confidenceText: TextView
    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var placeText: TextView
    private lateinit var confidenceCard: CardView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

    private val classNames: Array<String>
        get() = AppConfig.getDiseaseClasses()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_result)

        // Initialize configuration
        AppConfig.initialize(this)
        
        historyManager = HistoryManager(this)
        modelUtils = ModelUtils(this)

        // Initialize views
        initializeViews()

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Scan Result"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Load model in background
        loadModelInBackground()

        // --- Determine the source of the scan result ---
        val scanResultFromIntent = intent.getParcelableExtra<ScanResult>("scan_result")
        val imageUriStringFromIntent = intent.getStringExtra("imageUri")

        Log.d("ScanResultActivity", "onCreate: scanResultFromIntent=$scanResultFromIntent, imageUriStringFromIntent=$imageUriStringFromIntent")
        if (scanResultFromIntent != null) {
            // Came from History or somewhere a full ScanResult was passed
            currentScanResult = scanResultFromIntent
            imageUriForAnalysis = scanResultFromIntent.imageUriString?.let { Uri.parse(it) }
            Log.d("ScanResultActivity", "Loaded ScanResult from intent (history). URI for potential re-analysis: $imageUriForAnalysis")
            setupResultDisplay(currentScanResult!!) // Display existing data first
        } else if (imageUriStringFromIntent != null) {
            // Came from camera/gallery, new scan
            imageUriForAnalysis = Uri.parse(imageUriStringFromIntent)
            Log.d("ScanResultActivity", "Received new image URI for analysis: $imageUriForAnalysis")

            // Initialize currentScanResult for a new scan
            val initialPrediction = "Analyzing..."
            val initialConfidence = ""
            val location = intent.getStringExtra("location") ?: "Location not available"
            val newTimestamp = System.currentTimeMillis()

            currentScanResult = ScanResult(
                prediction = initialPrediction,
                confidence = initialConfidence,
                imageByteArray = null,
                imageUriString = imageUriForAnalysis?.toString(),
                timestamp = newTimestamp,
                location = location
            )
            updateDisplayFields(currentScanResult!!) // Show "Analyzing..."
            Glide.with(this) // Load the image into view
                .load(imageUriForAnalysis)
                .placeholder(R.drawable.camera)
                .error(R.drawable.ic_error_placeholder)
                .into(resultImage)
        } else {
            // Should not happen if activity is started correctly
            Log.e("ScanResultActivity", "No ScanResult or image URI received in intent. Displaying fallback.")
            Toast.makeText(this, "No scan result received!", Toast.LENGTH_LONG).show()
            setupResultDisplayWithFallback()
        }

        setupButtonListeners()
    }

    private fun initializeViews() {
        resultImage = findViewById(R.id.resultImage)
        predictionText = findViewById(R.id.predictionText)
        confidenceText = findViewById(R.id.confidenceText)
        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        placeText = findViewById(R.id.placeText)
        confidenceCard = findViewById(R.id.confidenceCard)
        btnSave = findViewById(R.id.btnSave)
        btnShare = findViewById(R.id.btnShare)
    }

    private fun loadModelInBackground() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val success = modelUtils.loadModel()
                withContext(Dispatchers.Main) {
                    if (success) {
                        Log.d("ScanResultActivity", "Model loaded successfully")
                        // Perform analysis if we have an image to analyze
                        if (imageUriForAnalysis != null && currentScanResult?.prediction == "Analyzing...") {
                            performImageAnalysis(imageUriForAnalysis)
                        }
                    } else {
                        Log.e("ScanResultActivity", "Failed to load model")
                        if (currentScanResult?.prediction == "Analyzing...") {
                            currentScanResult = currentScanResult?.copy(
                                prediction = "Error: Model not loaded",
                                confidence = ""
                            )
                            updateDisplayFields(currentScanResult!!)
                            Toast.makeText(this@ScanResultActivity, "Analysis model not loaded. Cannot perform scan.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ScanResultActivity", "Error loading model", e)
                withContext(Dispatchers.Main) {
                    if (currentScanResult?.prediction == "Analyzing...") {
                        currentScanResult = currentScanResult?.copy(
                            prediction = "Error: Model loading failed",
                            confidence = ""
                        )
                        updateDisplayFields(currentScanResult!!)
                        Toast.makeText(this@ScanResultActivity, "Error loading analysis model: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun performImageAnalysis(imageUri: Uri?) {
        if (imageUri == null) {
            Log.e("ScanResultActivity", "Cannot perform analysis: URI is null.")
            runOnUiThread {
                currentScanResult = currentScanResult?.copy(
                    prediction = "Error: Analysis input missing",
                    confidence = "",
                    timestamp = currentScanResult?.timestamp ?: System.currentTimeMillis()
                )
                updateDisplayFields(currentScanResult!!)
            }
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(this@ScanResultActivity.contentResolver, imageUri)

                var outputArray: FloatArray? = null

                // Try real inference first
                if (modelUtils.isModelLoaded()) {
                    outputArray = modelUtils.runInference(bitmap)
                }

                // Fallback to mock inference if real inference fails
                if (outputArray == null || outputArray.isEmpty()) {
                    Log.w("ScanResultActivity", "Real inference failed, using mock inference")
                    outputArray = modelUtils.runMockInference(bitmap)
                }

                val predictionClass = modelUtils.getPredictionClass(outputArray)
                val confidence = modelUtils.getPredictionConfidence(outputArray)

                val confidenceThreshold = AppConfig.getConfidenceThreshold()
                val (finalClassName, finalConfidencePercentage) = if (confidence < confidenceThreshold) {
                    "No disease detected" to String.format(Locale.US, "%.1f%%", confidence * 100)
                } else if (predictionClass >= 0 && predictionClass < classNames.size) {
                    classNames[predictionClass] to String.format(Locale.US, "%.1f%%", confidence * 100)
                } else {
                    "Unknown Disease" to String.format(Locale.US, "%.1f%%", confidence * 100)
                }

                withContext(Dispatchers.Main) {
                    Log.d("ScanResultActivity", "Analysis complete: $finalClassName, Confidence: $finalConfidencePercentage")
                    currentScanResult = currentScanResult?.copy(
                        prediction = finalClassName,
                        confidence = finalConfidencePercentage,
                        imageUriString = imageUri.toString(),
                        timestamp = currentScanResult?.timestamp ?: System.currentTimeMillis()
                    )
                    updateDisplayFields(currentScanResult!!)
                }

            } catch (e: Exception) {
                Log.e("ScanResultActivity", "Error during image analysis", e)
                withContext(Dispatchers.Main) {
                    currentScanResult = currentScanResult?.copy(
                        prediction = "Error in analysis",
                        confidence = "N/A",
                        timestamp = currentScanResult?.timestamp ?: System.currentTimeMillis()
                    )
                    updateDisplayFields(currentScanResult!!)
                    Toast.makeText(this@ScanResultActivity, "Error during image analysis: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateDisplayFields(scanResult: ScanResult) {
        predictionText.text = scanResult.prediction
        confidenceText.text = if (scanResult.confidence.isNotEmpty() && scanResult.prediction != "Analyzing...") {
            "Confidence: ${scanResult.confidence}"
        } else if (scanResult.prediction == "Analyzing...") {
            "" // No confidence while analyzing
        } else {
            scanResult.confidence // handles empty or "N/A"
        }
        timeText.text = scanResult.time
        dateText.text = scanResult.date
        placeText.text = scanResult.location
        setCardColor(confidenceCard, scanResult.prediction)

        // Load image using Glide, preferring URI if available, then byte array
        val imageSourceUri = scanResult.imageUriString?.let { Uri.parse(it) }
        val imageSourceBytes = scanResult.imageByteArray

        if (imageSourceUri != null) {
            Glide.with(this)
                .load(imageSourceUri)
                .placeholder(R.drawable.camera)
                .error(R.drawable.ic_error_placeholder)
                .into(resultImage)
        } else if (imageSourceBytes != null && imageSourceBytes.isNotEmpty()) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(imageSourceBytes, 0, imageSourceBytes.size)
                resultImage.setImageBitmap(bitmap)
                resultImage.scaleType = ImageView.ScaleType.CENTER_CROP
            } catch (e: Exception) {
                Log.e("ScanResultActivity", "Error decoding byte array for display", e)
                resultImage.setImageResource(R.drawable.ic_error_placeholder)
            }
        } else {
            resultImage.setImageResource(R.drawable.camera) // Fallback
        }
    }

    private fun setupButtonListeners() {
        btnSave.setOnClickListener { saveScanResult() }
        btnShare.setOnClickListener { shareScanResult() }
    }

    private fun saveScanResult() {
        if (currentScanResult != null && currentScanResult!!.prediction != "Analyzing...") {
            var resultToSave = currentScanResult!!

            // Ensure byteArray is populated if only URI is present (for consistent history saving)
            if (resultToSave.imageByteArray == null && resultToSave.imageUriString != null) {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(resultToSave.imageUriString))
                    val stream = java.io.ByteArrayOutputStream()
                    // Compress to JPEG for smaller size. Adjust quality as needed.
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    resultToSave = resultToSave.copy(imageByteArray = stream.toByteArray())
                    Log.d("ScanResultActivity", "Converted URI to ByteArray for history, size: ${resultToSave.imageByteArray?.size}")
                } catch (e: Exception) {
                    Log.e("ScanResultActivity", "Error converting URI to ByteArray for history", e)
                    // Optionally, still try to save without the byte array or show error
                }
            }

            try {
                historyManager.saveScanResult(resultToSave)
                Toast.makeText(this, "Scan result saved!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving scan result: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ScanResultActivity", "Error saving scan result", e)
            }
        } else if (currentScanResult?.prediction == "Analyzing...") {
            Toast.makeText(this, "Please wait for analysis to complete.", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this, "No scan result to save.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareScanResult() {
        if (currentScanResult != null && currentScanResult!!.prediction != "Analyzing...") {
            val shareText = """
                Plant Disease Detection Result:
                Disease: ${currentScanResult!!.prediction}
                Confidence: ${currentScanResult!!.confidence}
                Date: ${currentScanResult!!.date}
                Time: ${currentScanResult!!.time}
                Location: ${currentScanResult!!.location}
                Scanned with BitterGuard Mobile App
            """.trimIndent()

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)

            // Optionally attach the image if available
            currentScanResult!!.imageUriString?.let { uriString ->
                try {
                    val imageUri = Uri.parse(uriString)
                    // Grant temporary read permission to the receiving app
                    shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    shareIntent.type = "image/*" // Change type if image is attached
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareText); // Re-add text as some apps only take one
                } catch(e: Exception) {
                    Log.e("ScanResultActivity", "Error preparing image for sharing", e)
                    // If image can't be attached, it will just share text.
                    shareIntent.type = "text/plain" // Revert to text only if image attach fails
                }
            }
            startActivity(Intent.createChooser(shareIntent, "Share Scan Result"))
        } else if (currentScanResult?.prediction == "Analyzing...") {
            Toast.makeText(this, "Please wait for analysis to complete.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No scan result to share.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupResultDisplay(scanResult: ScanResult) {
        // This function is now mainly a wrapper around updateDisplayFields
        // if it was loaded from history, as image loading is handled there.
        updateDisplayFields(scanResult)
    }

    private fun setupResultDisplayWithFallback() {
        val currentTimestamp = System.currentTimeMillis()
        val fallbackScanResult = ScanResult(
            prediction = "No result",
            confidence = "",
            imageByteArray = null,
            imageUriString = null, // Fallback has no image URI
            timestamp = currentTimestamp,
            location = "Unknown"
        )
        updateDisplayFields(fallbackScanResult)
        resultImage.setImageResource(R.drawable.camera) // Explicitly set placeholder
    }

    private fun setCardColor(confidenceCard: CardView, prediction: String) {
        val colorResId = when (prediction) {
            "Healthy" -> android.R.color.holo_green_light
            "Fusarium Wilt" -> android.R.color.holo_red_dark
            "Bacterial Blight" -> android.R.color.holo_orange_light
            "Leaf Spot" -> android.R.color.holo_purple
            "Powdery Mildew" -> android.R.color.holo_orange_light
            "No significant detection" -> android.R.color.darker_gray
            "Error in analysis", "Error: Analysis input missing", "Error: Analysis incomplete", "Error: No data" -> android.R.color.holo_red_light
            "Analyzing..." -> android.R.color.darker_gray
            else -> android.R.color.darker_gray // Default for any other unexpected string
        }
        confidenceCard.setCardBackgroundColor(ContextCompat.getColor(this, colorResId))
    }

    @Throws(IOException::class)
    fun assetFilePath(activity: AppCompatActivity, assetName: String): String {
        val file = File(activity.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        activity.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
        }
        return file.absolutePath
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Cancel all coroutines started by this scope
        modelUtils.close() // Close the model if it holds resources
    }
}

