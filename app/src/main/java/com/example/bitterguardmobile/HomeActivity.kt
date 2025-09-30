package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.location.Geocoder
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import android.widget.TextView
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import org.json.JSONObject
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL

class HomeActivity : AppCompatActivity() {
    private lateinit var requestLocationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private var locationCityTextView: TextView? = null
    private var locationRegionTextView: TextView? = null
    private var locationCountryTextView: TextView? = null
    private var temperatureTextView: TextView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))

        // Set up bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_forum -> {
                    val offline = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("offline_mode", false)
                    return@setOnItemSelectedListener if (offline) {
                        android.widget.Toast.makeText(this, getString(R.string.forum_online_only), android.widget.Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        val intent = Intent(this, ForumActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        true
                    }
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        // Set up tool navigation
        setupToolNavigation()

        // Set up floating action button (camera)
        val fabScanner = findViewById<FloatingActionButton>(R.id.fabScanner)
        fabScanner.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Initialize weather/location views
        locationCityTextView = findViewById(R.id.locationCityTextView)
        locationRegionTextView = findViewById(R.id.locationRegionTextView)
        locationCountryTextView = findViewById(R.id.locationCountryTextView)
        temperatureTextView = findViewById(R.id.temperatureTextView)

        // Initialize permission launcher
        initializePermissionLauncher()
        ensureLocationAndUpdateWeather()
    }

    private fun setupToolNavigation() {
        // Scan Card
        val scanCard = findViewById<CardView>(R.id.scanCard)
        scanCard?.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // History Card
        val historyCard = findViewById<CardView>(R.id.historyCard)
        historyCard?.setOnClickListener {
            val intent = Intent(this, ViewHistoryActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Disease Card
        val diseaseCard = findViewById<CardView>(R.id.diseaseCard)
        diseaseCard?.setOnClickListener {
            val intent = Intent(this, DiseaseInfoActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Treatment Card
        val treatmentCard = findViewById<CardView>(R.id.treatmentCard)
        treatmentCard?.setOnClickListener {
            val intent = Intent(this, TreatmentGuideActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Forum Card
        val forumCard = findViewById<CardView>(R.id.forumCard)
        forumCard?.setOnClickListener {
            val offline = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("offline_mode", false)
            if (offline) {
                android.widget.Toast.makeText(this, getString(R.string.forum_online_only), android.widget.Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, ForumActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }
    }

    private fun initializePermissionLauncher() {
        requestLocationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    updateLocationAndWeather()
                } else {
                    setLocationTexts("Location", "permission", "denied")
                }
            }
    }

    private fun ensureLocationAndUpdateWeather() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            updateLocationAndWeather()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Location (Optional)")
                .setMessage("Allow access to your location to show local temperature.")
                .setPositiveButton("Allow") { _, _ ->
                    requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                .setNegativeButton("Skip") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun updateLocationAndWeather() {
        lifecycleScope.launchWhenStarted {
            try {
                val locationData = LocationService.getCurrentLocation(this@HomeActivity)
                if (locationData != null) {
                    setLocationTexts(locationData.city, locationData.region, locationData.country)
                    val temp = fetchCurrentTemperatureCelsius(locationData.latitude, locationData.longitude)
                    temperatureTextView?.text = if (temp != null) "${temp}°C" else "--°C"
                } else {
                    setLocationTexts("Location", "not", "available")
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error getting location: ${e.message}")
                setLocationTexts("Location", "not", "available")
            }
        }
    }

    private fun getBestLastKnownLocation(): android.location.Location? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        var bestLocation: android.location.Location? = null
        for (provider in providers) {
            val loc = try { locationManager.getLastKnownLocation(provider) } catch (_: SecurityException) { null } ?: continue
            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                bestLocation = loc
            }
        }
        return bestLocation
    }

    private fun tryReverseGeocodeParts(lat: Double, lon: Double): Triple<String, String, String>? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val results = geocoder.getFromLocation(lat, lon, 1)
            if (!results.isNullOrEmpty()) {
                val addr = results[0]
                val city = listOfNotNull(addr.locality, addr.subAdminArea).firstOrNull()
                    ?: addr.adminArea ?: addr.featureName ?: ""
                val region = addr.adminArea ?: addr.subAdminArea ?: ""
                val country = addr.countryName ?: ""
                Triple(city, region, country)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun setLocationTexts(city: String, region: String, country: String) {
        locationCityTextView?.text = city
        locationRegionTextView?.text = region
        locationCountryTextView?.text = country
    }

    private fun requestSingleLocationFix(onResult: (android.location.Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onResult(null)
            return
        }
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                locationManager.removeUpdates(this)
                onResult(location)
            }
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener)
            // Fallback timeout: stop after 10s if no fix
            temperatureTextView?.postDelayed({
                try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
                onResult(null)
            }, 10000)
        } catch (_: SecurityException) {
            onResult(null)
        }
    }

    private suspend fun fetchCurrentTemperatureCelsius(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
            }
            try {
                val code = conn.responseCode
                if (code in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { br -> br.readText() }
                    val json = JSONObject(body)
                    val current = json.optJSONObject("current_weather")
                    val temp = current?.optDouble("temperature", Double.NaN)
                    if (temp != null && !temp.isNaN()) {
                        return@withContext String.format(Locale.getDefault(), "%.1f", temp)
                    }
                }
            } finally {
                try { conn.disconnect() } catch (_: Exception) {}
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}