package com.example.bitterguardmobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import androidx.fragment.app.Fragment

class HelpFaqFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_help_faq, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val faqList = view.findViewById<ExpandableListView>(R.id.faqList)

        val categories = listOf("General", "Account", "Payment", "Services")
        val faqMap = mapOf(
            "General" to listOf(
                "How do I manage my notifications?" to "Go to Settings → Notifications to customize alerts.",
                "Is my data safe and private?" to "We use Supabase Auth and PostgreSQL with secure rules.",
                "Does the app work offline?" to "You can view saved scan results offline; scanning requires internet."
            ),
            "Account" to listOf(
                "How do I change my password?" to "Open Profile → Change Password and follow the steps.",
                "How do I edit my profile?" to "Open Profile and tap the Edit button to update your info.",
                "How do I log out?" to "Open Profile and tap Logout."
            ),
            "Payment" to listOf(
                "Is the app free?" to "Yes. All core features are free for now.",
                "Are there subscriptions?" to "No subscriptions yet. We may add premium features later."
            ),
            "Services" to listOf(
                "How do I start a scan?" to "From Home, tap Scan, choose Camera or Gallery, then analyze.",
                "Where can I see my history?" to "Open History from the bottom navigation to view past scans.",
                "How do I get treatment advice?" to "After a scan, open Treatment Guide for recommendations."
            )
        )

        faqList.setAdapter(HelpFaqAdapter(requireContext(), categories, faqMap))
        // Expand first group by default
        faqList.expandGroup(0)
    }
}


