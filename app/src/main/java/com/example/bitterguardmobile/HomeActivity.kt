package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomeActivity : AppCompatActivity() {
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
                    val intent = Intent(this, ForumActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
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
        }
    }

    private fun setupToolNavigation() {
        // Scan Card
        val scanCard = findViewById<CardView>(R.id.scanCard)
        scanCard?.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        // History Card
        val historyCard = findViewById<CardView>(R.id.historyCard)
        historyCard?.setOnClickListener {
            val intent = Intent(this, ViewHistoryActivity::class.java)
            startActivity(intent)
        }

        // Disease Card
        val diseaseCard = findViewById<CardView>(R.id.diseaseCard)
        diseaseCard?.setOnClickListener {
            val intent = Intent(this, DiseaseInfoActivity::class.java)
            startActivity(intent)
        }

        // Treatment Card
        val treatmentCard = findViewById<CardView>(R.id.treatmentCard)
        treatmentCard?.setOnClickListener {
            val intent = Intent(this, TreatmentGuideActivity::class.java)
            startActivity(intent)
        }

        // Forum Card
        val forumCard = findViewById<CardView>(R.id.forumCard)
        forumCard?.setOnClickListener {
            val intent = Intent(this, ForumActivity::class.java)
            startActivity(intent)
        }
    }
}