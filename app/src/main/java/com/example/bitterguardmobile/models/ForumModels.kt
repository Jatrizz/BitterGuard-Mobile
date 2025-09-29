package com.example.bitterguardmobile.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Parcelize
data class ForumPost(
    val id: String = "",
    val author_uid: String = "",
    val author_name: String = "",
    val title: String = "",
    val content: String = "",
    val created_at: String = "",
    val updated_at: String = "",
    val comment_count: Int = 0,
    val like_count: Int = 0,
    val view_count: Int = 0,
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val is_pinned: Boolean = false,
    val is_locked: Boolean = false,
    val attachments: List<String> = emptyList()
) : Parcelable {
    // Convenience properties for backward compatibility
    val authorUid: String get() = author_uid
    val authorName: String get() = author_name
    val createdAt: Long get() = parseTimestamp(created_at)
    val updatedAt: Long get() = parseTimestamp(updated_at)
    val commentCount: Int get() = comment_count
    val likeCount: Int get() = like_count
    val viewCount: Int get() = view_count
    val isPinned: Boolean get() = is_pinned
    val isLocked: Boolean get() = is_locked
    
    private fun parseTimestamp(timestamp: String): Long {
        if (timestamp.isEmpty()) return System.currentTimeMillis()
        
        // Try to parse as Long first (for backwards compatibility)
        timestamp.toLongOrNull()?.let { return it }
        
        // Try to parse as ISO 8601 timestamp with timezone offset (Supabase returns +00:00)
        try {
            val odt = java.time.OffsetDateTime.parse(timestamp, java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val result = odt.toInstant().toEpochMilli()
            android.util.Log.d("ForumModels", "Parsed timestamp '$timestamp' via ISO_OFFSET_DATE_TIME to $result")
            return result
        } catch (e: Exception) {
            android.util.Log.d("ForumModels", "Failed ISO_OFFSET_DATE_TIME parse for '$timestamp': ${e.message}")
            // Try Instant (supports Z-suffixed instants)
            try {
                val instant = java.time.Instant.parse(timestamp)
                val result = instant.toEpochMilli()
                android.util.Log.d("ForumModels", "Parsed timestamp '$timestamp' via Instant to $result")
                return result
            } catch (e2: Exception) {
                android.util.Log.d("ForumModels", "Failed Instant parse for '$timestamp': ${e2.message}")
            }
            // Try other common formats without timezone (fallback)
            try {
                val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", java.util.Locale.getDefault())
                val result = formatter.parse(timestamp)?.time ?: System.currentTimeMillis()
                android.util.Log.d("ForumModels", "Parsed timestamp '$timestamp' via pattern SSSSSSXXX to $result")
                return result
            } catch (e3: Exception) {
                android.util.Log.d("ForumModels", "Failed to parse timestamp '$timestamp': ${e3.message}, using current time")
                return System.currentTimeMillis()
            }
        }
    }
}

@Serializable
@Parcelize
data class ForumComment(
    val id: String = "",
    val post_id: String = "",
    val parent_comment_id: String = "", // For threaded comments
    val author_uid: String = "",
    val author_name: String = "",
    val text: String = "",
    val created_at: String = "",
    val updated_at: String = "",
    val like_count: Int = 0,
    val is_edited: Boolean = false,
    val depth: Int = 0, // For comment threading
    val replies: List<ForumComment> = emptyList()
) : Parcelable {
    // Convenience properties for backward compatibility
    val postId: String get() = post_id
    val parentCommentId: String get() = parent_comment_id
    val authorUid: String get() = author_uid
    val authorName: String get() = author_name
    val createdAt: Long get() = created_at.toLongOrNull() ?: System.currentTimeMillis()
    val updatedAt: Long get() = updated_at.toLongOrNull() ?: System.currentTimeMillis()
    val likeCount: Int get() = like_count
    val isEdited: Boolean get() = is_edited
}

@Parcelize
data class ForumCategory(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val color: String = "#4CAF50",
    val postCount: Int = 0,
    val isActive: Boolean = true,
    val order: Int = 0
) : Parcelable


@Parcelize
data class ForumUser(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatar: String = "",
    val reputation: Int = 0,
    val postCount: Int = 0,
    val commentCount: Int = 0,
    val joinDate: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    val isModerator: Boolean = false,
    val isBanned: Boolean = false,
    val banReason: String = "",
    val preferences: ForumUserPreferences = ForumUserPreferences()
) : Parcelable

@Parcelize
data class ForumUserPreferences(
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val allowDirectMessages: Boolean = true,
    val signature: String = "",
    val timezone: String = "UTC"
) : Parcelable

@Parcelize
data class ForumLike(
    val id: String = "",
    val userId: String = "",
    val contentType: String = "", // "post" or "comment"
    val contentId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
