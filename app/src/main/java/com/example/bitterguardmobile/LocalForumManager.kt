package com.example.bitterguardmobile

import android.content.Context
import android.content.SharedPreferences
import com.example.bitterguardmobile.models.ForumPost
import com.example.bitterguardmobile.models.ForumComment
import com.example.bitterguardmobile.models.ForumNotification
import com.example.bitterguardmobile.models.ForumReport
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
    
    suspend fun toggleBookmark(postId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val bookmarks = getStoredBookmarks().toMutableSet()
                val isBookmarked = bookmarks.contains(postId)
                if (isBookmarked) {
                    bookmarks.remove(postId)
                } else {
                    bookmarks.add(postId)
                }
                saveBookmarks(bookmarks)
                Result.success(!isBookmarked)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    fun isLiked(postId: String): Boolean {
        return prefs.getBoolean("liked_$postId", false)
    }
    
    fun isBookmarked(postId: String): Boolean {
        return getStoredBookmarks().contains(postId)
    }
    
    // ==================== BOOKMARKS ====================
    
    suspend fun getBookmarks(): Result<List<ForumPost>> {
        return withContext(Dispatchers.IO) {
            try {
                val bookmarkedIds = getStoredBookmarks()
                val posts = getStoredPosts().filter { bookmarkedIds.contains(it.id) }
                Result.success(posts)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // ==================== NOTIFICATIONS ====================
    
    suspend fun getNotifications(): Result<List<ForumNotification>> {
        return withContext(Dispatchers.IO) {
            try {
                val notifications = getStoredNotifications()
                Result.success(notifications)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // ==================== REPORTS ====================
    
    suspend fun getReports(): Result<List<ForumReport>> {
        return withContext(Dispatchers.IO) {
            try {
                val reports = getStoredReports()
                Result.success(reports)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun resolveReport(reportId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val reports: MutableList<ForumReport> = getStoredReports().toMutableList()
                val reportIndex = reports.indexOfFirst { it.id == reportId }
                if (reportIndex != -1) {
                    reports.removeAt(reportIndex)
                    saveReports(reports)
                    Result.success("Report resolved")
                } else {
                    Result.failure(Exception("Report not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun reportContent(contentId: String, contentType: String, reason: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val reportId = UUID.randomUUID().toString()
                val report = ForumReport(
                    id = reportId,
                    reporterUid = "local-user",
                    reportedContentType = contentType,
                    reportedContentId = contentId,
                    reason = reason,
                    description = reason,
                    createdAt = System.currentTimeMillis(),
                    status = "pending"
                )
                
                val reports: MutableList<ForumReport> = getStoredReports().toMutableList()
                reports.add(report)
                saveReports(reports)
                
                Result.success(reportId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
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
    
    private fun getStoredBookmarks(): Set<String> {
        val bookmarksJson = prefs.getString("bookmarks", "[]") ?: "[]"
        return try {
            json.decodeFromString<Set<String>>(bookmarksJson)
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    private fun saveBookmarks(bookmarks: Set<String>) {
        val bookmarksJson = json.encodeToString(bookmarks)
        prefs.edit().putString("bookmarks", bookmarksJson).apply()
    }
    
    private fun getStoredNotifications(): List<ForumNotification> {
        val notificationsJson = prefs.getString("notifications", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<ForumNotification>>(notificationsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveNotifications(notifications: List<ForumNotification>) {
        val notificationsJson = json.encodeToString(notifications)
        prefs.edit().putString("notifications", notificationsJson).apply()
    }
    
    private fun getStoredReports(): List<ForumReport> {
        val reportsJson = prefs.getString("reports", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<ForumReport>>(reportsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveReports(reports: List<ForumReport>) {
        val reportsJson = json.encodeToString(reports)
        prefs.edit().putString("reports", reportsJson).apply()
    }
}
