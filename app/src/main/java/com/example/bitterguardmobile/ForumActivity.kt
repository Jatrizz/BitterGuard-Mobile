package com.example.bitterguardmobile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bitterguardmobile.models.ForumPost
import kotlinx.coroutines.*

class ForumActivity : BaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: ForumPostsAdapter
    private lateinit var forumManager: LocalForumManager
    
    private var currentCategory = "All"
    private var currentSort = "created_at"
    private var currentSortOrder = "desc"
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.community_forum)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        forumManager = LocalForumManager(this)

        setupViews()
        setupSearch()
        setupFiltering()
        loadPosts()
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
        swipeRefresh.setOnRefreshListener { loadPosts() }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddPost)
            .setOnClickListener { showCreatePostDialog() }
    }
    
    private fun setupSearch() {
        val searchEditText = findViewById<android.widget.EditText>(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim()
                loadPosts()
            }
        })
    }
    
    private fun setupFiltering() {
        val categorySpinner = findViewById<android.widget.Spinner>(R.id.categorySpinner)
        val sortSpinner = findViewById<android.widget.Spinner>(R.id.sortSpinner)
        
        // Setup category spinner
        val categories = listOf("All", "General", "Diseases", "Tips & Advice", "Harvest & Yield", "Equipment")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentCategory = categories[position]
                loadPosts()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Setup sort spinner
        val sortOptions = listOf("Latest", "Oldest", "Most Liked", "Most Comments", "Most Views")
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = sortAdapter
        
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> { currentSort = "created_at"; currentSortOrder = "desc" }
                    1 -> { currentSort = "created_at"; currentSortOrder = "asc" }
                    2 -> { currentSort = "like_count"; currentSortOrder = "desc" }
                    3 -> { currentSort = "comment_count"; currentSortOrder = "desc" }
                    4 -> { currentSort = "view_count"; currentSortOrder = "desc" }
                }
                loadPosts()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadPosts() {
        swipeRefresh.isRefreshing = true
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val category = if (currentCategory == "All") null else currentCategory
                val result = forumManager.getPosts(
                    category = category,
                    sortBy = currentSort,
                    sortOrder = currentSortOrder,
                    limit = 50
                )
                
                if (result.isSuccess) {
                    var posts = result.getOrNull() ?: emptyList()
                    
                    // Apply search filter
                    if (searchQuery.isNotEmpty()) {
                        posts = posts.filter { post ->
                            post.title.contains(searchQuery, ignoreCase = true) ||
                            post.content.contains(searchQuery, ignoreCase = true) ||
                            post.authorName.contains(searchQuery, ignoreCase = true)
                        }
                    }
                    
                    adapter.updateItems(posts)
                    findViewById<android.widget.TextView>(R.id.emptyView).visibility = 
                        if (posts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                } else {
                    android.widget.Toast.makeText(this@ForumActivity, "Failed to load posts", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ForumActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showCreatePostDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_post_enhanced, null)
        val etTitle = dialogView.findViewById<android.widget.EditText>(R.id.etTitle)
        val etContent = dialogView.findViewById<android.widget.EditText>(R.id.etContent)
        val categorySpinner = dialogView.findViewById<android.widget.Spinner>(R.id.categorySpinner)
        val etTags = dialogView.findViewById<android.widget.EditText>(R.id.etTags)

        // Setup category spinner for dialog
        val categories = listOf("General", "Diseases", "Tips & Advice", "Harvest & Yield", "Equipment")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Create New Post")
            .setView(dialogView)
            .setPositiveButton(getString(R.string.post)) { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()
                val category = categories[categorySpinner.selectedItemPosition]
                val tags = etTags.text.toString().trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                
                if (title.isEmpty() || content.isEmpty()) {
                    android.widget.Toast.makeText(this, "Please fill in all required fields", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = forumManager.createPost(title, content, category, tags)
                        if (result.isSuccess) {
                            android.widget.Toast.makeText(this@ForumActivity, "Post created successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            loadPosts()
                        } else {
                            android.widget.Toast.makeText(this@ForumActivity, "Failed to create post: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(this@ForumActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.forum_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_moderation -> {
                if (isModerator()) {
                    val intent = Intent(this, ForumModerationActivity::class.java)
                    startActivity(intent)
                } else {
                    android.widget.Toast.makeText(this, "Moderator access required", android.widget.Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_notifications -> {
                val intent = Intent(this, ForumNotificationsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_bookmarks -> {
                val intent = Intent(this, ForumBookmarksActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
    
    private fun isModerator(): Boolean {
        // Check if current user is a moderator
        // This would typically check the user's role in Supabase
        return false // Placeholder - implement based on your user role system
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}