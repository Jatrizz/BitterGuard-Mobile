package com.example.bitterguardmobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment

class HelpContactFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_help_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<LinearLayout>(R.id.contactEmail).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@bitterguard.com"))
                putExtra(Intent.EXTRA_SUBJECT, "BitterGuard Mobile Support")
            }
            startActivity(Intent.createChooser(intent, getString(R.string.contact_support)))
        }

        view.findViewById<LinearLayout>(R.id.contactWhatsApp).setOnClickListener {
            val url = "https://wa.me/639123456789"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        view.findViewById<LinearLayout>(R.id.visitWebsite).setOnClickListener {
            val url = "https://www.bitterguard.com"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        view.findViewById<LinearLayout>(R.id.openFacebook).setOnClickListener {
            val url = "https://www.facebook.com/bitterguard"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        view.findViewById<LinearLayout>(R.id.openTwitter).setOnClickListener {
            val url = "https://twitter.com/bitterguard"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        view.findViewById<LinearLayout>(R.id.openInstagram).setOnClickListener {
            val url = "https://instagram.com/bitterguard"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}


