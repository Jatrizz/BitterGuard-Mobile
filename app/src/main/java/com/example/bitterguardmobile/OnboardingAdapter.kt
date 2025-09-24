package com.example.bitterguardmobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    private val onboardingItems = listOf(
        OnboardingItem(
            R.drawable.camera,
            "Smart Leaf Scanning",
            "Take a photo of your bitter gourd leaves and get instant disease detection using advanced AI technology."
        ),
        OnboardingItem(
            R.drawable.disease,
            "Disease Information",
            "Learn about common bitter gourd diseases, their symptoms, and how to identify them early."
        ),
        OnboardingItem(
            R.drawable.treatment,
            "Treatment Guides",
            "Get expert treatment recommendations and organic solutions to keep your plants healthy."
        ),
        OnboardingItem(
            R.drawable.forum,
            "Community Support",
            "Connect with fellow farmers, share experiences, and get advice from the BitterGuard community."
        )
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(onboardingItems[position])
    }

    override fun getItemCount(): Int = onboardingItems.size

    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.onboardingImage)
        private val titleTextView: TextView = itemView.findViewById(R.id.onboardingTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.onboardingDescription)

        fun bind(item: OnboardingItem) {
            imageView.setImageResource(item.imageResId)
            titleTextView.text = item.title
            descriptionTextView.text = item.description
        }
    }

    data class OnboardingItem(
        val imageResId: Int,
        val title: String,
        val description: String
    )
} 