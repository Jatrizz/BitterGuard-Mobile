package com.example.bitterguardmobile

/**
 * Supabase Configuration
 * Update these values with your actual Supabase project details
 */
object SupabaseConfig {
    
    // Supabase project configuration
    const val PROJECT_URL = "https://bihiognjjtaztmepehtv.supabase.co"
    
    // Supabase anon/public API key
    const val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJpaGlvZ25qanRhenRtZXBlaHR2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTgyOTU4MzcsImV4cCI6MjA3Mzg3MTgzN30.-5C_l2jMgyFfItcmCX5uXGbJi3QSI8irJ7UGQ7JHLkE"
    
    // These are derived from the above, don't change them
    const val AUTH_URL = "$PROJECT_URL/auth/v1/"
    const val REST_URL = "$PROJECT_URL/rest/v1/"
    
    // Check if configuration is valid
    fun isConfigured(): Boolean {
        return PROJECT_URL != "https://your-project-id.supabase.co" && 
               API_KEY != "your-api-key-here"
    }
}