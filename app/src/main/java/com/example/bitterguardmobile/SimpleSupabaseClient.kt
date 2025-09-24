package com.example.bitterguardmobile

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException

/**
 * Simple HTTP client for Supabase API calls
 * This is a lightweight alternative to the full Supabase Kotlin SDK
 */
class SimpleSupabaseClient {
    
    companion object {
        private const val TAG = "SimpleSupabaseClient"
        private val BASE_URL = SupabaseConfig.REST_URL
        private val AUTH_URL = SupabaseConfig.AUTH_URL
        private val API_KEY = SupabaseConfig.API_KEY
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val hasAuth = original.header("Authorization") != null
            val builder = original.newBuilder()
                .header("apikey", API_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", original.header("Prefer") ?: "return=minimal")
            if (!hasAuth) {
                builder.header("Authorization", "Bearer $API_KEY")
            }
            chain.proceed(builder.build())
        }
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * GET request to Supabase
     */
    suspend fun get(table: String, filters: Map<String, String> = emptyMap()): Result<String> {
        return try {
            val urlBuilder = "$BASE_URL$table".toHttpUrl().newBuilder()
            
            filters.forEach { (key, value) ->
                urlBuilder.addQueryParameter(key, value)
            }
            
            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "GET $table successful: $responseBody")
                Result.success(responseBody)
            } else {
                Log.e(TAG, "GET $table failed: ${response.code} - $responseBody")
                Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "GET $table error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * POST request to Supabase
     */
    suspend fun post(table: String, data: JsonObject): Result<String> {
        return try {
            val jsonString = json.encodeToString(JsonObject.serializer(), data)
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL$table")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "POST $table successful: $responseBody")
                Result.success(responseBody)
            } else {
                Log.e(TAG, "POST $table failed: ${response.code} - $responseBody")
                Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "POST $table error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * POST with user auth token, optional upsert via on_conflict
     */
    suspend fun postWithUserAuth(
        table: String,
        data: JsonObject,
        authToken: String,
        onConflict: String? = null,
        prefer: String = "return=minimal"
    ): Result<String> {
        return try {
            val url = if (onConflict != null) "$BASE_URL$table?on_conflict=$onConflict" else "$BASE_URL$table"
            val jsonString = json.encodeToString(JsonObject.serializer(), data)
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Authorization", "Bearer $authToken")
                .header("Prefer", prefer)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Log.d(TAG, "POST (user) $table successful: $responseBody")
                Result.success(responseBody)
            } else {
                Log.e(TAG, "POST (user) $table failed: ${response.code} - $responseBody")
                Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "POST (user) $table error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * POST request to Supabase Auth endpoint
     */
    suspend fun postAuth(endpoint: String, data: JsonObject): Result<String> {
        return try {
            val jsonString = json.encodeToString(JsonObject.serializer(), data)
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$AUTH_URL$endpoint")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "POST Auth $endpoint successful: $responseBody")
                Result.success(responseBody)
            } else {
                Log.e(TAG, "POST Auth $endpoint failed: ${response.code} - $responseBody")
                Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "POST Auth $endpoint error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * PUT request to Supabase
     */
    suspend fun put(table: String, id: String, data: JsonObject): Result<String> {
        return try {
            val jsonString = json.encodeToString(JsonObject.serializer(), data)
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL$table?id=eq.$id")
                .put(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "PUT $table successful: $responseBody")
                Result.success(responseBody)
            } else {
                Log.e(TAG, "PUT $table failed: ${response.code} - $responseBody")
                Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "PUT $table error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * DELETE request to Supabase
     */
    suspend fun delete(table: String, id: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL$table?id=eq.$id")
                .delete()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "DELETE $table successful: $responseBody")
                Result.success(responseBody)
            } else {
                Log.e(TAG, "DELETE $table failed: ${response.code} - $responseBody")
                Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "DELETE $table error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
