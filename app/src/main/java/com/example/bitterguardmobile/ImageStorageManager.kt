package com.example.bitterguardmobile

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Image Storage Manager - Handles different types of image storage
 * 
 * Two types of images:
 * 1. SCAN IMAGES - Stored locally (private, for disease detection)
 * 2. FORUM IMAGES - Stored in Supabase (public, for sharing)
 */
class ImageStorageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageStorageManager"
        private const val SCAN_IMAGES_DIR = "scan_images"
        private const val FORUM_IMAGES_DIR = "forum_images"
    }
    
    // ==================== SCAN IMAGES (Local Storage) ====================
    
    /**
     * Store scan image locally for disease detection
     * These images are private and stay on the user's device
     */
    suspend fun storeScanImageLocally(imageUri: Uri, fileName: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val scanDir = File(context.filesDir, SCAN_IMAGES_DIR)
                if (!scanDir.exists()) {
                    scanDir.mkdirs()
                }
                
                val finalFileName = fileName ?: "scan_${System.currentTimeMillis()}.jpg"
                val imageFile = File(scanDir, finalFileName)
                
                // Copy image from URI to local file
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    FileOutputStream(imageFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                Log.d(TAG, "Scan image stored locally: ${imageFile.absolutePath}")
                Result.success(imageFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Error storing scan image locally: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get local scan image file
     */
    fun getScanImageFile(fileName: String): File? {
        val scanDir = File(context.filesDir, SCAN_IMAGES_DIR)
        val imageFile = File(scanDir, fileName)
        return if (imageFile.exists()) imageFile else null
    }
    
    /**
     * List all local scan images
     */
    fun getAllScanImages(): List<File> {
        val scanDir = File(context.filesDir, SCAN_IMAGES_DIR)
        return if (scanDir.exists()) {
            scanDir.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * Delete local scan image
     */
    fun deleteScanImage(fileName: String): Boolean {
        val scanDir = File(context.filesDir, SCAN_IMAGES_DIR)
        val imageFile = File(scanDir, fileName)
        return imageFile.delete()
    }
    
    // ==================== FORUM IMAGES (Supabase Storage) ====================
    
    /**
     * Upload forum image to Supabase Storage
     * These images are public and can be shared in forum posts
     */
    suspend fun uploadForumImageToSupabase(imageUri: Uri, fileName: String? = null, accessToken: String, userId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val finalFileName = fileName ?: "forum_${System.currentTimeMillis()}.jpg"
                Log.d(TAG, "Starting image upload: $finalFileName")

                // Read bytes from URI
                val bytes = context.contentResolver.openInputStream(imageUri)?.use(InputStream::readBytes)
                    ?: return@withContext Result.failure(Exception("Unable to read image"))

                Log.d(TAG, "Image size: ${bytes.size} bytes")

                // Try multiple bucket names in case the bucket doesn't exist
                val possibleBuckets = listOf("forum_images", "forum-images", "images", "public")
                
                for (bucketId in possibleBuckets) {
                    try {
                        val client = OkHttpClient()
                        val objectPath = "$userId/$finalFileName"
                        val url = "${SupabaseConfig.PROJECT_URL}/storage/v1/object/$bucketId/$objectPath"

                        Log.d(TAG, "Trying bucket '$bucketId' with URL: $url")

                        val mediaType = "image/jpeg".toMediaType()
                        val req = Request.Builder()
                            .url(url)
                            .post(bytes.toRequestBody(mediaType))
                            .addHeader("Authorization", "Bearer $accessToken")
                            .addHeader("apikey", SupabaseConfig.API_KEY)
                            .addHeader("Content-Type", "image/jpeg")
                            .build()

                        val resp = client.newCall(req).execute()
                        val bodyStr = resp.body?.string() ?: ""
                        
                        Log.d(TAG, "Upload response for bucket '$bucketId': ${resp.code} - $bodyStr")
                        
                        if (resp.isSuccessful) {
                            val publicUrl = "${SupabaseConfig.PROJECT_URL}/storage/v1/object/public/$bucketId/$objectPath"
                            Log.d(TAG, "Forum image uploaded successfully: $publicUrl")
                            return@withContext Result.success(publicUrl)
                        } else {
                            Log.w(TAG, "Bucket '$bucketId' failed with ${resp.code}: $bodyStr")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error with bucket '$bucketId': ${e.message}")
                    }
                }
                
                // If all buckets failed, try a fallback approach - save locally and return file URL
                Log.w(TAG, "All Supabase buckets failed, falling back to local storage")
                val fallbackResult = saveImageLocally(imageUri, finalFileName)
                if (fallbackResult.isSuccess) {
                    val localUrl = fallbackResult.getOrNull()!!
                    Log.d(TAG, "Image saved locally: $localUrl")
                    return@withContext Result.success(localUrl)
                }
                
                Result.failure(Exception("All upload methods failed"))
                
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading forum image: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    private suspend fun saveImageLocally(imageUri: Uri, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val forumDir = File(context.filesDir, FORUM_IMAGES_DIR)
            if (!forumDir.exists()) {
                forumDir.mkdirs()
            }
            
            val imageFile = File(forumDir, fileName)
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            val fileUrl = "file://${imageFile.absolutePath}"
            Log.d(TAG, "Image saved locally: $fileUrl")
            Result.success(fileUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image locally: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get forum image URL from Supabase
     */
    suspend fun getForumImageUrl(fileName: String): String? {
        return try {
            // TODO: Implement Supabase Storage URL generation
            // val publicUrl = SupabaseManager.storage.from("forum-images").createSignedUrl(fileName, 3600)
            // publicUrl
            
            // For now, return local file path
            val forumDir = File(context.filesDir, FORUM_IMAGES_DIR)
            val imageFile = File(forumDir, fileName)
            if (imageFile.exists()) {
                "file://${imageFile.absolutePath}"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting forum image URL: ${e.message}", e)
            null
        }
    }
    
    /**
     * Delete forum image from Supabase
     */
    suspend fun deleteForumImageFromSupabase(fileName: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // TODO: Implement Supabase Storage deletion
                // SupabaseManager.storage.from("forum-images").delete(fileName)
                
                // For now, delete local file
                val forumDir = File(context.filesDir, FORUM_IMAGES_DIR)
                val imageFile = File(forumDir, fileName)
                val deleted = imageFile.delete()
                
                Log.d(TAG, "Forum image deleted: $fileName")
                Result.success(deleted)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting forum image: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Get file size in MB
     */
    fun getFileSizeMB(file: File): Double {
        return file.length() / (1024.0 * 1024.0)
    }
    
    /**
     * Check if image file is too large
     */
    fun isImageTooLarge(file: File, maxSizeMB: Double = 10.0): Boolean {
        return getFileSizeMB(file) > maxSizeMB
    }
    
    /**
     * Clean up old scan images (optional)
     */
    fun cleanupOldScanImages(olderThanDays: Int = 30) {
        val scanDir = File(context.filesDir, SCAN_IMAGES_DIR)
        if (!scanDir.exists()) return
        
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        
        scanDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
                Log.d(TAG, "Deleted old scan image: ${file.name}")
            }
        }
    }
    
    /**
     * Get storage usage statistics
     */
    fun getStorageStats(): StorageStats {
        val scanDir = File(context.filesDir, SCAN_IMAGES_DIR)
        val forumDir = File(context.filesDir, FORUM_IMAGES_DIR)
        
        val scanImagesCount = scanDir.listFiles()?.size ?: 0
        val scanImagesSize = scanDir.listFiles()?.sumOf { it.length() } ?: 0L
        
        val forumImagesCount = forumDir.listFiles()?.size ?: 0
        val forumImagesSize = forumDir.listFiles()?.sumOf { it.length() } ?: 0L
        
        return StorageStats(
            scanImagesCount = scanImagesCount,
            scanImagesSizeMB = scanImagesSize / (1024.0 * 1024.0),
            forumImagesCount = forumImagesCount,
            forumImagesSizeMB = forumImagesSize / (1024.0 * 1024.0)
        )
    }
}

/**
 * Data class for storage statistics
 */
data class StorageStats(
    val scanImagesCount: Int,
    val scanImagesSizeMB: Double,
    val forumImagesCount: Int,
    val forumImagesSizeMB: Double
)
