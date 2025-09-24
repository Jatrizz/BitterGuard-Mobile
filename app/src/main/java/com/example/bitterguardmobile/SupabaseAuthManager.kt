package com.example.bitterguardmobile

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Supabase Authentication Manager - Handles user authentication using Supabase Auth API
 */
class SupabaseAuthManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SupabaseAuthManager"
        private val AUTH_URL = SupabaseConfig.AUTH_URL
        private val API_KEY = SupabaseConfig.API_KEY
        
        // SharedPreferences keys
        private const val PREFS_NAME = "supabase_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
    }
    
    private val httpClient = SimpleSupabaseClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    data class AuthResult(
        val success: Boolean,
        val user: User? = null,
        val error: String? = null
    )
    
    data class User(
        val id: String,
        val email: String,
        val name: String? = null,
        val phone: String? = null
    )
    
    /**
     * Register a new user with email and password
     */
    suspend fun register(email: String, password: String, name: String? = null, phone: String? = null, realEmail: String? = null): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val requestData = buildJsonObject {
                    put("email", email)
                    put("password", password)
                    // Persist user metadata (full name, phone, real_email) on signup
                    put("data", buildJsonObject {
                        if (name != null) put("name", name)
                        if (phone != null) put("phone", phone)
                        if (realEmail != null && realEmail.isNotBlank()) put("real_email", realEmail)
                    })
                }
                
                val result = httpClient.postAuth("signup", requestData)
                
                if (result.isSuccess) {
                    val responseJson = json.parseToJsonElement(result.getOrNull() ?: "")
                    val responseObj = responseJson.jsonObject
                    
                    val accessToken = responseObj["access_token"]?.jsonPrimitive?.content
                    val refreshToken = responseObj["refresh_token"]?.jsonPrimitive?.content
                    val userObj = responseObj["user"]?.jsonObject
                    
                    if (accessToken != null && refreshToken != null && userObj != null) {
                        val user = User(
                            id = userObj["id"]?.jsonPrimitive?.content ?: "",
                            email = userObj["email"]?.jsonPrimitive?.content ?: email,
                            name = name ?: userObj["user_metadata"]?.jsonObject?.get("name")?.jsonPrimitive?.content,
                            phone = phone ?: userObj["user_metadata"]?.jsonObject?.get("phone")?.jsonPrimitive?.content
                        )
                        
                        // Save tokens and user info
                        saveAuthData(accessToken, refreshToken, user)
                        
                        Log.d(TAG, "User registered successfully: ${user.email}")
                        AuthResult(success = true, user = user)
                    } else {
                        AuthResult(success = false, error = "Invalid response format")
                    }
                } else {
                    AuthResult(success = false, error = result.exceptionOrNull()?.message ?: "Registration failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}", e)
                AuthResult(success = false, error = e.message)
            }
        }
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val requestData = buildJsonObject {
                    put("email", email)
                    put("password", password)
                }
                
                val result = httpClient.postAuth("token?grant_type=password", requestData)
                
                if (result.isSuccess) {
                    val responseJson = json.parseToJsonElement(result.getOrNull() ?: "")
                    val responseObj = responseJson.jsonObject
                    
                    val accessToken = responseObj["access_token"]?.jsonPrimitive?.content
                    val refreshToken = responseObj["refresh_token"]?.jsonPrimitive?.content
                    val userObj = responseObj["user"]?.jsonObject
                    
                    if (accessToken != null && refreshToken != null && userObj != null) {
                        val user = User(
                            id = userObj["id"]?.jsonPrimitive?.content ?: "",
                            email = userObj["email"]?.jsonPrimitive?.content ?: email,
                            name = userObj["user_metadata"]?.jsonObject?.get("name")?.jsonPrimitive?.content
                        )
                        
                        // Save tokens and user info
                        saveAuthData(accessToken, refreshToken, user)
                        
                        Log.d(TAG, "User signed in successfully: ${user.email}")
                        AuthResult(success = true, user = user)
                    } else {
                        AuthResult(success = false, error = "Invalid response format")
                    }
                } else {
                    AuthResult(success = false, error = result.exceptionOrNull()?.message ?: "Sign in failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign in error: ${e.message}", e)
                AuthResult(success = false, error = e.message)
            }
        }
    }
    
    /**
     * Sign out the current user
     */
    suspend fun signOut(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
                if (refreshToken != null) {
                    val requestData = buildJsonObject {
                        put("refresh_token", refreshToken)
                    }
                    
                    // Call signout endpoint
                    httpClient.postAuth("logout", requestData)
                }
                
                // Clear local data regardless of API call result
                clearAuthData()
                Log.d(TAG, "User signed out successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Sign out error: ${e.message}", e)
                // Clear local data even if API call fails
                clearAuthData()
                true
            }
        }
    }
    
    /**
     * Get current user if signed in
     */
    fun getCurrentUser(): User? {
        val userId = prefs.getString(KEY_USER_ID, null)
        val userEmail = prefs.getString(KEY_USER_EMAIL, null)
        val userName = prefs.getString(KEY_USER_NAME, null)
        
        return if (userId != null && userEmail != null) {
            User(
                id = userId,
                email = userEmail,
                name = userName
            )
        } else {
            null
        }
    }
    
    /**
     * Check if user is currently signed in
     */
    fun isSignedIn(): Boolean {
        return getCurrentUser() != null && prefs.getString(KEY_ACCESS_TOKEN, null) != null
    }
    
    /**
     * Get access token for API calls
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Save authentication data to SharedPreferences
     */
    private fun saveAuthData(accessToken: String, refreshToken: String, user: User) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_NAME, user.name)
            apply()
        }
    }
    
    /**
     * Clear authentication data from SharedPreferences
     */
    private fun clearAuthData() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Refresh access token using refresh token
     */
    suspend fun refreshToken(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
                if (refreshToken == null) return@withContext false
                
                val requestData = buildJsonObject {
                    put("refresh_token", refreshToken)
                }
                
                val result = httpClient.postAuth("token?grant_type=refresh_token", requestData)
                
                if (result.isSuccess) {
                    val responseJson = json.parseToJsonElement(result.getOrNull() ?: "")
                    val responseObj = responseJson.jsonObject
                    
                    val newAccessToken = responseObj["access_token"]?.jsonPrimitive?.content
                    
                    if (newAccessToken != null) {
                        prefs.edit().putString(KEY_ACCESS_TOKEN, newAccessToken).apply()
                        Log.d(TAG, "Token refreshed successfully")
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh error: ${e.message}", e)
                false
            }
        }
    }
}
