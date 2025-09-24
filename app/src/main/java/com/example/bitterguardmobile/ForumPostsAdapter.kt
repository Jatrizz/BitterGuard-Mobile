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
    private val onClick: (ForumPost) -> Unit
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
        holder.tvTime.text = android.text.format.DateUtils.getRelativeTimeSpanString(post.createdAt)
        holder.tvComments.text = "${post.commentCount}"
        holder.tvLikes.text = "${post.likeCount}"
        holder.tvViews.text = "${post.viewCount}"
        
        // Thumbnail (basic public URL handling)
        holder.ivThumb.visibility = View.GONE
        holder.ivThumb.visibility = View.GONE
        val urlField = post.attachments.firstOrNull()
        if (!urlField.isNullOrBlank()) {
            if (urlField.startsWith("file:") || urlField.startsWith("content:")) {
                holder.ivThumb.visibility = View.VISIBLE
                holder.ivThumb.setImageURI(android.net.Uri.parse(urlField))
            } else if (urlField.startsWith("http")) {
                holder.ivThumb.visibility = View.VISIBLE
                // Use simple WebView to render public image if Bitmap fetch fails (no 3rd party libs)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val bmp = android.graphics.BitmapFactory.decodeStream(java.net.URL(urlField).openStream())
                        withContext(Dispatchers.Main) { holder.ivThumb.setImageBitmap(bmp) }
                    } catch (_: Exception) {
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
        
        // Like button
        holder.btnLike.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = supabaseForum.toggleLikePost(post.id)
                    if (result.isSuccess) {
                        val isLiked = result.getOrNull() ?: false
                        holder.btnLike.setImageResource(
                            if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
                        )
                        // Update like count
                        val base = holder.tvLikes.text.toString().toIntOrNull() ?: 0
                        holder.tvLikes.text = "${base + if (isLiked) 1 else -1}"
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(holder.itemView.context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Bookmark button
        holder.btnBookmark.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = supabaseForum.toggleBookmarkPost(post.id)
                    if (result.isSuccess) {
                        val isBookmarked = result.getOrNull() ?: false
                        holder.btnBookmark.setImageResource(
                            if (isBookmarked) android.R.drawable.ic_input_add else android.R.drawable.ic_input_add
                        )
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(holder.itemView.context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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
                // Check if liked
                val isLiked = forumManager.isLiked(post.id)
                holder.btnLike.setImageResource(
                    if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
                )
                
                // Check if bookmarked
                val isBookmarked = forumManager.isBookmarked(post.id)
                holder.btnBookmark.setImageResource(
                    if (isBookmarked) android.R.drawable.ic_input_add else android.R.drawable.ic_input_add
                )
            } catch (e: Exception) {
                // Handle silently
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
}


