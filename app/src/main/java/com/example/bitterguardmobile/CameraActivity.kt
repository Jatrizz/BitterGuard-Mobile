package com.example.bitterguardmobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.text
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
//import androidx.glance.visibility
import java.io.File
import java.io.FileOutputStream
import java.io.IOException // Added for explicit exception handling

class CameraActivity : AppCompatActivity() {

    private lateinit var btnTakePhoto: Button
    private lateinit var btnChooseGallery: Button
    private lateinit var btnScan: Button
    private lateinit var cameraPreview: ImageView
    private lateinit var previewText: TextView
    private lateinit var instructionText: TextView

    // This will hold the URI of the selected/captured image to be passed to ScanResultActivity
    private var imageUriToScan: Uri? = null
    
    // Coroutine scope for location operations
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    companion object {
        // Keep for legacy permission handling if any part still uses it,
        // otherwise, can be removed if all permission requests use launchers.
        private const val CAMERA_PERMISSION_REQUEST_LEGACY_CALLBACK = 100
    }

    // --- ActivityResultLaunchers ---
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCameraInternal()
            } else {
                showPermissionDeniedDialog(
                    "Camera Permission Required",
                    "Camera permission is required to take photos. Please enable it in app settings to use this feature."
                ) // Default behavior is to show settings
            }
        }

    private val requestGalleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openGalleryInternal()
            } else {
                showPermissionDeniedDialog(
                    "Gallery Permission Required",
                    "Gallery permission is required to select images. Please enable it in app settings to use this feature."
                ) // Default behavior is to show settings
            }
        }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Location permission granted, proceed with scan
                navigateToScanResultWithLocation()
            } else {
                // Location permission denied, proceed without location
                navigateToScanResultWithLocation()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (currentPhotoUri != null) {
                    imageUriToScan = currentPhotoUri
                    updatePreviewWithUri(imageUriToScan!!)
                    Toast.makeText(this, getString(R.string.photo_captured_successfully), Toast.LENGTH_SHORT).show()
                    currentPhotoUri = null
                } else {
                    // Fallback for devices returning a thumbnail only (rare once EXTRA_OUTPUT is used)
                    val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                    if (imageBitmap != null) {
                        imageUriToScan = saveBitmapToTempFile(imageBitmap)
                        imageUriToScan?.let { updatePreviewWithUri(it) }
                    } else {
                        Toast.makeText(this, getString(R.string.camera_error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    private val selectImageFromGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    imageUriToScan = uri
                    updatePreviewWithUri(uri)
                    Toast.makeText(this, getString(R.string.image_selected), Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(this, getString(R.string.gallery_permission_denied), Toast.LENGTH_SHORT).show()
                }
            }
        }

    private var currentPhotoUri: Uri? = null // For storing URI when using FileProvider with camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Scan Leaf"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnChooseGallery = findViewById(R.id.btnChooseGallery)
        btnScan = findViewById(R.id.btnScan)
        btnScan.isEnabled = false // Initially disabled
        cameraPreview = findViewById(R.id.cameraPreview)
        previewText = findViewById(R.id.previewText)
        instructionText = findViewById(R.id.instructionText)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        btnTakePhoto.setOnClickListener {
            checkAndRequestCameraPermission()
        }
        btnChooseGallery.setOnClickListener {
            checkAndRequestGalleryPermission()
        }
        btnScan.setOnClickListener {
            if (imageUriToScan != null) {
                checkLocationPermissionAndNavigate()
            } else {
                Toast.makeText(this, "Please select or capture an image first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCameraInternal()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                showPermissionDeniedDialog(
                    getString(R.string.permission_required),
                    getString(R.string.camera_permission_denied)
                ) { requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA) } // Provide action to re-request
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkAndRequestGalleryPermission() {
        val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            // For older versions, READ_EXTERNAL_STORAGE is sufficient.
            // WRITE_EXTERNAL_STORAGE is generally not needed for just picking images.
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permissionToRequest) == PackageManager.PERMISSION_GRANTED -> {
                openGalleryInternal()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, permissionToRequest) -> {
                showPermissionDeniedDialog(
                    getString(R.string.permission_required),
                    getString(R.string.gallery_permission_denied)
                ) { requestGalleryPermissionLauncher.launch(permissionToRequest) } // Provide action to re-request
            }
            else -> {
                requestGalleryPermissionLauncher.launch(permissionToRequest)
            }
        }
    }

    private fun openCameraInternal() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Log.e("CameraActivity", "Error creating image file", ex)
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        it
                    )
                    currentPhotoUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    // Grant URI permissions to camera activity
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    takePictureLauncher.launch(takePictureIntent)
                } ?: run {
                    Toast.makeText(this, "Unable to prepare storage for photo.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir != null && !storageDir.exists()) storageDir.mkdirs()
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }


    private fun openGalleryInternal() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        selectImageFromGalleryLauncher.launch(intent)
    }

    private fun updatePreviewWithUri(uri: Uri) {
        try {
            // Display image as-is, respecting EXIF orientation using Glide
            Glide.with(this)
                .load(uri)
                .dontTransform()
                .into(cameraPreview)
            cameraPreview.scaleType = ImageView.ScaleType.FIT_CENTER
            cameraPreview.adjustViewBounds = true
            previewText.visibility = View.GONE // Hide "No image selected"
            instructionText.text = "Image ready to scan"
            btnScan.isEnabled = true
        } catch (e: Exception) {
            Log.e("CameraActivity", "Error updating preview with URI: $uri", e)
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            previewText.visibility = View.VISIBLE
            previewText.text = "Error loading image"
            instructionText.text = "Please try selecting another image."
            btnScan.isEnabled = false
        }
    }

    private fun checkLocationPermissionAndNavigate() {
        if (LocationService.hasLocationPermission(this)) {
            navigateToScanResultWithLocation()
        } else {
            // Ask for location permission
            AlertDialog.Builder(this)
                .setTitle("Location Permission (Optional)")
                .setMessage("Allow access to your location to tag your scan with its location. This is optional.")
                .setPositiveButton("Allow") { _, _ ->
                    requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                .setNegativeButton("Skip") { _, _ ->
                    navigateToScanResultWithLocation()
                }
                .show()
        }
    }

    private fun navigateToScanResultWithLocation() {
        if (imageUriToScan == null) {
            Toast.makeText(this, "No image to scan", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            try {
                val locationString = LocationService.getLocationString(this@CameraActivity)
                
                val intent = Intent(this@CameraActivity, ScanResultActivity::class.java).apply {
                    putExtra("imageUri", imageUriToScan.toString())
                    putExtra("location", locationString)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("CameraActivity", "Error getting location: ${e.message}")
                // Proceed without location
                val intent = Intent(this@CameraActivity, ScanResultActivity::class.java).apply {
                    putExtra("imageUri", imageUriToScan.toString())
                    putExtra("location", "Location not available")
                }
                startActivity(intent)
            }
        }
    }

    private fun saveBitmapToTempFile(bitmap: Bitmap): Uri? {
        val imagesDir = File(cacheDir, "images_temp") // Use a dedicated temp sub-directory in cache
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "temp_capture_${System.currentTimeMillis()}.jpg")

        return try {
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", imageFile)
        } catch (e: IOException) {
            Log.e("CameraActivity", "Error saving bitmap to temp file", e)
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // This is now mainly a fallback or for other permission requests not using launchers.
        // The ActivityResultLaunchers for permissions have their own callbacks.
        if (requestCode == CAMERA_PERMISSION_REQUEST_LEGACY_CALLBACK) { // Example for a non-launcher request
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Action for this legacy permission
                openCameraInternal() // Or whatever the action was
            } else {
                showPermissionDeniedDialog(
                    getString(R.string.permission_required),
                    getString(R.string.permission_denied)
                )
            }
        }
    }

    private fun showPermissionDeniedDialog(title: String, message: String, retryAction: (() -> Unit)? = null) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(if (retryAction != null) "Retry" else "Settings") { dialog, _ ->
                if (retryAction != null) {
                    retryAction.invoke()
                } else {
                    // Open app settings
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        startActivity(this)
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                if (retryAction != null) { // Only show "feature unavailable" if it was a retryable action
                    Toast.makeText(this, "Feature unavailable without permission.", Toast.LENGTH_SHORT).show()
                }
            }
            .setCancelable(false) // User must interact with the dialog
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
