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
    private lateinit var supabaseForum: SupabaseForumService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum_bookmarks)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Bookmarks"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        forumManager = LocalForumManager(this)
        authManager = SupabaseAuthManager(this)
        supabaseForum = SupabaseForumService(this)

        setupViews()
        loadBookmarks()
    }

    private fun setupViews() {
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.postsRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ForumPostsAdapter(
            mutableListOf(), 
            forumManager,
            onClick = { post ->
                // Show post details in a dialog for now
                showPostDetailsDialog(post)
            },
            onBookmarkClick = { post ->
                // Custom bookmark handler for bookmarks page
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = supabaseForum.toggleBookmarkPost(post.id)
                        if (result.isSuccess) {
                            val isBookmarked = result.getOrNull() ?: false
                            
                            if (!isBookmarked) {
                                // Post was unbookmarked, refresh the entire list
                                loadBookmarks()
                            }
                            
                            android.widget.Toast.makeText(this@ForumBookmarksActivity, 
                                if (isBookmarked) "Bookmarked!" else "Removed from bookmarks", 
                                android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(this@ForumBookmarksActivity, "Failed to toggle bookmark", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(this@ForumBookmarksActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        recycler.adapter = adapter

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadBookmarks() }
    }

    private fun loadBookmarks() {
        swipeRefresh.isRefreshing = true
        
        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            swipeRefresh.isRefreshing = false
            android.widget.Toast.makeText(this, "Please log in to view bookmarks", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // Get user's bookmarked post IDs from Supabase
                val bookmarkResult = supabaseForum.getUserBookmarks()
                
                if (bookmarkResult.isSuccess) {
                    val bookmarkedPostIds = bookmarkResult.getOrNull() ?: emptyList()
                    android.util.Log.d("ForumBookmarks", "Found ${bookmarkedPostIds.size} bookmarked post IDs: $bookmarkedPostIds")
                    
                    if (bookmarkedPostIds.isEmpty()) {
                        adapter.updateItems(emptyList<ForumPost>())
                        findViewById<android.widget.TextView>(R.id.emptyView).visibility = android.view.View.VISIBLE
                    } else {
                        // Get the actual posts for these IDs
                        val postsResult = supabaseForum.getPostsByIds(bookmarkedPostIds)
                        if (postsResult.isSuccess) {
                            val allPosts = postsResult.getOrNull() ?: emptyList<ForumPost>()
                            
                            // Double-check that each post is actually bookmarked by the current user
                            val verifiedBookmarkedPosts = mutableListOf<ForumPost>()
                            for (post in allPosts) {
                                val isBookmarkedResult = supabaseForum.isPostBookmarked(post.id)
                                val isBookmarked = isBookmarkedResult.isSuccess && (isBookmarkedResult.getOrNull() ?: false)
                                android.util.Log.d("ForumBookmarks", "Post ${post.id} (${post.title}) - isBookmarked: $isBookmarked")
                                if (isBookmarked) {
                                    verifiedBookmarkedPosts.add(post)
                                }
                            }
                            
                            android.util.Log.d("ForumBookmarks", "Verified ${verifiedBookmarkedPosts.size} bookmarked posts out of ${allPosts.size} total posts")
                            adapter.updateItems(verifiedBookmarkedPosts)
                            findViewById<android.widget.TextView>(R.id.emptyView).visibility = 
                                if (verifiedBookmarkedPosts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                        } else {
                            android.widget.Toast.makeText(this@ForumBookmarksActivity, "Error loading bookmarked posts: ${postsResult.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_SHORT).show()
                            adapter.updateItems(emptyList<ForumPost>())
                            findViewById<android.widget.TextView>(R.id.emptyView).visibility = android.view.View.VISIBLE
                        }
                    }
                } else {
                    android.widget.Toast.makeText(this@ForumBookmarksActivity, "Error loading bookmarks: ${bookmarkResult.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_SHORT).show()
                    adapter.updateItems(emptyList<ForumPost>())
                    findViewById<android.widget.TextView>(R.id.emptyView).visibility = android.view.View.VISIBLE
                }
                    
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ForumBookmarksActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                adapter.updateItems(emptyList<ForumPost>())
                findViewById<android.widget.TextView>(R.id.emptyView).visibility = android.view.View.VISIBLE
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
