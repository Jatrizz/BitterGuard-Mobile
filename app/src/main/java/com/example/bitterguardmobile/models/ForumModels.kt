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
    val is_reported: Boolean = false,
    val reported_count: Int = 0,
    val attachments: List<String> = emptyList()
) : Parcelable {
    // Convenience properties for backward compatibility
    val authorUid: String get() = author_uid
    val authorName: String get() = author_name
    val createdAt: Long get() = created_at.toLongOrNull() ?: System.currentTimeMillis()
    val updatedAt: Long get() = updated_at.toLongOrNull() ?: System.currentTimeMillis()
    val commentCount: Int get() = comment_count
    val likeCount: Int get() = like_count
    val viewCount: Int get() = view_count
    val isPinned: Boolean get() = is_pinned
    val isLocked: Boolean get() = is_locked
    val isReported: Boolean get() = is_reported
    val reportedCount: Int get() = reported_count
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
    val is_reported: Boolean = false,
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
    val isReported: Boolean get() = is_reported
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
data class ForumReport(
    val id: String = "",
    val reporterUid: String = "",
    val reportedContentType: String = "", // "post" or "comment"
    val reportedContentId: String = "",
    val reason: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "pending", // "pending", "reviewed", "resolved", "dismissed"
    val moderatorUid: String = "",
    val moderatorNotes: String = "",
    val resolvedAt: Long = 0L
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
data class ForumBookmark(
    val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class ForumLike(
    val id: String = "",
    val userId: String = "",
    val contentType: String = "", // "post" or "comment"
    val contentId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class ForumNotification(
    val id: String = "",
    val userId: String = "",
    val type: String = "", // "comment", "like", "mention", "reply"
    val title: String = "",
    val message: String = "",
    val relatedPostId: String = "",
    val relatedCommentId: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) : Parcelable
