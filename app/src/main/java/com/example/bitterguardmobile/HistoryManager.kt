package com.example.bitterguardmobile

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "HistoryManager"
        private const val PREF_NAME = "scan_history"
        private const val KEY_HISTORY = "history_list"
    }
    
    init {
        // Initialize AppConfig
        AppConfig.initialize(context)
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val authManager = SupabaseAuthManager(context)
    private val gson = Gson()
    
    /**
     * Build a per-user key for storing history so each user only sees their own scans
     */
    private fun getUserScopedHistoryKey(): String {
        val userId = authManager.getCurrentUser()?.id
        // Use a stable fallback for guests or when no user is signed in
        val suffix = if (!userId.isNullOrBlank()) userId else "guest"
        return "${KEY_HISTORY}_$suffix"
    }
    
    /**
     * Save a scan result to history
     */
    fun saveScanResult(scanResult: ScanResult) {
        try {
            val currentHistory = getScanHistory().toMutableList()
            
            // Add new result at the beginning (most recent first)
            currentHistory.add(0, scanResult)
            
            // Keep only the last MAX_HISTORY_SIZE results
            val maxHistorySize = AppConfig.getMaxHistorySize()
            if (currentHistory.size > maxHistorySize) {
                currentHistory.removeAt(currentHistory.size - 1)
            }
            
            // Save to SharedPreferences
            val historyJson = gson.toJson(currentHistory)
            sharedPreferences.edit().putString(getUserScopedHistoryKey(), historyJson).apply()
            
            Log.d(TAG, "Scan result saved: ${scanResult.prediction} (${scanResult.confidence})")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving scan result: ${e.message}")
        }
    }
    
    /**
     * Get all scan history
     */
    fun getScanHistory(): List<ScanResult> {
        return try {
            val historyJson = sharedPreferences.getString(getUserScopedHistoryKey(), "[]")
            val type = object : TypeToken<List<ScanResult>>() {}.type
            gson.fromJson(historyJson, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading scan history: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Search scan history by prediction
     */
    fun searchHistory(query: String): List<ScanResult> {
        val allHistory = getScanHistory()
        if (query.isBlank()) return allHistory
        
        return allHistory.filter { scanResult ->
            scanResult.prediction.contains(query, ignoreCase = true) ||
            scanResult.confidence.contains(query, ignoreCase = true) ||
            scanResult.location.contains(query, ignoreCase = true)
        }
    }
    
    /**
     * Clear all history
     */
    fun clearHistory() {
        sharedPreferences.edit().remove(getUserScopedHistoryKey()).apply()
        Log.d(TAG, "Scan history cleared")
    }
    
    /**
     * Get history count
     */
    fun getHistoryCount(): Int {
        return getScanHistory().size
    }
} 