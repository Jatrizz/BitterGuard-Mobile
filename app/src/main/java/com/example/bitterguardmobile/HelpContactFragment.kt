package com.example.bitterguardmobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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

        view.findViewById<LinearLayout>(R.id.contactPhoneJomarr).setOnClickListener {
            val phone = "09984551432"
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            startActivity(intent)
        }

        view.findViewById<LinearLayout>(R.id.contactPhoneJanna).setOnClickListener {
            val phone = "09480870179"
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            startActivity(intent)
        }

        view.findViewById<LinearLayout>(R.id.contactPhoneJohn).setOnClickListener {
            val phone = "09777741589"
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            startActivity(intent)
        }
    }
}


