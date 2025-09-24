package com.example.bitterguardmobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // For ACTION_IMAGE_CAPTURE without a pre-defined URI, the image is often returned as a Bitmap in "data" extra.
                // This is typically a thumbnail. For full-size images, use FileProvider (handled in openCameraInternal if you switch).
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    // Save this bitmap to a temp file and get its content URI
                    imageUriToScan = saveBitmapToTempFile(imageBitmap)
                    if (imageUriToScan != null) {
                        updatePreviewWithUri(imageUriToScan!!) // Update preview using the URI
                        Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to save captured photo.", Toast.LENGTH_SHORT).show()
                    }
                } else if (currentPhotoUri != null) {
                    // If openCameraInternal was set up to save to currentPhotoUri (FileProvider for full-size)
                    imageUriToScan = currentPhotoUri
                    updatePreviewWithUri(imageUriToScan!!)
                    Toast.makeText(this, "Full-size photo captured!", Toast.LENGTH_SHORT).show()
                    currentPhotoUri = null // Reset for next capture
                } else {
                    Toast.makeText(this, "Failed to get photo.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val selectImageFromGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    imageUriToScan = uri
                    updatePreviewWithUri(uri)
                    Toast.makeText(this, "Image selected!", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(this, "Failed to get image from gallery.", Toast.LENGTH_SHORT).show()
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
                navigateToScanResult(imageUriToScan!!)
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
                    "Camera Permission Required",
                    "Camera permission is essential for capturing photos. Please grant the permission to proceed."
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
                    "Gallery Permission Required",
                    "Access to your gallery is needed to select photos. Please grant the permission."
                ) { requestGalleryPermissionLauncher.launch(permissionToRequest) } // Provide action to re-request
            }
            else -> {
                requestGalleryPermissionLauncher.launch(permissionToRequest)
            }
        }
    }

    private fun openCameraInternal() {
        // Option 1: Capture thumbnail (simpler, what you had with cameraResultLauncher)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(intent)
        } else {
            Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show()
        }

        // Option 2: Capture full-size image using FileProvider (more robust)
        // Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
        //    takePictureIntent.resolveActivity(packageManager)?.also {
        //        val photoFile: File? = try {
        //            createImageFile() // A method to create a temporary file
        //        } catch (ex: IOException) {
        //            Log.e("CameraActivity", "Error creating image file", ex)
        //            null
        //        }
        //        photoFile?.also {
        //            val photoURI: Uri = FileProvider.getUriForFile(
        //                this,
        //                "${applicationContext.packageName}.fileprovider",
        //                it
        //            )
        //            currentPhotoUri = photoURI // Save for the result
        //            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        //            takePictureLauncher.launch(takePictureIntent)
        //        }
        //    } ?: run {
        //        Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show()
        //    }
        // }
    }

    // Example helper function if using FileProvider for full-size camera images
    // @Throws(IOException::class)
    // private fun createImageFile(): File {
    //    // Create an image file name
    //    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    //    val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES) // Or cacheDir
    //    return File.createTempFile(
    //        "JPEG_${timeStamp}_",
    //        ".jpg",
    //        storageDir
    //    ).apply {
    //        // Save a file: path for use with ACTION_VIEW intents
    //        // currentPhotoPath = absolutePath // If you need the absolute path
    //    }
    // }


    private fun openGalleryInternal() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        selectImageFromGalleryLauncher.launch(intent)
    }

    private fun updatePreviewWithUri(uri: Uri) {
        try {
            // It's safer to load the URI directly with Glide or decodeStream for Bitmaps
            // to avoid potential issues with large images or ContentResolver complexities.
            cameraPreview.setImageURI(uri) // ImageView can often handle content URIs directly
            cameraPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            previewText.visibility = View.GONE // Hide "No image selected"
            instructionText.text = "Image ready to scan"
            btnScan.isEnabled = true
        } catch (e: Exception) {
            Log.e("CameraActivity", "Error updating preview with URI: $uri", e)
            Toast.makeText(this, "Error displaying image: ${e.message}", Toast.LENGTH_SHORT).show()
            previewText.visibility = View.VISIBLE
            previewText.text = "Error loading image"
            instructionText.text = "Please try selecting another image."
            btnScan.isEnabled = false
        }
    }

    private fun navigateToScanResult(imageUri: Uri) {
        val intent = Intent(this, ScanResultActivity::class.java).apply {
            putExtra("imageUri", imageUri.toString())
            // Pass any other necessary data, like location if captured here
            // intent.putExtra("location", "Current Location if available") // Example
        }
        startActivity(intent)
        // finish() // Optional: Finish CameraActivity so back button from ScanResult goes further back
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
            Toast.makeText(this, "Error preparing image: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    "Permission Required",
                    "This permission is needed for the feature to work. Please enable it in settings."
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
}
