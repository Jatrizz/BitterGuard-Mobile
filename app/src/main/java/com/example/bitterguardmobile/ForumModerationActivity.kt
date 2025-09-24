package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bitterguardmobile.models.ForumReport
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.*

class ForumModerationActivity : BaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: ForumReportsAdapter
    private lateinit var forumManager: LocalForumManager
    private lateinit var authManager: SupabaseAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum_moderation)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Forum Moderation"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        forumManager = LocalForumManager(this)
        authManager = SupabaseAuthManager(this)

        setupViews()
        loadReports()
    }

    private fun setupViews() {
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.reportsRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ForumReportsAdapter(mutableListOf(), forumManager) { report ->
            // Handle report click - could show report details
            showReportDetailsDialog(report)
        }
        recycler.adapter = adapter

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadReports() }
    }

    private fun loadReports() {
        swipeRefresh.isRefreshing = true
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = forumManager.getReports()
                if (result.isSuccess) {
                    val reports = result.getOrNull() ?: emptyList()
                    adapter.updateItems(reports)
                    findViewById<android.widget.TextView>(R.id.emptyView).visibility = 
                        if (reports.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                } else {
                    android.widget.Toast.makeText(this@ForumModerationActivity, "Failed to load reports", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ForumModerationActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showReportDetailsDialog(report: ForumReport) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_report_details, null)
        
        // Populate dialog with report details
        dialogView.findViewById<android.widget.TextView>(R.id.tvReportReason).text = report.reason
        dialogView.findViewById<android.widget.TextView>(R.id.tvReportDescription).text = report.description
        dialogView.findViewById<android.widget.TextView>(R.id.tvReportDate).text = 
            android.text.format.DateUtils.getRelativeTimeSpanString(report.createdAt)
        dialogView.findViewById<android.widget.TextView>(R.id.tvContentType).text = 
            if (report.reportedContentType == "post") "Post" else "Comment"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Report Details")
            .setView(dialogView)
            .setPositiveButton("Resolve") { _, _ ->
                showResolveDialog(report)
            }
            .setNegativeButton("Dismiss") { _, _ ->
                dismissReport(report)
            }
            .setNeutralButton("View Content") { _, _ ->
                // Show content in a dialog for now
                showReportedContentDialog(report)
            }
            .show()
    }

    private fun showResolveDialog(report: ForumReport) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_resolve_report, null)
        val etNotes = dialogView.findViewById<android.widget.EditText>(R.id.etNotes)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Resolve Report")
            .setView(dialogView)
            .setPositiveButton("Resolve") { _, _ ->
                val notes = etNotes.text.toString().trim()
                resolveReport(report, "resolved", notes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resolveReport(report: ForumReport, status: String, notes: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = forumManager.resolveReport(report.id)
                if (result.isSuccess) {
                    android.widget.Toast.makeText(this@ForumModerationActivity, "Report resolved successfully", android.widget.Toast.LENGTH_SHORT).show()
                    loadReports()
                } else {
                    android.widget.Toast.makeText(this@ForumModerationActivity, "Failed to resolve report", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ForumModerationActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dismissReport(report: ForumReport) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = forumManager.resolveReport(report.id)
                if (result.isSuccess) {
                    android.widget.Toast.makeText(this@ForumModerationActivity, "Report dismissed", android.widget.Toast.LENGTH_SHORT).show()
                    loadReports()
                } else {
                    android.widget.Toast.makeText(this@ForumModerationActivity, "Failed to dismiss report", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ForumModerationActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.moderation_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_resolved_reports -> {
                // Show resolved reports
                loadResolvedReports()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadResolvedReports() {
        swipeRefresh.isRefreshing = true
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = forumManager.getReports()
                if (result.isSuccess) {
                    val reports = result.getOrNull() ?: emptyList()
                    adapter.updateItems(reports)
                    findViewById<android.widget.TextView>(R.id.emptyView).visibility = 
                        if (reports.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                } else {
                    android.widget.Toast.makeText(this@ForumModerationActivity, "Failed to load resolved reports", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ForumModerationActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun showReportedContentDialog(report: ForumReport) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_post_details, null)
        
        // Set content details
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostTitle).text = "Reported Content"
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostContent).text = "Content ID: ${report.reportedContentId}\nType: ${report.reportedContentType}\nReason: ${report.reason}"
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostAuthor).text = "Reported by: ${report.reporterUid}"
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostDate).text = "Reported: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(report.createdAt))}"
        
        AlertDialog.Builder(this)
            .setTitle("Reported Content Details")
            .setView(dialogView)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class ForumReportsAdapter(
    private var reports: MutableList<ForumReport>,
    private val forumManager: LocalForumManager,
    private val onClick: (ForumReport) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<ForumReportsAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val tvReason: android.widget.TextView = itemView.findViewById(R.id.tvReason)
        val tvContentType: android.widget.TextView = itemView.findViewById(R.id.tvContentType)
        val tvDate: android.widget.TextView = itemView.findViewById(R.id.tvDate)
        val tvStatus: android.widget.TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ReportViewHolder {
        val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_forum_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun getItemCount(): Int = reports.size

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        
        holder.tvReason.text = report.reason
        holder.tvContentType.text = if (report.reportedContentType == "post") "Post" else "Comment"
        holder.tvDate.text = android.text.format.DateUtils.getRelativeTimeSpanString(report.createdAt)
        holder.tvStatus.text = report.status.capitalize()
        
        // Set status color
        val statusColor = when (report.status) {
            "pending" -> android.graphics.Color.parseColor("#FF9800")
            "resolved" -> android.graphics.Color.parseColor("#4CAF50")
            "dismissed" -> android.graphics.Color.parseColor("#9E9E9E")
            else -> android.graphics.Color.parseColor("#666666")
        }
        holder.tvStatus.setTextColor(statusColor)
        
        holder.itemView.setOnClickListener { onClick(report) }
    }

    fun updateItems(newItems: List<ForumReport>) {
        reports.clear()
        reports.addAll(newItems)
        notifyDataSetChanged()
    }
}
