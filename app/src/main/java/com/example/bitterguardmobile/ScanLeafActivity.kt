package com.example.bitterguardmobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
//import androidx.glance.visibility
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanLeafActivity : AppCompatActivity() {

    private lateinit var btnCamera: Button
    private lateinit var btnGallery: Button
    private lateinit var btnScan: Button
    // Test buttons - consider removing for production or placing under a debug flag
    // Debug buttons removed for production

    private lateinit var cameraPreview: ImageView
    private lateinit var previewText: TextView
    private lateinit var instructionText: TextView

    private var imageUriToScan: Uri? = null // Single URI for the image to be processed
    private var currentPhotoUri: Uri? = null // Temp URI for camera capture using FileProvider

    private lateinit var modelUtils: ModelUtils
    private lateinit var imageStorageManager: ImageStorageManager
    private lateinit var loadingDialog: LoadingDialog
    // private lateinit var historyManager: HistoryManager // HistoryManager seems unused here, but was in original

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // --- ActivityResultLaunchers ---
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestGalleryPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestLocationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectImageFromGalleryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ScanLeafActivity", "onCreate called")
        if (savedInstanceState != null) {
            Log.d("ScanLeafActivity", "Activity is being recreated from savedInstanceState")
            Toast.makeText(this, "Activity recreated!", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("ScanLeafActivity", "Activity is being created fresh")
        }

        // Global exception handler for debugging
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("ScanLeafActivity", "Uncaught exception in thread ${thread.name}", throwable)
            runOnUiThread {
                Toast.makeText(this, "Crash: ${throwable.message}", Toast.LENGTH_LONG).show()
            }
        }

        setContentView(R.layout.activity_scan_leaf)
        initializeViews()
        initializeLaunchers() // Initialize launchers before they are used
        modelUtils = ModelUtils(this)
        imageStorageManager = ImageStorageManager(this)
        loadingDialog = LoadingDialog(this)
        setupToolbar()
        setupClickListeners()
        loadModelInBackground()
        Log.d("ScanLeafActivity", "onCreate completed")
    }

    override fun onResume() {
        super.onResume()
        Log.d("ScanLeafActivity", "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d("ScanLeafActivity", "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.d("ScanLeafActivity", "onStop called")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("ScanLeafActivity", "onRestart called")
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("ScanLeafActivity", "Configuration changed")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("ScanLeafActivity", "onSaveInstanceState called")
    }

    private fun initializeViews() {
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)
        btnScan = findViewById(R.id.btnScan)
        // Debug buttons removed for production
        cameraPreview = findViewById(R.id.cameraPreview)
        previewText = findViewById(R.id.previewText)
        instructionText = findViewById(R.id.instructionText)
        btnScan.isEnabled = false // Disable initially
    }

    private fun initializeLaunchers() {
        requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                Log.d("ScanLeafActivity", "Camera permission result: $isGranted")
                if (isGranted) {
                    Log.d("ScanLeafActivity", "Camera permission granted, opening camera")
                    openCameraInternal()
                } else {
                    Log.d("ScanLeafActivity", "Camera permission denied")
                    showPermissionDeniedDialog("Camera Permission Required", "Camera permission is needed to take photos.")
                }
            }

        requestGalleryPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    openGalleryInternal()
                } else {
                    showPermissionDeniedDialog("Gallery Permission Required", "Gallery permission is needed to select images.")
                }
            }

        requestLocationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    Toast.makeText(this, "Location permission granted. Location will be included.", Toast.LENGTH_SHORT).show()
                    // Optionally, trigger something that depends on location being granted now
                } else {
                    Toast.makeText(this, "Location permission denied. Location will not be included.", Toast.LENGTH_LONG).show()
                }
            }

        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                Log.d("ScanLeafActivity", "Camera result received: ${result.resultCode}")
                Log.d("ScanLeafActivity", "currentPhotoUri at result: $currentPhotoUri")
                if (isDestroyed) {
                    Log.d("ScanLeafActivity", "Activity is destroyed, ignoring camera result")
                    return@registerForActivityResult
                }
                if (result.resultCode == Activity.RESULT_OK) {
                    Log.d("ScanLeafActivity", "Camera capture successful")
                    currentPhotoUri?.let { uri ->
                        Log.d("ScanLeafActivity", "Processing captured photo: $uri")
                        imageUriToScan = uri
                        updatePreviewWithUri(uri)
                        Toast.makeText(this, "Photo captured successfully!", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Log.e("ScanLeafActivity", "currentPhotoUri is null after camera, showing error dialog")
                        showErrorDialog("Failed to capture photo. Please try again.")
                    }
                    currentPhotoUri = null // Reset for next capture
                } else {
                    Log.d("ScanLeafActivity", "Camera capture cancelled or failed: ${result.resultCode}")
                    currentPhotoUri = null
                }
            }

        selectImageFromGalleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        imageUriToScan = uri
                        updatePreviewWithUri(uri)
                        Toast.makeText(this, "Image selected!", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Toast.makeText(this, "Failed to get image from gallery.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Gallery selection cancelled.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Scan Leaf"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadModelInBackground() {
        loadingDialog.show("Loading AI Model...", "Initializing disease detection system")
        coroutineScope.launch(Dispatchers.IO) {
            val success = modelUtils.loadModel()
            withContext(Dispatchers.Main) {
                loadingDialog.hide()
                if (success) {
                    Toast.makeText(this@ScanLeafActivity, "AI model loaded successfully", Toast.LENGTH_SHORT).show()
                    Log.d("ScanLeafActivity", "Model loaded successfully")
                } else {
                    Toast.makeText(this@ScanLeafActivity, "Using fallback detection mode", Toast.LENGTH_LONG).show()
                    Log.w("ScanLeafActivity", "Model loading failed, will use mock inference")
                    // The app will still work with mock inference
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnCamera.setOnClickListener { 
            Log.d("ScanLeafActivity", "Camera button clicked")
            checkAndRequestCameraPermission() 
        }
        btnGallery.setOnClickListener { 
            Log.d("ScanLeafActivity", "Gallery button clicked")
            checkAndRequestGalleryPermission() 
        }
        btnScan.setOnClickListener {
            Log.d("ScanLeafActivity", "Scan button clicked")
            imageUriToScan?.let {
                // Request location permission before running inference if not already granted
                checkAndRequestLocationPermissionThenRunInference(it)
            } ?: Toast.makeText(this, "Please select or capture an image first", Toast.LENGTH_SHORT).show()
        }
        // Debug button listeners removed for production
    }

    private fun checkAndRequestCameraPermission() {
        Log.d("ScanLeafActivity", "Checking camera permission")
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun checkAndRequestGalleryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestGalleryPermissionLauncher.launch(permission)
    }

    private fun checkAndRequestLocationPermissionThenRunInference(imageUri: Uri) {
        Log.d("ScanLeafActivity", "checkAndRequestLocationPermissionThenRunInference called with URI: $imageUri")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("ScanLeafActivity", "Location permission already granted, obtaining best/one-time location then running inference")
            val best = getBestLastKnownLocation()
            if (best != null) {
                runModelInference(imageUri, "Lat: ${String.format("%.4f", best.latitude)}, Lon: ${String.format("%.4f", best.longitude)}")
            } else {
                requestSingleLocationFix { fresh ->
                    val locString = if (fresh != null) {
                        "Lat: ${String.format("%.4f", fresh.latitude)}, Lon: ${String.format("%.4f", fresh.longitude)}"
                    } else "Location not available"
                    runOnUiThread { runModelInference(imageUri, locString) }
                }
            }
        } else {
            Log.d("ScanLeafActivity", "Location permission not granted, showing dialog")
            // Explain why location is needed (optional, but good practice)
            AlertDialog.Builder(this)
                .setTitle("Location Permission (Optional)")
                .setMessage("Granting location permission allows us to tag your scan with its location. This is optional.")
                .setPositiveButton("Grant") { _, _ ->
                    Log.d("ScanLeafActivity", "User granted location permission")
                    requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    // Note: Inference will run after permission result if granted.
                    // For simplicity here, we might just proceed without location if denied immediately after.
                    // A better UX would be to re-trigger inference post-permission dialog.
                    // For now, if denied, it will use "Location not available".
                    // We'll call runModelInference again in the launcher callback IF granted.
                    // Or, simpler: just get location (which will return "not available" if denied) and proceed.
                    val best = getBestLastKnownLocation()
                    if (best != null) {
                        runModelInference(imageUri, "Lat: ${String.format("%.4f", best.latitude)}, Lon: ${String.format("%.4f", best.longitude)}")
                    } else {
                        requestSingleLocationFix { fresh ->
                            val locString = if (fresh != null) {
                                "Lat: ${String.format("%.4f", fresh.latitude)}, Lon: ${String.format("%.4f", fresh.longitude)}"
                            } else "Location not available"
                            runOnUiThread { runModelInference(imageUri, locString) }
                        }
                    }
                }
                .setNegativeButton("Skip") { _, _ ->
                    Log.d("ScanLeafActivity", "User skipped location permission, running inference without location")
                    runModelInference(imageUri, "Location not provided")
                }
                .setNeutralButton("Settings") { _, _ ->
                    Log.d("ScanLeafActivity", "User chose to go to settings")
                    openAppSettings() // If they previously denied with "don't ask again"
                }
                .show()
        }
    }


    private fun openCameraInternal() {
        Log.d("ScanLeafActivity", "Opening camera (cache-only minimal version)...")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            val cameraApp = takePictureIntent.resolveActivity(packageManager)
            if (cameraApp != null) {
                Log.d("ScanLeafActivity", "Camera app found: ${cameraApp.className}")
                try {
                    val photoFile = createImageFileInCache()
                    Log.d("ScanLeafActivity", "Created photo file in cache: ${photoFile.absolutePath}")
                    currentPhotoUri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        photoFile
                    )
                    Log.d("ScanLeafActivity", "FileProvider URI: $currentPhotoUri")
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    Log.d("ScanLeafActivity", "Launching camera with intent: $takePictureIntent")
                    takePictureLauncher.launch(takePictureIntent)
                } catch (ex: Exception) {
                    Log.e("ScanLeafActivity", "Error launching camera (cache-only)", ex)
                    Toast.makeText(this, "Error launching camera: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("ScanLeafActivity", "No camera app found")
                Toast.makeText(this, "No camera app found on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGalleryInternal() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        selectImageFromGalleryLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES) // Or cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            // currentPhotoPath = absolutePath // If you need it
        }
    }

    @Throws(IOException::class)
    private fun createImageFileInCache(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun updatePreviewWithUri(uri: Uri) {
        Log.d("ScanLeafActivity", "updatePreviewWithUri called with URI: $uri")
        try {
            // cameraPreview.setImageURI(uri) // Can be slow for large images, or cause OOMs
            // Load bitmap efficiently for preview
            Log.d("ScanLeafActivity", "Loading bitmap from URI...")
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri) // Consider downsampling for preview if very large
            Log.d("ScanLeafActivity", "Bitmap loaded successfully: ${bitmap.width}x${bitmap.height}")
            cameraPreview.setImageBitmap(bitmap)
            cameraPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            previewText.visibility = View.GONE
            instructionText.text = "Image ready. Tap 'Scan Leaf' to analyze."
            btnScan.isEnabled = true
            Log.d("ScanLeafActivity", "Preview updated successfully")
        } catch (e: Exception) {
            Log.e("ScanLeafActivity", "Error updating preview with URI: $uri", e)
            previewText.text = "Error loading preview"
            previewText.visibility = View.VISIBLE
            instructionText.text = "Please try another image."
            btnScan.isEnabled = false
            Toast.makeText(this, "Error displaying image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runModelInference(imageUri: Uri, location: String) {
        Log.d("ScanLeafActivity", "runModelInference called with URI: $imageUri, location: $location")
        val bitmap: Bitmap? = try {
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        } catch (e: Exception) {
            Log.e("ScanLeafActivity", "Error loading bitmap from URI for inference", e)
            Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show()
            null
        }

        if (bitmap == null) {
            Log.e("ScanLeafActivity", "Bitmap is null, cannot run inference")
            return
        }

        Log.d("ScanLeafActivity", "Bitmap loaded successfully: ${bitmap.width}x${bitmap.height}")
        loadingDialog.show("Analyzing Leaf...", "Detecting diseases using AI")

        coroutineScope.launch(Dispatchers.IO) {
            var diseaseName = "Analysis Error"
            var confidencePercent = "N/A"
            var isUsingMockInference = false

            try {
                Log.d("ScanLeafActivity", "Starting inference process")
                if (!modelUtils.isModelLoaded()) {
                    Log.w("ScanLeafActivity", "Model not loaded, using mock inference.")
                    isUsingMockInference = true
                    // Fallback to mock inference if model isn't ready
                    val mockOutput = modelUtils.runMockInference(bitmap)
                    diseaseName = getDiseaseName(modelUtils.getPredictionClass(mockOutput))
                    confidencePercent = String.format("%.1f%%", modelUtils.getPredictionConfidence(mockOutput) * 100)
                } else {
                    val outputArray = modelUtils.runInference(bitmap)
                    if (outputArray != null && outputArray.isNotEmpty()) {
                        // Check if this is the special "no bitter gourd leaf" marker
                        if (outputArray.size == 4 && outputArray.all { it == -1.0f }) {
                            diseaseName = "Not a Bitter Gourd Leaf"
                            confidencePercent = "N/A"
                            Log.d("ScanLeafActivity", "Result: Not a Bitter Gourd Leaf")
                        } else {
                            // Normal disease detection result
                            val predictionClass = modelUtils.getPredictionClass(outputArray)
                            val confidence = modelUtils.getPredictionConfidence(outputArray)
                            diseaseName = getDiseaseName(predictionClass)
                            confidencePercent = String.format("%.1f%%", confidence * 100)
                            Log.d("ScanLeafActivity", "Disease detection result: $diseaseName (${confidencePercent})")
                        }
                        Log.d("ScanLeafActivity", "Two-stage inference result: $diseaseName, Confidence: $confidencePercent")
                    } else {
                        Log.w("ScanLeafActivity", "Real inference failed or returned empty, using mock as fallback.")
                        isUsingMockInference = true
                        val mockOutput = modelUtils.runMockInference(bitmap) // Fallback
                        diseaseName = getDiseaseName(modelUtils.getPredictionClass(mockOutput))
                        confidencePercent = String.format("%.1f%%", modelUtils.getPredictionConfidence(mockOutput) * 100)
                    }
                }
            } catch (e: Exception) {
                Log.e("ScanLeafActivity", "Error during inference: ${e.message}", e)
                isUsingMockInference = true
                // Use mock inference as a final fallback on any error during the try block
                val mockOutput = modelUtils.runMockInference(bitmap)
                diseaseName = getDiseaseName(modelUtils.getPredictionClass(mockOutput))
                confidencePercent = String.format("%.1f%%", modelUtils.getPredictionConfidence(mockOutput) * 100)
            } finally {
                withContext(Dispatchers.Main) {
                    Log.d("ScanLeafActivity", "Inference completed, hiding loading dialog")
                    loadingDialog.hide()
                    
                    // Show appropriate message based on inference type
                    if (isUsingMockInference) {
                        Toast.makeText(this@ScanLeafActivity, "Using demo mode for analysis", Toast.LENGTH_SHORT).show()
                    }
                    
                    Log.d("ScanLeafActivity", "Navigating to scan result screen")
                    navigateToScanResultScreen(diseaseName, confidencePercent, imageUri, location)
                }
            }
        }
    }


    // Debug test methods removed for production

    private fun getDiseaseName(classIndex: Int): String {
        return when (classIndex) {
            0 -> "Downey Mildew"
            1 -> "Fresh Leaf"
            2 -> "Fusarium Wilt"
            3 -> "Mosaic Virus"
            else -> "Unknown ( $classIndex)" // Include index for debugging unknown classes
        }
    }

    private fun navigateToScanResultScreen(diseaseName: String, confidence: String, imageUri: Uri, location: String) {
        Log.d("ScanLeafActivity", "navigateToScanResultScreen called with disease: $diseaseName, confidence: $confidence")
        Log.d("ScanLeafActivity", "Image URI: $imageUri, Location: $location")
        
        // Store scan image locally for privacy
        coroutineScope.launch {
            try {
                val fileName = "scan_${System.currentTimeMillis()}.jpg"
                val result = imageStorageManager.storeScanImageLocally(imageUri, fileName)
                
                if (result.isSuccess) {
                    Log.d("ScanLeafActivity", "Scan image stored locally: ${result.getOrNull()}")
                } else {
                    Log.e("ScanLeafActivity", "Failed to store scan image locally: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("ScanLeafActivity", "Error storing scan image: ${e.message}")
            }
        }
        
        val scanResult = ScanResult(
            prediction = diseaseName,
            confidence = confidence,
            imageUriString = imageUri.toString(), // Pass URI as string
            imageByteArray = null, // Set to null, ScanResultActivity will load from URI
            location = location,
            timestamp = System.currentTimeMillis() // Add timestamp
        )

        Log.d("ScanLeafActivity", "Created ScanResult: $scanResult")

        val intent = Intent(this, ScanResultActivity::class.java).apply {
            putExtra("scan_result", scanResult) // Use the correct key that ScanResultActivity expects
        }
        
        Log.d("ScanLeafActivity", "Created intent: $intent")
        Log.d("ScanLeafActivity", "Starting ScanResultActivity...")
        
        try {
            startActivity(intent)
            Log.d("ScanLeafActivity", "ScanResultActivity started successfully")
        } catch (e: Exception) {
            Log.e("ScanLeafActivity", "Error starting ScanResultActivity", e)
            Toast.makeText(this, "Error navigating to results: ${e.message}", Toast.LENGTH_LONG).show()
        }
        // finish() // Consider if you want to finish this activity
    }

    private fun getCurrentLocation(): String {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("ScanLeafActivity", "Location permission not granted. Requesting...")
            // Permission will be requested by checkAndRequestLocationPermissionThenRunInference
            // If called directly without prior check (e.g. for testing), it should ask.
            // For now, if no permission, return "not available"
            return "Location not available (Permission Denied)"
        }

        return try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)
            var bestLocation: android.location.Location? = null

            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }

            bestLocation?.let {
                "Lat: ${String.format("%.4f", it.latitude)}, Lon: ${String.format("%.4f", it.longitude)}"
            } ?: "Location not available (No provider)"
        } catch (e: SecurityException) {
            Log.e("ScanLeafActivity", "Error getting location (SecurityException): ${e.message}")
            "Location not available (Error)"
        } catch (e: Exception) {
            Log.e("ScanLeafActivity", "Error getting location: ${e.message}")
            "Location not available (Error)"
        }
    }

    private fun getBestLastKnownLocation(): android.location.Location? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        var bestLocation: android.location.Location? = null
        for (provider in providers) {
            val loc = try { locationManager.getLastKnownLocation(provider) } catch (_: SecurityException) { null } ?: continue
            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                bestLocation = loc
            }
        }
        return bestLocation
    }

    private fun requestSingleLocationFix(onResult: (android.location.Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onResult(null)
            return
        }
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                locationManager.removeUpdates(this)
                onResult(location)
            }
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener)
            // Fallback timeout: stop after 10s if no fix
            window.decorView.postDelayed({
                try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
                onResult(null)
            }, 10000)
        } catch (_: SecurityException) {
            onResult(null)
        }
    }


    private fun showPermissionDeniedDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("$message Please enable it in app settings to use this feature.")
            .setPositiveButton("Settings") { _, _ -> openAppSettings() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        // onBackPressedDispatcher.onBackPressed() // Use this for modern handling
        super.onBackPressed() // Or keep the old one if it works for your navigation setup
        return true
    }

    // onDestroy is a good place to cancel ongoing coroutines
    override fun onDestroy() {
        super.onDestroy()
        Log.d("ScanLeafActivity", "onDestroy called!")
        coroutineScope.cancel() // Cancel all coroutines started by this scope
        modelUtils.close() // Close the model if it holds resources like TFLite interpreter
    }

    override fun finish() {
        Log.d("ScanLeafActivity", "finish() called!")
        super.finish()
    }

    override fun onBackPressed() {
        Log.d("ScanLeafActivity", "onBackPressed called!")
        super.onBackPressed()
    }
}
