package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import com.example.bitterguardmobile.models.ForumPost

class ForumModerationActivity : BaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: ForumPostsAdapter
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
        loadPosts()
    }

    private fun setupViews() {
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.postsRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ForumPostsAdapter(mutableListOf(), forumManager) { post ->
            // Handle post click - show post details
            showPostDetailsDialog(post)
        }
        recycler.adapter = adapter

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadPosts() }
    }

    private fun loadPosts() {
        swipeRefresh.isRefreshing = true
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = forumManager.getPosts()
                if (result.isSuccess) {
                    val posts = result.getOrNull() ?: emptyList()
                    adapter.updateItems(posts)
                    findViewById<android.widget.TextView>(R.id.emptyView).visibility = 
                        if (posts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                } else {
                    android.widget.Toast.makeText(this@ForumModerationActivity, "Failed to load posts", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ForumModerationActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Post Details")
            .setView(dialogView)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.moderation_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

