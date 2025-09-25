package com.example.bitterguardmobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bitterguardmobile.models.ForumPost
import kotlinx.coroutines.*

class ForumPostsAdapter(
    private var posts: MutableList<ForumPost>,
    private val forumManager: LocalForumManager,
    private val onClick: (ForumPost) -> Unit,
    private val onBookmarkClick: ((ForumPost) -> Unit)? = null
) : RecyclerView.Adapter<ForumPostsAdapter.PostViewHolder>() {

    private val supabaseForum by lazy { SupabaseForumService(itemViewContext) }
    private lateinit var itemViewContext: android.content.Context

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        val tvComments: TextView = itemView.findViewById(R.id.tvComments)
        val tvLikes: TextView = itemView.findViewById(R.id.tvLikes)
        val tvViews: TextView = itemView.findViewById(R.id.tvViews)
        val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvTags: TextView = itemView.findViewById(R.id.tvTags)
        val ivThumb: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        val btnBookmark: ImageView = itemView.findViewById(R.id.btnBookmark)
        val btnReport: ImageView = itemView.findViewById(R.id.btnReport)
        val ivPinned: ImageView = itemView.findViewById(R.id.ivPinned)
        val ivLocked: ImageView = itemView.findViewById(R.id.ivLocked)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forum_post_enhanced, parent, false)
        itemViewContext = parent.context
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        
        // Basic content
        holder.tvTitle.text = post.title
        holder.tvContent.text = post.content
        holder.tvAuthor.text = post.authorName
        holder.tvTime.text = getRelativeTimeString(post.createdAt)
        holder.tvComments.text = "${post.commentCount}"
        holder.tvLikes.text = "${post.likeCount}"
        holder.tvViews.text = "${post.viewCount}"
        
        // Thumbnail (basic public URL handling)
        holder.ivThumb.visibility = View.GONE
        val urlField = post.attachments.firstOrNull()
        if (!urlField.isNullOrBlank()) {
            if (urlField.startsWith("file:") || urlField.startsWith("content:")) {
                holder.ivThumb.visibility = View.VISIBLE
                holder.ivThumb.setImageURI(android.net.Uri.parse(urlField))
                holder.ivThumb.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            } else if (urlField.startsWith("http")) {
                holder.ivThumb.visibility = View.VISIBLE
                // Load image from URL with proper scaling
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val bmp = android.graphics.BitmapFactory.decodeStream(java.net.URL(urlField).openStream())
                        withContext(Dispatchers.Main) { 
                            holder.ivThumb.setImageBitmap(bmp)
                            holder.ivThumb.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                            holder.ivThumb.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ForumPostsAdapter", "Failed to load image: ${e.message}")
                        withContext(Dispatchers.Main) { holder.ivThumb.visibility = View.GONE }
                    }
                }
            }
        }

        // Category and tags
        holder.tvCategory.text = post.category
        if (post.tags.isNotEmpty()) {
            holder.tvTags.text = post.tags.joinToString(", ")
            holder.tvTags.visibility = View.VISIBLE
        } else {
            holder.tvTags.visibility = View.GONE
        }
        
        // Status indicators
        holder.ivPinned.visibility = if (post.isPinned) View.VISIBLE else View.GONE
        holder.ivLocked.visibility = if (post.isLocked) View.VISIBLE else View.GONE
        
        // Like button - make it easier to click and same size as other icons
        holder.btnLike.setPadding(0, 0, 0, 0)
        holder.btnLike.minimumWidth = 64
        holder.btnLike.minimumHeight = 64
        holder.btnLike.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = supabaseForum.toggleLikePost(post.id)
                    if (result.isSuccess) {
                        val justLiked = result.getOrNull() ?: false
                        holder.btnLike.setImageResource(
                            if (justLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
                        )
                        // Add color tinting
                        holder.btnLike.setColorFilter(
                            if (justLiked) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#9E9E9E")
                        )
                        
                        // Refresh the actual counts from the server instead of manual calculation
                        refreshPostCounts(post.id)
                    } else {
                        android.widget.Toast.makeText(holder.itemView.context, "Failed to toggle like", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(holder.itemView.context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Bookmark button - make it easier to click and same size as other icons
        holder.btnBookmark.setPadding(0, 0, 0, 0)
        holder.btnBookmark.minimumWidth = 64
        holder.btnBookmark.minimumHeight = 64
        holder.btnBookmark.setOnClickListener {
            if (onBookmarkClick != null) {
                // Use custom bookmark handler
                onBookmarkClick(post)
            } else {
                // Use default bookmark handler
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = supabaseForum.toggleBookmarkPost(post.id)
                        if (result.isSuccess) {
                            val isBookmarked = result.getOrNull() ?: false
                            holder.btnBookmark.setImageResource(
                                if (isBookmarked) android.R.drawable.ic_input_add else android.R.drawable.ic_input_add
                            )
                            // Add color tinting to show bookmark state
                            holder.btnBookmark.setColorFilter(
                                if (isBookmarked) android.graphics.Color.parseColor("#FF9800") else android.graphics.Color.parseColor("#9E9E9E")
                            )
                            android.widget.Toast.makeText(holder.itemView.context, 
                                if (isBookmarked) "Bookmarked!" else "Removed from bookmarks", 
                                android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(holder.itemView.context, "Failed to toggle bookmark", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(holder.itemView.context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        // Report / Edit / Delete via long press menu
        holder.btnReport.setOnClickListener { showReportDialog(holder.itemView.context, post) }
        holder.itemView.setOnLongClickListener {
            val items = arrayOf("Edit", "Delete", "Report")
            androidx.appcompat.app.AlertDialog.Builder(holder.itemView.context)
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> showEditPostDialog(holder.itemView.context, post)
                        1 -> confirmDeletePost(holder.itemView.context, post)
                        2 -> showReportDialog(holder.itemView.context, post)
                    }
                }.show()
            true
        }
        
        // Main click listener
        holder.itemView.setOnClickListener { onClick(post) }
        
        // Load initial states
        loadInitialStates(holder, post)
    }
    
    private fun loadInitialStates(holder: PostViewHolder, post: ForumPost) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Check if liked from server
                val isLikedResult = supabaseForum.isPostLiked(post.id)
                val isLiked = if (isLikedResult.isSuccess) isLikedResult.getOrNull() ?: false else false
                holder.btnLike.setImageResource(
                    if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
                )
                // Add color tinting
                holder.btnLike.setColorFilter(
                    if (isLiked) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#9E9E9E")
                )
                
                // Check if bookmarked from server
                val isBookmarkedResult = supabaseForum.isPostBookmarked(post.id)
                val isBookmarked = if (isBookmarkedResult.isSuccess) isBookmarkedResult.getOrNull() ?: false else false
                holder.btnBookmark.setImageResource(
                    if (isBookmarked) android.R.drawable.ic_input_add else android.R.drawable.ic_input_add
                )
                // Add color tinting to show bookmark state
                holder.btnBookmark.setColorFilter(
                    if (isBookmarked) android.graphics.Color.parseColor("#FF9800") else android.graphics.Color.parseColor("#9E9E9E")
                )
            } catch (e: Exception) {
                // Handle silently - set default states
                holder.btnLike.setImageResource(android.R.drawable.btn_star_big_off)
                holder.btnLike.setColorFilter(android.graphics.Color.parseColor("#9E9E9E"))
                holder.btnBookmark.setImageResource(android.R.drawable.ic_input_add)
                holder.btnBookmark.setColorFilter(android.graphics.Color.parseColor("#9E9E9E"))
            }
        }
    }
    
    private fun showReportDialog(context: android.content.Context, post: ForumPost) {
        val reasons = arrayOf("Spam", "Inappropriate Content", "Off-topic", "Harassment", "Other")
        
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Report Post")
            .setItems(reasons) { _, which ->
                val reason = reasons[which]
                showReportDescriptionDialog(context, post, reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showReportDescriptionDialog(context: android.content.Context, post: ForumPost, reason: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_report_description, null)
        val etDescription = dialogView.findViewById<android.widget.EditText>(R.id.etDescription)
        
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Report: $reason")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val description = etDescription.text.toString().trim()
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = forumManager.reportContent(post.id, "post", reason)
                        if (result.isSuccess) {
                            android.widget.Toast.makeText(context, "Report submitted successfully", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Failed to submit report", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditPostDialog(context: android.content.Context, post: ForumPost) {
        val dialog = android.widget.LinearLayout(context).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(32,16,32,0) }
        val etTitle = android.widget.EditText(context).apply { setText(post.title) }
        val etBody = android.widget.EditText(context).apply { setText(post.content) }
        dialog.addView(etTitle); dialog.addView(etBody)

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Edit Post")
            .setView(dialog)
            .setPositiveButton("Save") { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    val service = SupabaseForumService(context)
                    val res = service.editPost(post.id, etTitle.text.toString().trim(), etBody.text.toString().trim())
                    if (res.isSuccess) {
                        val idx = adapterPositionSafe(post)
                        if (idx >= 0) {
                            val updated = post.copy(
                                title = etTitle.text.toString().trim(),
                                content = etBody.text.toString().trim()
                            )
                            posts[idx] = updated
                            notifyItemChanged(idx)
                        }
                    } else android.widget.Toast.makeText(context, "Edit failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun confirmDeletePost(context: android.content.Context, post: ForumPost) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    val service = SupabaseForumService(context)
                    val res = service.deletePost(post.id)
                    if (res.isSuccess) {
                        val idx = adapterPositionSafe(post)
                        if (idx >= 0) { posts.removeAt(idx); notifyItemRemoved(idx) }
                    } else android.widget.Toast.makeText(context, "Delete failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun adapterPositionSafe(post: ForumPost): Int = posts.indexOfFirst { it.id == post.id }

    fun updateItems(newItems: List<ForumPost>) {
        posts.clear()
        posts.addAll(newItems)
        notifyDataSetChanged()
    }

    fun appendItems(moreItems: List<ForumPost>) {
        val start = posts.size
        posts.addAll(moreItems)
        notifyItemRangeInserted(start, moreItems.size)
    }
    
    fun updatePostCounts(postId: String, likeCount: Int, commentCount: Int, viewCount: Int) {
        val index = posts.indexOfFirst { it.id == postId }
        if (index >= 0) {
            val currentPost = posts[index]
            val updatedPost = currentPost.copy(
                like_count = likeCount,
                comment_count = commentCount,
                view_count = viewCount
            )
            posts[index] = updatedPost
            notifyItemChanged(index)
        }
    }
    
    suspend fun refreshPostCounts(postId: String) {
        try {
            val result = supabaseForum.getPostCounts(postId)
            if (result.isSuccess) {
                val (likeCount, commentCount, viewCount) = result.getOrNull()!!
                updatePostCounts(postId, likeCount, commentCount, viewCount)
            }
        } catch (e: Exception) {
            android.util.Log.e("ForumPostsAdapter", "Failed to refresh counts for post $postId: ${e.message}")
        }
    }
    
    private fun getRelativeTimeString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff <= 15000 -> "Just now"
            diff < 60000 -> "${diff / 1000}s"
            diff < 3600000 -> "${diff / 60000}m"
            diff < 86400000 -> "${diff / 3600000}h"
            diff < 604800000 -> "${diff / 86400000}d"
            diff < 2592000000L -> "${diff / 604800000}w"
            diff < 31536000000L -> "${diff / 2592000000L}mo"
            else -> "${diff / 31536000000L}y"
        }
    }
    
    private fun scaleImageToFit(bitmap: android.graphics.Bitmap, targetWidth: Int, targetHeight: Int): android.graphics.Bitmap {
        if (targetWidth <= 0 || targetHeight <= 0) return bitmap
        
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        
        // Calculate scale factor to fit the image within the target dimensions
        val scaleX = targetWidth.toFloat() / originalWidth
        val scaleY = targetHeight.toFloat() / originalHeight
        val scale = minOf(scaleX, scaleY)
        
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        
        return android.graphics.Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }
    
}


