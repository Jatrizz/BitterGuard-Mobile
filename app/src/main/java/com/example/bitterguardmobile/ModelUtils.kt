package com.example.bitterguardmobile

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ModelUtils(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelUtils"
        private val MEAN_VALUES = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD_VALUES = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
    
    init {
        // Initialize AppConfig
        AppConfig.initialize(context)
    }
    
    private var leafDetectionModule: Module? = null
    private var diseaseDetectionModule: Module? = null
    
    /**
     * Load both PyTorch models from assets
     */
    fun loadModel(): Boolean {
        return try {
            Log.d(TAG, "Starting two-stage model loading process...")
            
            // Load leaf detection model
            val leafModelLoaded = loadLeafDetectionModel()
            if (!leafModelLoaded) {
                Log.e(TAG, "Failed to load leaf detection model")
                return false
            }
            
            // Load disease detection model
            val diseaseModelLoaded = loadDiseaseDetectionModel()
            if (!diseaseModelLoaded) {
                Log.e(TAG, "Failed to load disease detection model")
                return false
            }
            
            Log.d(TAG, "Both models loaded successfully!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading models: ${e.message}", e)
            false
        }
    }
    
    /**
     * Load the leaf detection model
     */
    private fun loadLeafDetectionModel(): Boolean {
        return try {
            val modelFilename = AppConfig.getLeafDetectionModel()
            Log.d(TAG, "Loading leaf detection model: $modelFilename")
            
            // Check if model file exists in assets
            val assetFile = context.assets.list("")?.find { it == modelFilename }
            if (assetFile == null) {
                Log.e(TAG, "Leaf detection model file $modelFilename not found in assets")
                return false
            }
            
            val modelFile = copyAssetToFilesDir(modelFilename)
            val fileSize = modelFile.length()
            Log.d(TAG, "Leaf detection model file size: ${fileSize / (1024 * 1024)} MB")
            
            leafDetectionModule = Module.load(modelFile.absolutePath)
            Log.d(TAG, "Leaf detection model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading leaf detection model: ${e.message}", e)
            false
        }
    }
    
    /**
     * Load the disease detection model
     */
    private fun loadDiseaseDetectionModel(): Boolean {
        return try {
            val modelFilename = AppConfig.getDiseaseDetectionModel()
            Log.d(TAG, "Loading disease detection model: $modelFilename")
            
            // Check if model file exists in assets
            val assetFile = context.assets.list("")?.find { it == modelFilename }
            if (assetFile == null) {
                Log.e(TAG, "Disease detection model file $modelFilename not found in assets")
                return false
            }
            
            val modelFile = copyAssetToFilesDir(modelFilename)
            val fileSize = modelFile.length()
            Log.d(TAG, "Disease detection model file size: ${fileSize / (1024 * 1024)} MB")
            
            diseaseDetectionModule = Module.load(modelFile.absolutePath)
            Log.d(TAG, "Disease detection model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading disease detection model: ${e.message}", e)
            false
        }
    }
    
    /**
     * Copy model file from assets to internal storage
     */
    private fun copyAssetToFilesDir(assetName: String): File {
        val file = File(context.filesDir, assetName)
        
        if (!file.exists()) {
            try {
                Log.d(TAG, "Copying model from assets to internal storage...")
                context.assets.open(assetName).use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d(TAG, "Model copied successfully to: ${file.absolutePath}")
            } catch (e: IOException) {
                Log.e(TAG, "Error copying model file: ${e.message}", e)
                throw e
            }
        } else {
            Log.d(TAG, "Model file already exists: ${file.absolutePath}")
        }
        
        return file
    }
    
    /**
     * Preprocess image for model input
     */
    private fun preprocessImage(bitmap: Bitmap): Tensor {
        val inputSize = AppConfig.getInputSize()
        Log.d(TAG, "Preprocessing image: ${bitmap.width}x${bitmap.height} -> ${inputSize}x${inputSize}")
        
        // Resize bitmap to model input size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        
        // Convert to tensor with normalization
        val tensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            MEAN_VALUES,
            STD_VALUES
        )
        
        Log.d(TAG, "Preprocessed tensor shape: ${tensor.shape().contentToString()}")
        return tensor
    }
    
    /**
     * Run two-stage inference: First detect bitter gourd leaf, then detect disease
     */
    fun runInference(bitmap: Bitmap): FloatArray? {
        return try {
            Log.d(TAG, "=== STARTING TWO-STAGE INFERENCE ===")
            Log.d(TAG, "Bitmap size: ${bitmap.width}x${bitmap.height}")
            Log.d(TAG, "Leaf detection model loaded: ${leafDetectionModule != null}")
            Log.d(TAG, "Disease detection model loaded: ${diseaseDetectionModule != null}")
            
            // Stage 1: Check if bitter gourd leaf is detected
            Log.d(TAG, "STAGE 1: Running leaf detection...")
            val leafDetectionResult = runLeafDetection(bitmap)
            if (leafDetectionResult == null) {
                Log.e(TAG, "Leaf detection failed")
                return null
            }
            
            val leafClasses = AppConfig.getLeafClasses()
            val leafPredictionClass = getPredictionClass(leafDetectionResult)
            val leafClassName = leafClasses[leafPredictionClass]
            val leafConfidence = getPredictionConfidence(leafDetectionResult)
            
            Log.d(TAG, "Leaf detection result: $leafClassName (confidence: ${leafConfidence * 100}%)")
            Log.d(TAG, "Leaf detection raw output: ${leafDetectionResult.joinToString(", ")}")
            
            // Add confidence threshold for more reliable detection
            val confidenceThreshold = 0.7f // 70% confidence threshold (higher for more accuracy)
            
            // If no bitter gourd leaf detected OR confidence is too low, return special result
            if (leafPredictionClass != 0 || leafConfidence < confidenceThreshold) { 
                Log.d(TAG, "No bitter gourd leaf detected (class: $leafPredictionClass, confidence: ${leafConfidence * 100}%)")
                Log.d(TAG, "Stopping detection - not proceeding to disease detection")
                // Return a special array indicating "no bitter gourd leaf"
                return floatArrayOf(-1.0f, -1.0f, -1.0f, -1.0f) // Special marker
            }
            
            Log.d(TAG, "Bitter gourd leaf confirmed with high confidence (${leafConfidence * 100}%) - proceeding to disease detection")
            
            // Stage 2: Run disease detection
            Log.d(TAG, "STAGE 2: Bitter gourd leaf confirmed, running disease detection...")
            val diseaseDetectionResult = runDiseaseDetection(bitmap)
            if (diseaseDetectionResult == null) {
                Log.e(TAG, "Disease detection failed")
                return null
            }
            
            val diseaseClasses = AppConfig.getDiseaseClasses()
            val diseasePredictionClass = getPredictionClass(diseaseDetectionResult)
            val diseaseClassName = diseaseClasses[diseasePredictionClass]
            val diseaseConfidence = getPredictionConfidence(diseaseDetectionResult)
            
            Log.d(TAG, "Disease detection result: $diseaseClassName (confidence: ${diseaseConfidence * 100}%)")
            
            diseaseDetectionResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during two-stage inference: ${e.message}", e)
            null
        }
    }
    
    /**
     * Run leaf detection inference
     */
    private fun runLeafDetection(bitmap: Bitmap): FloatArray? {
        return try {
            leafDetectionModule?.let { model ->
                Log.d(TAG, "Running leaf detection inference...")
                
                val inputTensor = preprocessImage(bitmap)
                val inputIValue = IValue.from(inputTensor)
                
                val outputIValue = model.forward(inputIValue)
                val outputTensor = outputIValue.toTensor()
                val outputArray = outputTensor.getDataAsFloatArray()
                
                Log.d(TAG, "Leaf detection output: ${outputArray.joinToString(", ")}")
                outputArray
            } ?: run {
                Log.e(TAG, "Leaf detection model not loaded")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during leaf detection: ${e.message}", e)
            null
        }
    }
    
    /**
     * Run disease detection inference
     */
    private fun runDiseaseDetection(bitmap: Bitmap): FloatArray? {
        return try {
            diseaseDetectionModule?.let { model ->
                Log.d(TAG, "Running disease detection inference...")
                
                val inputTensor = preprocessImage(bitmap)
                val inputIValue = IValue.from(inputTensor)
                
                val outputIValue = model.forward(inputIValue)
                val outputTensor = outputIValue.toTensor()
                val outputArray = outputTensor.getDataAsFloatArray()
                
                Log.d(TAG, "Disease detection output: ${outputArray.joinToString(", ")}")
                outputArray
            } ?: run {
                Log.e(TAG, "Disease detection model not loaded")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during disease detection: ${e.message}", e)
            null
        }
    }
    
    /**
     * Mock inference for testing when real model fails
     */
    fun runMockInference(bitmap: Bitmap): FloatArray {
        Log.d(TAG, "Running mock inference for testing")
        // Create a mock output with 4 classes (0=Downey Mildew, 1=Fresh Leaf, 2=Fusarium Wilt, 3=Mosaic Virus)
        val mockOutput = FloatArray(4)
        
        // Generate different results based on image characteristics for more realistic simulation
        val imageHash = bitmap.hashCode()
        val random = java.util.Random(imageHash.toLong())
        
        // Simulate more realistic disease detection patterns
        val imageBrightness = calculateImageBrightness(bitmap)
        val imageContrast = calculateImageContrast(bitmap)
        
        // Adjust probabilities based on image characteristics
        val baseProbabilities = when {
            imageBrightness < 0.3f -> floatArrayOf(0.1f, 0.6f, 0.2f, 0.1f) // Dark images more likely to be healthy
            imageBrightness > 0.7f -> floatArrayOf(0.3f, 0.2f, 0.3f, 0.2f) // Bright images more likely to have diseases
            imageContrast < 0.2f -> floatArrayOf(0.2f, 0.4f, 0.2f, 0.2f) // Low contrast more likely healthy
            else -> floatArrayOf(0.25f, 0.25f, 0.25f, 0.25f) // Equal probability
        }
        
        // Add some randomness but keep the base pattern
        for (i in 0 until 4) {
            val baseProb = baseProbabilities[i]
            val randomFactor = (random.nextFloat() - 0.5f) * 0.3f // ±15% variation
            mockOutput[i] = (baseProb + randomFactor).coerceIn(0.01f, 0.99f)
        }
        
        // Normalize to make it look like proper softmax output
        val sum = mockOutput.sum()
        for (i in mockOutput.indices) {
            mockOutput[i] = mockOutput[i] / sum
        }
        
        Log.d(TAG, "Mock inference result: ${mockOutput.joinToString(", ")}")
        return mockOutput
    }
    
    /**
     * Calculate average brightness of the image (0.0 = black, 1.0 = white)
     */
    private fun calculateImageBrightness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        var totalBrightness = 0f
        var pixelCount = 0
        
        // Sample every 10th pixel for performance
        for (x in 0 until width step 10) {
            for (y in 0 until height step 10) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel) / 255f
                val g = android.graphics.Color.green(pixel) / 255f
                val b = android.graphics.Color.blue(pixel) / 255f
                totalBrightness += (r + g + b) / 3f
                pixelCount++
            }
        }
        
        return if (pixelCount > 0) totalBrightness / pixelCount else 0.5f
    }
    
    /**
     * Calculate contrast of the image (higher = more contrast)
     */
    private fun calculateImageContrast(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val brightnesses = mutableListOf<Float>()
        
        // Sample every 20th pixel for performance
        for (x in 0 until width step 20) {
            for (y in 0 until height step 20) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel) / 255f
                val g = android.graphics.Color.green(pixel) / 255f
                val b = android.graphics.Color.blue(pixel) / 255f
                brightnesses.add((r + g + b) / 3f)
            }
        }
        
        if (brightnesses.isEmpty()) return 0.5f
        
        val mean = brightnesses.average().toFloat()
        val variance = brightnesses.map { (it - mean) * (it - mean) }.average().toFloat()
        return kotlin.math.sqrt(variance)
    }
    
    /**
     * Get model prediction class (assuming classification model)
     */
    fun getPredictionClass(outputArray: FloatArray): Int {
        var maxIndex = -1
        var maxValue = Float.NEGATIVE_INFINITY
        
        for (i in 0 until outputArray.size) {
            if (outputArray[i] > maxValue) {
                maxValue = outputArray[i]
                maxIndex = i
            }
        }
        
        Log.d(TAG, "Prediction class: $maxIndex with confidence: $maxValue")
        return maxIndex
    }
    
    /**
     * Get prediction confidence
     */
    fun getPredictionConfidence(outputArray: FloatArray): Float {
        val maxIndex = getPredictionClass(outputArray)
        return if (maxIndex >= 0) outputArray[maxIndex] else 0f
    }
    
    /**
     * Check if both models are loaded
     */
    fun isModelLoaded(): Boolean {
        val leafLoaded = leafDetectionModule != null
        val diseaseLoaded = diseaseDetectionModule != null
        val bothLoaded = leafLoaded && diseaseLoaded
        Log.d(TAG, "Models loaded status - Leaf: $leafLoaded, Disease: $diseaseLoaded, Both: $bothLoaded")
        return bothLoaded
    }
    
    /**
     * Get disease name from prediction class
     */
    fun getDiseaseName(predictionClass: Int): String {
        val diseaseClasses = AppConfig.getDiseaseClasses()
        return if (predictionClass >= 0 && predictionClass < diseaseClasses.size) {
            diseaseClasses[predictionClass]
        } else {
            "Unknown"
        }
    }
    
    /**
     * Get leaf detection result message
     */
    fun getLeafDetectionMessage(outputArray: FloatArray): String {
        // Check if this is the special "no bitter gourd leaf" marker
        if (outputArray.size == 4 && outputArray.all { it == -1.0f }) {
            return "Not a Bitter Gourd Leaf"
        }
        
        val leafClasses = arrayOf("Bitter Gourd", "Not")
        val predictionClass = getPredictionClass(outputArray)
        val confidence = getPredictionConfidence(outputArray)
        
        return if (predictionClass == 0) { // Bitter Gourd detected
            "Bitter gourd leaf detected (${(confidence * 100).toInt()}%)"
        } else {
            "Not a Bitter Gourd Leaf"
        }
    }
    
    /**
     * Get disease detection result message
     */
    fun getDiseaseDetectionMessage(outputArray: FloatArray): String {
        val diseaseClasses = AppConfig.getDiseaseClasses()
        val predictionClass = getPredictionClass(outputArray)
        val confidence = getPredictionConfidence(outputArray)
        
        return if (predictionClass == 1) { // Fresh Leaf (no disease)
            "No disease detected - Fresh Leaf (${(confidence * 100).toInt()}%)"
        } else {
            val diseaseName = getDiseaseName(predictionClass)
            "$diseaseName detected (${(confidence * 100).toInt()}%)"
        }
    }
    
    /**
     * Release model resources
     */
    fun releaseModel() {
        leafDetectionModule = null
        diseaseDetectionModule = null
        Log.d(TAG, "Both models released")
    }

    fun close() {
        releaseModel()
    }

    /**
     * Diagnostic method to check model and PyTorch setup
     */
    fun runDiagnostics(): String {
        val diagnostics = StringBuilder()
        
        try {
            // Check leaf detection model
            val leafModelFilename = AppConfig.getLeafDetectionModel()
            val assetFiles = context.assets.list("")
            val leafModelExists = assetFiles?.contains(leafModelFilename) ?: false
            diagnostics.append("Leaf detection model in assets: ${if (leafModelExists) "✓" else "✗"}\n")
            
            // Check disease detection model
            val diseaseModelFilename = AppConfig.getDiseaseDetectionModel()
            val diseaseModelExists = assetFiles?.contains(diseaseModelFilename) ?: false
            diagnostics.append("Disease detection model in assets: ${if (diseaseModelExists) "✓" else "✗"}\n")
            
            // Check if both models are loaded
            val leafLoaded = leafDetectionModule != null
            val diseaseLoaded = diseaseDetectionModule != null
            diagnostics.append("Leaf detection model loaded: ${if (leafLoaded) "✓" else "✗"}\n")
            diagnostics.append("Disease detection model loaded: ${if (diseaseLoaded) "✓" else "✗"}\n")
            
            // Test PyTorch availability
            try {
                val testTensor = Tensor.fromBlob(floatArrayOf(1.0f), longArrayOf(1))
                diagnostics.append("PyTorch available: ✓\n")
            } catch (e: Exception) {
                diagnostics.append("PyTorch available: ✗ (${e.message})\n")
            }
            
        } catch (e: Exception) {
            diagnostics.append("Diagnostic error: ${e.message}\n")
        }
        
        return diagnostics.toString()
    }
    
    /**
     * Test leaf detection with a specific image
     */
    fun testLeafDetection(bitmap: Bitmap): String {
        return try {
            val result = runLeafDetection(bitmap)
            if (result != null) {
                val leafClasses = AppConfig.getLeafClasses()
                val predictionClass = getPredictionClass(result)
                val confidence = getPredictionConfidence(result)
                val className = leafClasses[predictionClass]
                
                "Leaf Detection Test:\n" +
                "Raw output: ${result.joinToString(", ")}\n" +
                "Predicted class: $predictionClass ($className)\n" +
                "Confidence: ${(confidence * 100).toInt()}%\n" +
                "Will proceed to disease detection: ${predictionClass == 0 && confidence >= 0.7f}"
            } else {
                "Leaf detection failed"
            }
        } catch (e: Exception) {
            "Leaf detection error: ${e.message}"
        }
    }
    
    /**
     * Verify both models are loaded and working
     */
    fun verifyTwoStageSystem(): String {
        val diagnostics = StringBuilder()
        
        diagnostics.append("=== TWO-STAGE SYSTEM VERIFICATION ===\n")
        
        // Check model loading status
        val leafLoaded = leafDetectionModule != null
        val diseaseLoaded = diseaseDetectionModule != null
        
        diagnostics.append("Leaf Detection Model: ${if (leafLoaded) "✓ LOADED" else "✗ NOT LOADED"}\n")
        diagnostics.append("Disease Detection Model: ${if (diseaseLoaded) "✓ LOADED" else "✗ NOT LOADED"}\n")
        
        // Check configuration
        val leafModelFile = AppConfig.getLeafDetectionModel()
        val diseaseModelFile = AppConfig.getDiseaseDetectionModel()
        
        diagnostics.append("Leaf Model File: $leafModelFile\n")
        diagnostics.append("Disease Model File: $diseaseModelFile\n")
        
        // Check if both models exist in assets
        try {
            val assetFiles = context.assets.list("")
            val leafExists = assetFiles?.contains(leafModelFile) ?: false
            val diseaseExists = assetFiles?.contains(diseaseModelFile) ?: false
            
            diagnostics.append("Leaf Model in Assets: ${if (leafExists) "✓" else "✗"}\n")
            diagnostics.append("Disease Model in Assets: ${if (diseaseExists) "✓" else "✗"}\n")
        } catch (e: Exception) {
            diagnostics.append("Error checking assets: ${e.message}\n")
        }
        
        diagnostics.append("System Ready: ${if (leafLoaded && diseaseLoaded) "✓ YES" else "✗ NO"}\n")
        
        return diagnostics.toString()
    }
} 