package com.example.bitterguardmobile

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Dynamic Translation Manager
 * Allows runtime language switching and translation management
 */
object TranslationManager {
    
    private const val PREF_NAME = "translation_config"
    private const val KEY_CURRENT_LANGUAGE = "current_language"
    private const val KEY_CUSTOM_TRANSLATIONS = "custom_translations"
    
    private lateinit var sharedPreferences: SharedPreferences
    private var translations: MutableMap<String, Map<String, String>> = mutableMapOf()
    
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadTranslations()
    }
    
    private fun loadTranslations() {
        // Load default translations
        translations["en"] = getDefaultEnglishTranslations()
        translations["tl"] = getDefaultFilipinoTranslations()
        
        // Load custom translations if any
        loadCustomTranslations()
    }
    
    private fun getDefaultEnglishTranslations(): Map<String, String> {
        return mapOf(
            "app_name" to "BitterGuard Mobile",
            "welcome_message" to "Welcome to BitterGuard",
            "scan_leaf" to "Scan Leaf",
            "view_history" to "View History",
            "disease_info" to "Disease Information",
            "continue_as_guest" to "Continue as Guest",
            "login_with_phone" to "Login with Phone",
            "login_with_google" to "Login with Google",
            "home" to "Home",
            "scan" to "Scan",
            "history" to "History",
            "profile" to "Profile",
            "settings" to "Settings",
            "about" to "About",
            "logout" to "Logout",
            "ok" to "OK",
            "cancel" to "Cancel",
            "save" to "Save",
            "delete" to "Delete",
            "edit" to "Edit",
            "back" to "Back",
            "next" to "Next",
            "previous" to "Previous",
            "loading" to "Loading...",
            "error" to "Error",
            "success" to "Success",
            "warning" to "Warning",
            "language" to "Language",
            "english" to "English",
            "filipino" to "Filipino",
            "theme" to "Theme",
            "light" to "Light",
            "dark" to "Dark",
            "system_default" to "System Default",
            "common_diseases" to "Common Bitter Gourd Diseases",
            "prevention_tips_title" to "Prevention Tips",
            "search_diseases" to "Search diseases...",
            "edit_profile" to "Edit Profile",
            "change_password" to "Change Password",
            "privacy_settings" to "Privacy Settings",
            "recent_activity" to "Recent Activity",
            "total_scans" to "Total Scans",
            "diseases_found" to "Diseases Found",
            "scan_completed" to "Scan completed",
            "disease_detected" to "Disease detected",
            "healthy" to "Healthy",
            "demo_mode" to "Using demo mode for analysis",
            "ai_model_loaded" to "AI model loaded successfully",
            "using_fallback_mode" to "Using fallback detection mode",
            "photo_captured_successfully" to "Photo captured successfully!",
            "image_selected" to "Image selected!",
            "no_diseases_found" to "No diseases found matching '%s'",
            "search_functionality_coming_soon" to "Search functionality coming soon!",
            "profile_editing_coming_soon" to "Profile editing functionality coming in next version.",
            "password_change_coming_soon" to "Password change functionality coming in next version.",
            "privacy_settings_coming_soon" to "Privacy settings functionality coming in next version.",
            "forum_coming_soon" to "Forum functionality coming in next version.",
            "browse_diseases" to "Browse Diseases",
            "theme_changed_restart" to "Theme changed to %s. Restart app to apply changes.",
            "version_format" to "Version %s",
            "about_description" to "BitterGuard Mobile is an AI-powered application designed to help Filipino farmers identify and manage bitter gourd plant diseases. Using advanced machine learning technology, the app provides instant disease detection and treatment recommendations.",
            "features_title" to "Key Features",
            "features_list" to "• AI-powered disease detection\n• Instant scan results\n• Treatment recommendations\n• Disease information database\n• Scan history tracking\n• Multi-language support",
            "contact_title" to "Contact Information",
            "contact_info" to "For support and inquiries:\nEmail: support@bitterguard.com\nPhone: +63 912 345 6789\nWebsite: www.bitterguard.com",
            "fusarium_wilt" to "Fusarium Wilt",
            "downy_mildew" to "Downy Mildew",
            "mosaic_virus" to "Mosaic Virus",
            "healthy_plant" to "Healthy Plant",
            "severity_high" to "Severity: High",
            "severity_medium" to "Severity: Medium",
            "severity_low" to "Severity: Low",
            "severity_none" to "No Disease",
            "treatment" to "Treatment",
            "treatment_recommendations" to "Treatment Recommendations",
            "prevention_tips" to "Prevention Tips",
            "scan_history" to "Scan History",
            "date" to "Date",
            "time" to "Time",
            "location" to "Location",
            "confidence" to "Confidence",
            "prediction" to "Prediction",
            "take_photo" to "Take Photo",
            "choose_from_gallery" to "Choose from Gallery",
            "analyzing" to "Analyzing...",
            "scan_result" to "Scan Result",
            "scan_leaf_desc" to "Take a photo or choose from gallery to scan for diseases",
            "camera_preview" to "Camera Preview",
            "position_leaf" to "Position the leaf in the frame",
            "instructions_title" to "📋 Instructions",
            "instruction_lighting" to "• Ensure good lighting",
            "instruction_surface" to "• Place leaf on clean surface",
            "instruction_steady" to "• Keep camera steady",
            "instruction_capture" to "• Capture the entire leaf",
            "scan_diseases" to "🔍 Scan for Diseases",
            "select_image_first" to "Please select an image first",
            "permission_required" to "Permission Required",
            "camera_permission_denied" to "Camera permission is required to take photos",
            "gallery_permission_denied" to "Gallery permission is required to select photos",
            "network_error" to "Network error. Please try again.",
            "camera_error" to "Cannot open camera",
            "permission_denied" to "Permission denied",
            "invalid_phone" to "Invalid phone number",
            "invalid_otp" to "Invalid OTP"
        )
    }
    
    private fun getDefaultFilipinoTranslations(): Map<String, String> {
        return mapOf(
            "app_name" to "BitterGuard Mobile",
            "welcome_message" to "Maligayang pagdating sa BitterGuard",
            "scan_leaf" to "I-scan ang Dahon",
            "view_history" to "Tingnan ang Kasaysayan",
            "disease_info" to "Impormasyon sa Sakit",
            "continue_as_guest" to "Magpatuloy bilang Bisita",
            "login_with_phone" to "Mag-login gamit ang Telepono",
            "login_with_google" to "Mag-login gamit ang Google",
            "home" to "Bahay",
            "scan" to "I-scan",
            "history" to "Kasaysayan",
            "profile" to "Profile",
            "settings" to "Mga Setting",
            "about" to "Tungkol sa",
            "logout" to "Mag-logout",
            "ok" to "OK",
            "cancel" to "Kanselahin",
            "save" to "I-save",
            "delete" to "Tanggalin",
            "edit" to "I-edit",
            "back" to "Bumalik",
            "next" to "Susunod",
            "previous" to "Nakaraan",
            "loading" to "Naglo-load...",
            "error" to "Error",
            "success" to "Tagumpay",
            "warning" to "Babala",
            "language" to "Wika",
            "english" to "English",
            "filipino" to "Filipino",
            "theme" to "Tema",
            "light" to "Maliwanag",
            "dark" to "Madilim",
            "system_default" to "Default ng System",
            "common_diseases" to "Mga Karaniwang Sakit ng Ampalaya",
            "prevention_tips_title" to "Mga Tip sa Pag-iwas",
            "search_diseases" to "Maghanap ng mga sakit...",
            "edit_profile" to "I-edit ang Profile",
            "change_password" to "Palitan ang Password",
            "privacy_settings" to "Mga Setting ng Privacy",
            "recent_activity" to "Kamakailang Aktibidad",
            "total_scans" to "Kabuuang mga Scan",
            "diseases_found" to "Mga Sakit na Natagpuan",
            "scan_completed" to "Tapos na ang scan",
            "disease_detected" to "Natagpuan ang sakit",
            "healthy" to "Malusog",
            "demo_mode" to "Ginagamit ang demo mode para sa pagsusuri",
            "ai_model_loaded" to "Matagumpay na na-load ang AI model",
            "using_fallback_mode" to "Ginagamit ang fallback detection mode",
            "photo_captured_successfully" to "Matagumpay na nakuha ang larawan!",
            "image_selected" to "Napili ang larawan!",
            "no_diseases_found" to "Walang nakitang sakit na tumugma sa '%s'",
            "search_functionality_coming_soon" to "Darating na ang search functionality!",
            "profile_editing_coming_soon" to "Darating na ang profile editing functionality sa susunod na bersyon.",
            "password_change_coming_soon" to "Darating na ang password change functionality sa susunod na bersyon.",
            "privacy_settings_coming_soon" to "Darating na ang privacy settings functionality sa susunod na bersyon.",
            "forum_coming_soon" to "Darating na ang forum functionality sa susunod na bersyon.",
            "browse_diseases" to "Tingnan ang mga Sakit",
            "theme_changed_restart" to "Napalitan ang tema sa %s. I-restart ang app para ma-apply ang mga pagbabago.",
            "version_format" to "Version %s",
            "about_description" to "Ang BitterGuard Mobile ay isang AI-powered na application na idinisenyo para tulungan ang mga Filipino farmers na matukoy at pamahalaan ang mga sakit sa ampalaya plants. Gamit ang advanced machine learning technology, ang app ay nagbibigay ng instant disease detection at treatment recommendations.",
            "features_title" to "Mga Pangunahing Feature",
            "features_list" to "• AI-powered disease detection\n• Instant scan results\n• Treatment recommendations\n• Disease information database\n• Scan history tracking\n• Multi-language support",
            "contact_title" to "Impormasyon sa Contact",
            "contact_info" to "Para sa support at inquiries:\nEmail: support@bitterguard.com\nPhone: +63 912 345 6789\nWebsite: www.bitterguard.com",
            "fusarium_wilt" to "Fusarium Wilt",
            "downy_mildew" to "Downy Mildew",
            "mosaic_virus" to "Mosaic Virus",
            "healthy_plant" to "Malusog na Halaman",
            "severity_high" to "Kalubhaan: Mataas",
            "severity_medium" to "Kalubhaan: Katamtaman",
            "severity_low" to "Kalubhaan: Mababa",
            "severity_none" to "Walang sakit",
            "treatment" to "Paggamot",
            "treatment_recommendations" to "Mga rekomendasyon sa paggamot",
            "prevention_tips" to "Mga tip sa pag-iwas",
            "scan_history" to "Kasaysayan ng mga Scan",
            "date" to "Petsa",
            "time" to "Oras",
            "location" to "Lokasyon",
            "confidence" to "Kumpiyansa",
            "prediction" to "Prediksyon",
            "take_photo" to "Kumuha ng Larawan",
            "choose_from_gallery" to "Pumili mula sa Gallery",
            "analyzing" to "Sinusuri...",
            "scan_result" to "Resulta ng Scan",
            "scan_leaf_desc" to "Kumuha ng larawan o pumili mula sa gallery para i-scan ang mga sakit",
            "camera_preview" to "Camera Preview",
            "position_leaf" to "Iposisyon ang dahon sa frame",
            "instructions_title" to "📋 Mga Tagubilin",
            "instruction_lighting" to "• Siguraduhing may magandang ilaw",
            "instruction_surface" to "• Ilagay ang dahon sa malinis na surface",
            "instruction_steady" to "• Panatilihing steady ang camera",
            "instruction_capture" to "• Kunin ang buong dahon",
            "scan_diseases" to "🔍 I-scan para sa mga Sakit",
            "select_image_first" to "Mangyaring pumili muna ng larawan",
            "permission_required" to "Kailangan ang Permission",
            "camera_permission_denied" to "Kailangan ang camera permission para kumuha ng larawan",
            "gallery_permission_denied" to "Kailangan ang gallery permission para pumili ng larawan",
            "network_error" to "Error sa network. Subukan ulit.",
            "camera_error" to "Hindi mabuksan ang camera",
            "permission_denied" to "Hindi pinayagan ang permission",
            "invalid_phone" to "Hindi wasto ang numero ng telepono",
            "invalid_otp" to "Hindi wasto ang OTP"
        )
    }
    
    private fun loadCustomTranslations() {
        val customTranslationsJson = sharedPreferences.getString(KEY_CUSTOM_TRANSLATIONS, "{}")
        try {
            val jsonObject = JSONObject(customTranslationsJson ?: "{}")
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val language = keys.next()
                val translationsJson = jsonObject.getJSONObject(language)
                val translationMap = mutableMapOf<String, String>()
                val translationKeys = translationsJson.keys()
                while (translationKeys.hasNext()) {
                    val key = translationKeys.next()
                    translationMap[key] = translationsJson.getString(key)
                }
                translations[language] = translationMap
            }
        } catch (e: Exception) {
            // Handle JSON parsing error
        }
    }
    
    fun getCurrentLanguage(): String {
        return sharedPreferences.getString(KEY_CURRENT_LANGUAGE, "en") ?: "en"
    }
    
    fun setCurrentLanguage(language: String) {
        sharedPreferences.edit().putString(KEY_CURRENT_LANGUAGE, language).apply()
    }
    
    fun getString(key: String, vararg args: Any): String {
        val currentLang = getCurrentLanguage()
        val translation = translations[currentLang]?.get(key) ?: translations["en"]?.get(key) ?: key
        
        return if (args.isNotEmpty()) {
            try {
                String.format(translation, *args)
            } catch (e: Exception) {
                translation
            }
        } else {
            translation
        }
    }
    
    fun addCustomTranslation(language: String, key: String, value: String) {
        val currentTranslations = translations[language]?.toMutableMap() ?: mutableMapOf()
        currentTranslations[key] = value
        translations[language] = currentTranslations
        saveCustomTranslations()
    }
    
    fun removeCustomTranslation(language: String, key: String) {
        val currentTranslations = translations[language]?.toMutableMap() ?: return
        currentTranslations.remove(key)
        translations[language] = currentTranslations
        saveCustomTranslations()
    }
    
    fun getAvailableLanguages(): List<String> {
        return translations.keys.toList()
    }
    
    fun getTranslationsForLanguage(language: String): Map<String, String> {
        return translations[language] ?: emptyMap()
    }
    
    fun addNewLanguage(language: String, translations: Map<String, String>) {
        this.translations[language] = translations
        saveCustomTranslations()
    }
    
    private fun saveCustomTranslations() {
        val jsonObject = JSONObject()
        translations.forEach { (language, translationMap) ->
            val languageJson = JSONObject()
            translationMap.forEach { (key, value) ->
                languageJson.put(key, value)
            }
            jsonObject.put(language, languageJson)
        }
        sharedPreferences.edit().putString(KEY_CUSTOM_TRANSLATIONS, jsonObject.toString()).apply()
    }
    
    fun resetToDefaults() {
        sharedPreferences.edit().remove(KEY_CUSTOM_TRANSLATIONS).apply()
        loadTranslations()
    }
    
    fun exportTranslations(): String {
        val jsonObject = JSONObject()
        translations.forEach { (language, translationMap) ->
            val languageJson = JSONObject()
            translationMap.forEach { (key, value) ->
                languageJson.put(key, value)
            }
            jsonObject.put(language, languageJson)
        }
        return jsonObject.toString(2)
    }
    
    fun importTranslations(jsonString: String): Boolean {
        return try {
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val language = keys.next()
                val translationsJson = jsonObject.getJSONObject(language)
                val translationMap = mutableMapOf<String, String>()
                val translationKeys = translationsJson.keys()
                while (translationKeys.hasNext()) {
                    val key = translationKeys.next()
                    translationMap[key] = translationsJson.getString(key)
                }
                translations[language] = translationMap
            }
            saveCustomTranslations()
            true
        } catch (e: Exception) {
            false
        }
    }
}
