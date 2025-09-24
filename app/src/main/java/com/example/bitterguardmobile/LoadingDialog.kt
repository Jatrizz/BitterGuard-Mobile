package com.example.bitterguardmobile

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View // Import View for visibility control
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
//import androidx.glance.visibility

class LoadingDialog(private val context: Context) {

    private var dialog: Dialog? = null

    /**
     * Show loading dialog with default messages.
     * This will call the two-argument show method.
     */
    fun show() {
        show("Analyzing leaf image...", "Please wait while we detect diseases")
    }

    /**
     * Show loading dialog with a custom title and a default/empty subtitle.
     * This is the new method you requested.
     */
    fun show(title: String) {
        // Option 1: Call the two-argument show with an empty subtitle
        // show(title, "")

        // Option 2: Call the two-argument show with a default subtitle
        // show(title, "Processing...")

        // Option 3: (More direct control) - Re-implement part of the show logic
        // to specifically hide or set a default for the subtitle.
        // This is often cleaner if you want distinct behavior.
        if (dialog?.isShowing == true) {
            // If you want to update the message if already showing, call updateMessage
            // updateMessage(title, "") // or a default subtitle
            return // Or allow re-showing/updating if desired
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)

        dialogView.findViewById<TextView>(R.id.loadingText).text = title

        // Decide how to handle the subtitle TextView (R.id.loadingSubtext)
        val subtitleTextView = dialogView.findViewById<TextView>(R.id.loadingSubtext)
        subtitleTextView.text = "Please wait..." // Set a default subtitle
        // OR hide it if no specific subtitle is needed for this version of show():
        // subtitleTextView.visibility = View.GONE
        // OR set it to an empty string:
        // subtitleTextView.text = ""


        dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog?.show()
    }

    /**
     * Show loading dialog with custom title and subtitle.
     */
    fun show(title: String, subtitle: String) {
        if (dialog?.isShowing == true) {
            // If you want to update the message if already showing, call updateMessage
            // updateMessage(title, subtitle)
            return // Or allow re-showing/updating if desired
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)

        // Set custom messages if provided
        dialogView.findViewById<TextView>(R.id.loadingText).text = title
        val subtitleTextView = dialogView.findViewById<TextView>(R.id.loadingSubtext)
        subtitleTextView.text = subtitle
        subtitleTextView.visibility = if (subtitle.isEmpty()) View.GONE else View.VISIBLE


        dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog?.show()
    }

    /**
     * Update loading dialog messages.
     * This can be used if the dialog is already showing and you want to change its text.
     */
    fun updateMessage(title: String, subtitle: String? = null) { // Made subtitle optional
        dialog?.let { currentDialog ->
            // It's generally better to find views from the dialog's window content view
            // if the dialog is already created and showing.
            // However, your original approach of finding them from a newly inflated view
            // inside `show` means `updateMessage` might need to work on the existing views.

            val titleView = currentDialog.findViewById<TextView>(R.id.loadingText)
            val subtextView = currentDialog.findViewById<TextView>(R.id.loadingSubtext)

            titleView?.text = title
            if (subtitle != null) {
                subtextView?.text = subtitle
                subtextView?.visibility = View.VISIBLE
            } else {
                subtextView?.visibility = View.GONE // Or set a default, or empty
            }
        }
    }

    /**
     * Hide loading dialog
     */
    fun hide() {
        dialog?.dismiss()
        dialog = null // Release the dialog instance
    }

    /**
     * Check if dialog is showing
     */
    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }
}
