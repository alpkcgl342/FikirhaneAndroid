package com.alpkcgl.fikirhane.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Beğeni / kaydetme / takip

@Serializable
data class LikeResult(val liked: Boolean, val likeCount: Int)

@Serializable
data class BookmarkResult(val bookmarked: Boolean)

@Serializable
data class FollowResult(val following: Boolean, val followerCount: Int)

// Yorumlar

@Serializable
data class CommentAuthor(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
)

@Serializable
data class Comment(
    val id: String,
    val parentId: String? = null,
    val content: String,
    val createdAt: String,
    val author: CommentAuthor,
)

@Serializable
data class CommentsResponse(val items: List<Comment>)

@Serializable
data class CommentInput(val content: String, val parentId: String? = null)

// Profil

@Serializable
data class Profile(
    val id: String,
    val username: String,
    val displayName: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val isBanned: Boolean = false,
    val createdAt: String,
    val postCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isMe: Boolean = false,
    val isFollowing: Boolean = false,
)

@Serializable
data class ProfileInput(val displayName: String, val bio: String?, val avatarUrl: String?)

// Arama

@Serializable
data class SearchUser(
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
)

@Serializable
data class SearchResponse(
    val query: String = "",
    val items: List<PostSummary>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
    val users: List<SearchUser> = emptyList(),
)

// Bildirimler

@Serializable
enum class NotificationType { COMMENT, LIKE, FOLLOW }

@Serializable
data class NotificationActor(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
)

@Serializable
data class NotificationPost(val id: String, val slug: String, val title: String)

@Serializable
data class NotificationData(
    val actor: NotificationActor,
    val post: NotificationPost? = null,
    val commentId: String? = null,
    /** COMMENT: true ise yorumuna yanıt, değilse yazına yorum. */
    val reply: Boolean = false,
)

@Serializable
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val data: NotificationData,
    val isRead: Boolean,
    val createdAt: String,
)

@Serializable
data class NotificationsPage(
    val items: List<AppNotification>,
    val unreadCount: Int,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
)

@Serializable
data class UnreadCount(val unreadCount: Int)

// Şikâyet ve yönetim

@Serializable
enum class ReportTargetType { POST, COMMENT, USER }

@Serializable
enum class ReportStatus { PENDING, RESOLVED, DISMISSED }

@Serializable
data class ReportInput(val targetType: ReportTargetType, val targetId: String, val reason: String)

@Serializable
data class Person(val id: String, val username: String, val displayName: String)

/**
 * Yönetim listesindeki şikâyet. [target] hedef türüne göre farklı biçimdedir
 * (yazı: slug/title/author, yorum: content/post/author, kullanıcı: username/isBanned);
 * içerik silinmişse null.
 */
@Serializable
data class AdminReport(
    val id: String,
    val targetType: ReportTargetType,
    val targetId: String,
    val reason: String,
    val status: ReportStatus,
    val createdAt: String,
    val reporter: Person,
    val target: JsonObject? = null,
)

@Serializable
data class AdminUser(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
    val role: Role,
    val isBanned: Boolean,
    val createdAt: String,
)

@Serializable
data class StatusInput(val status: ReportStatus)

@Serializable
data class BanInput(val banned: Boolean)

@Serializable
data class RoleInput(val role: Role)
