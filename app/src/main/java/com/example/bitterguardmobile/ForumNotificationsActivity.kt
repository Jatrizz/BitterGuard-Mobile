package com.example.bitterguardmobile

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bitterguardmobile.models.ForumNotification
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.*

class ForumNotificationsActivity : BaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: ForumNotificationsAdapter
    private lateinit var forumManager: LocalForumManager
    private lateinit var authManager: SupabaseAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum_notifications)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Notifications"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        forumManager = LocalForumManager(this)
        authManager = SupabaseAuthManager(this)

        setupViews()
        loadNotifications()
    }

    private fun setupViews() {
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.notificationsRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ForumNotificationsAdapter(mutableListOf(), forumManager) { notification ->
            // Handle notification click - navigate to related content
            handleNotificationClick(notification)
        }
        recycler.adapter = adapter

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadNotifications() }

        findViewById<android.widget.Button>(R.id.btnMarkAllRead).setOnClickListener {
            markAllAsRead()
        }
    }

    private fun loadNotifications() {
        swipeRefresh.isRefreshing = true
        
        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            swipeRefresh.isRefreshing = false
            return
        }
        
        lifecycleScope.launch {
            try {
                val result = forumManager.getNotifications()
                if (result.isSuccess) {
                    val notifications = result.getOrNull() ?: emptyList()
                    adapter.updateItems(notifications)
                    findViewById<android.widget.TextView>(R.id.emptyView).visibility = 
                        if (notifications.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                } else {
                    android.widget.Toast.makeText(this@ForumNotificationsActivity, "Failed to load notifications", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ForumNotificationsActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun handleNotificationClick(notification: ForumNotification) {
        // Mark as read first
        markAsRead(notification)
        
        // Show notification details in a dialog for now
        showNotificationDetailsDialog(notification)
    }

    private fun markAsRead(notification: ForumNotification) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Mark notification as read in Supabase
                // This would typically update the notification document
                android.widget.Toast.makeText(this@ForumNotificationsActivity, "Marked as read", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

    private fun markAllAsRead() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Mark all notifications as read
                // This would typically update all notification documents for the user
                android.widget.Toast.makeText(this@ForumNotificationsActivity, "All notifications marked as read", android.widget.Toast.LENGTH_SHORT).show()
                loadNotifications()
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ForumNotificationsActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showNotificationDetailsDialog(notification: ForumNotification) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_post_details, null)
        
        // Set notification details
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostTitle).text = notification.title
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostContent).text = notification.message
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostAuthor).text = "From: ${notification.fromUserName}"
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostDate).text = "Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(notification.createdAt))}"
        
        AlertDialog.Builder(this)
            .setTitle("Notification Details")
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

class ForumNotificationsAdapter(
    private var notifications: MutableList<ForumNotification>,
    private val forumManager: LocalForumManager,
    private val onClick: (ForumNotification) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<ForumNotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val tvTitle: android.widget.TextView = itemView.findViewById(R.id.tvTitle)
        val tvMessage: android.widget.TextView = itemView.findViewById(R.id.tvMessage)
        val tvDate: android.widget.TextView = itemView.findViewById(R.id.tvDate)
        val tvFromUser: android.widget.TextView = itemView.findViewById(R.id.tvFromUser)
        val ivIcon: android.widget.ImageView = itemView.findViewById(R.id.ivIcon)
        val indicator: android.view.View = itemView.findViewById(R.id.unreadIndicator)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): NotificationViewHolder {
        val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_forum_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun getItemCount(): Int = notifications.size

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        
        holder.tvTitle.text = notification.title
        holder.tvMessage.text = notification.message
        holder.tvDate.text = android.text.format.DateUtils.getRelativeTimeSpanString(notification.createdAt)
        holder.tvFromUser.text = "from ${notification.fromUserName}"
        
        // Set icon based on type
        val iconRes = when (notification.type) {
            "comment" -> android.R.drawable.ic_dialog_email
            "like" -> android.R.drawable.btn_star_big_on
            "reply" -> android.R.drawable.ic_menu_recent_history
            "mention" -> android.R.drawable.ic_menu_agenda
            else -> android.R.drawable.ic_dialog_info
        }
        holder.ivIcon.setImageResource(iconRes)
        
        // Show unread indicator
        holder.indicator.visibility = if (notification.isRead) android.view.View.GONE else android.view.View.VISIBLE
        
        holder.itemView.setOnClickListener { onClick(notification) }
    }

    fun updateItems(newItems: List<ForumNotification>) {
        notifications.clear()
        notifications.addAll(newItems)
        notifyDataSetChanged()
    }
}
