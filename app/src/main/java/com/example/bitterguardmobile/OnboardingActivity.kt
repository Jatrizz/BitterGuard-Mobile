package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var skipButton: Button
    private lateinit var nextButton: Button
    private lateinit var getStartedButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        skipButton = findViewById(R.id.skipButton)
        nextButton = findViewById(R.id.nextButton)
        getStartedButton = findViewById(R.id.getStartedButton)

        // Set up ViewPager with adapter
        val onboardingAdapter = OnboardingAdapter()
        viewPager.adapter = onboardingAdapter

        // Set up tab layout with ViewPager
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // Set up button listeners
        skipButton.setOnClickListener {
            startMainActivity()
        }

        nextButton.setOnClickListener {
            if (viewPager.currentItem < onboardingAdapter.itemCount - 1) {
                viewPager.currentItem += 1
            } else {
                startMainActivity()
            }
        }

        getStartedButton.setOnClickListener {
            startMainActivity()
        }

        // Update button visibility based on current page
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == onboardingAdapter.itemCount - 1) {
                    nextButton.visibility = View.GONE
                    getStartedButton.visibility = View.VISIBLE
                } else {
                    nextButton.visibility = View.VISIBLE
                    getStartedButton.visibility = View.GONE
                }
            }
        })
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
} 