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
    
    private var module: Module? = null
    
    /**
     * Load the PyTorch model from assets
     */
    fun loadModel(): Boolean {
        return try {
            val modelFilename = AppConfig.getModelFilename()
            Log.d(TAG, "Starting model loading process...")
            
            // Check if model file exists in assets
            val assetFile = context.assets.list("")?.find { it == modelFilename }
            if (assetFile == null) {
                Log.e(TAG, "Model file $modelFilename not found in assets")
                return false
            }
            
            Log.d(TAG, "Model file found in assets: $modelFilename")
            
            val modelFile = copyAssetToFilesDir(modelFilename)
            Log.d(TAG, "Model copied to: ${modelFile.absolutePath}")
            
            // Check file size
            val fileSize = modelFile.length()
            Log.d(TAG, "Model file size: ${fileSize / (1024 * 1024)} MB")
            
            module = Module.load(modelFile.absolutePath)
            Log.d(TAG, "Model loaded successfully into PyTorch Module")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}", e)
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
     * Run inference on the model
     */
    fun runInference(bitmap: Bitmap): FloatArray? {
        return try {
            module?.let { model ->
                Log.d(TAG, "Starting inference with bitmap size: ${bitmap.width}x${bitmap.height}")
                
                val inputTensor = preprocessImage(bitmap)
                Log.d(TAG, "Input tensor created with shape: ${inputTensor.shape().contentToString()}")
                
                val inputIValue = IValue.from(inputTensor)
                
                // Run inference
                Log.d(TAG, "Running model forward pass...")
                val outputIValue = model.forward(inputIValue)
                val outputTensor = outputIValue.toTensor()
                
                Log.d(TAG, "Output tensor shape: ${outputTensor.shape().contentToString()}")
                
                // Convert output tensor to float array
                val outputArray = outputTensor.getDataAsFloatArray()
                
                Log.d(TAG, "Output array size: ${outputArray.size}")
                Log.d(TAG, "Output array values: ${outputArray.take(10).joinToString(", ")}...")
                
                // Validate output
                if (outputArray.isEmpty()) {
                    Log.e(TAG, "Output array is empty")
                    return null
                }
                
                // Check if all values are the same (indicates potential issue)
                val firstValue = outputArray[0]
                val allSame = outputArray.all { it == firstValue }
                if (allSame) {
                    Log.w(TAG, "All output values are the same: $firstValue")
                }
                
                Log.d(TAG, "Inference completed successfully")
                outputArray
            } ?: run {
                Log.e(TAG, "Model not loaded")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during inference: ${e.message}", e)
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
     * Check if model is loaded
     */
    fun isModelLoaded(): Boolean {
        val loaded = module != null
        Log.d(TAG, "Model loaded status: $loaded")
        return loaded
    }
    
    /**
     * Release model resources
     */
    fun releaseModel() {
        module = null
        Log.d(TAG, "Model released")
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
            // Check if model file exists in assets
            val modelFilename = AppConfig.getModelFilename()
            val assetFiles = context.assets.list("")
            val modelFileExists = assetFiles?.contains(modelFilename) ?: false
            diagnostics.append("Model file in assets: ${if (modelFileExists) "✓" else "✗"}\n")
            
            if (modelFileExists) {
                // Check file size
                context.assets.open(modelFilename).use { input ->
                    val size = input.available()
                    diagnostics.append("Model file size: ${size / (1024 * 1024)} MB\n")
                }
            }
            
            // Check if model is loaded
            diagnostics.append("Model loaded: ${if (isModelLoaded()) "✓" else "✗"}\n")
            
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
} 