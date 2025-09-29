package com.example.bitterguardmobile

import android.content.Context
import android.content.SharedPreferences
import com.example.bitterguardmobile.models.ForumPost
import com.example.bitterguardmobile.models.ForumComment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class LocalForumManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("forum_data", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    // ==================== POSTS ====================
    
    suspend fun createPost(title: String, content: String, category: String = "General", tags: List<String> = emptyList()): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val postId = UUID.randomUUID().toString()
                val post = ForumPost(
                    id = postId,
                    author_uid = "local-user",
                    author_name = "Local User",
                    title = title,
                    content = content,
                    category = category,
                    tags = tags,
                    created_at = System.currentTimeMillis().toString(),
                    updated_at = System.currentTimeMillis().toString()
                )
                
                val posts = getStoredPosts().toMutableList()
                posts.add(post)
                savePosts(posts)
                
                Result.success(postId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getPosts(
        category: String? = null,
        sortBy: String = "created_at",
        sortOrder: String = "desc",
        limit: Int = 20
    ): Result<List<ForumPost>> {
        return withContext(Dispatchers.IO) {
            try {
                var posts = getStoredPosts()
                
                // Filter by category
                if (category != null && category != "All") {
                    posts = posts.filter { it.category == category }
                }
                
                // Sort
                posts = when (sortBy) {
                    "created_at" -> if (sortOrder == "desc") posts.sortedByDescending { it.createdAt } else posts.sortedBy { it.createdAt }
                    "title" -> if (sortOrder == "desc") posts.sortedByDescending { it.title } else posts.sortedBy { it.title }
                    else -> posts
                }
                
                // Limit
                posts = posts.take(limit)
                
                Result.success(posts)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getPost(postId: String): Result<ForumPost?> {
        return withContext(Dispatchers.IO) {
            try {
                val posts = getStoredPosts()
                val post = posts.find { it.id == postId }
                Result.success(post)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // ==================== COMMENTS ====================
    
    suspend fun addComment(postId: String, text: String, parentCommentId: String = ""): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val commentId = UUID.randomUUID().toString()
                val comment = ForumComment(
                    id = commentId,
                    post_id = postId,
                    parent_comment_id = parentCommentId,
                    author_uid = "local-user",
                    author_name = "Local User",
                    text = text,
                    created_at = System.currentTimeMillis().toString(),
                    updated_at = System.currentTimeMillis().toString(),
                    depth = if (parentCommentId.isEmpty()) 0 else 1
                )
                
                val comments = getStoredComments().toMutableList()
                comments.add(comment)
                saveComments(comments)
                
                Result.success(commentId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getComments(postId: String): Result<List<ForumComment>> {
        return withContext(Dispatchers.IO) {
            try {
                val comments = getStoredComments().filter { it.post_id == postId }
                Result.success(comments)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // ==================== LIKES & BOOKMARKS ====================
    
    suspend fun toggleLike(postId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val posts = getStoredPosts().toMutableList()
                val postIndex = posts.indexOfFirst { it.id == postId }
                if (postIndex != -1) {
                    val post = posts[postIndex]
                    val newLikeCount = if (isLiked(postId)) post.like_count - 1 else post.like_count + 1
                    posts[postIndex] = post.copy(like_count = newLikeCount)
                    savePosts(posts)
                    Result.success(!isLiked(postId))
                } else {
                    Result.failure(Exception("Post not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    
    fun isLiked(postId: String): Boolean {
        return prefs.getBoolean("liked_$postId", false)
    }
    
    
    
    
    // ==================== STORAGE HELPERS ====================
    
    private fun getStoredPosts(): List<ForumPost> {
        val postsJson = prefs.getString("posts", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<ForumPost>>(postsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun savePosts(posts: List<ForumPost>) {
        val postsJson = json.encodeToString(posts)
        prefs.edit().putString("posts", postsJson).apply()
    }
    
    private fun getStoredComments(): List<ForumComment> {
        val commentsJson = prefs.getString("comments", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<ForumComment>>(commentsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveComments(comments: List<ForumComment>) {
        val commentsJson = json.encodeToString(comments)
        prefs.edit().putString("comments", commentsJson).apply()
    }
    
    
    
}
