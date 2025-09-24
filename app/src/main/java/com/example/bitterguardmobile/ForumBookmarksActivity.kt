package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bitterguardmobile.models.ForumPost
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.*

class ForumBookmarksActivity : BaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: ForumPostsAdapter
    private lateinit var forumManager: LocalForumManager
    private lateinit var authManager: SupabaseAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum_bookmarks)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Bookmarks"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        forumManager = LocalForumManager(this)
        authManager = SupabaseAuthManager(this)

        setupViews()
        loadBookmarks()
    }

    private fun setupViews() {
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.postsRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ForumPostsAdapter(mutableListOf(), forumManager) { post ->
            // Show post details in a dialog for now
            showPostDetailsDialog(post)
        }
        recycler.adapter = adapter

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadBookmarks() }
    }

    private fun loadBookmarks() {
        swipeRefresh.isRefreshing = true
        
        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            swipeRefresh.isRefreshing = false
            return
        }
        
        lifecycleScope.launch {
            try {
                // Get user's bookmarks using Supabase
                val result = forumManager.getBookmarks()
                
                if (result.isSuccess) {
                    val bookmarks = result.getOrNull() ?: emptyList()
                    // For now, we'll show empty list since we haven't implemented bookmark parsing yet
                    adapter.updateItems(emptyList())
                    findViewById<android.widget.TextView>(R.id.emptyView).visibility = 
                        android.view.View.VISIBLE
                } else {
                    android.widget.Toast.makeText(this@ForumBookmarksActivity, "Error loading bookmarks: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
                    
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ForumBookmarksActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun showPostDetailsDialog(post: ForumPost) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_post_details, null)
        
        // Set post details
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostTitle).text = post.title
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostContent).text = post.content
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostAuthor).text = "By: ${post.authorName}"
        dialogView.findViewById<android.widget.TextView>(R.id.tvPostDate).text = "Posted: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(post.createdAt))}"
        
        AlertDialog.Builder(this)
            .setTitle("Post Details")
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
