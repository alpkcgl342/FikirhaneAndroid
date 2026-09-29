package com.alpkcgl.fikirhane.data.repository

import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.api.FikirhaneApi
import com.alpkcgl.fikirhane.data.model.AdminReport
import com.alpkcgl.fikirhane.data.model.AdminUser
import com.alpkcgl.fikirhane.data.model.FollowResult
import com.alpkcgl.fikirhane.data.model.NotificationsPage
import com.alpkcgl.fikirhane.data.model.Page
import com.alpkcgl.fikirhane.data.model.Profile
import com.alpkcgl.fikirhane.data.model.ProfileInput
import com.alpkcgl.fikirhane.data.model.PublicUser
import com.alpkcgl.fikirhane.data.model.ReportInput
import com.alpkcgl.fikirhane.data.model.ReportStatus
import com.alpkcgl.fikirhane.data.model.ReportTargetType
import com.alpkcgl.fikirhane.data.model.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Profil ve takip. */
class UsersRepository(
    private val api: FikirhaneApi,
    private val auth: AuthRepository,
) {
    suspend fun profile(username: String): Profile =
        auth.authorized { token -> api.getProfile(token, username) }

    suspend fun toggleFollow(userId: String): FollowResult =
        auth.authorized { token -> api.toggleFollow(token, userId) }

    /** Profili günceller; oturumdaki kullanıcı bilgisi de tazelenir. */
    suspend fun updateProfile(displayName: String, bio: String, avatarUrl: String?): PublicUser {
        val user = auth.authorized { token ->
            api.updateProfile(token, ProfileInput(displayName.trim(), bio.trim(), avatarUrl)).user
        }
        auth.updateCurrentUser(user)
        return user
    }
}

/** Bildirimler; okunmamış sayısı üst çubuktaki rozette gösterilir. */
class NotificationsRepository(
    private val api: FikirhaneApi,
    private val auth: AuthRepository,
) {
    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread.asStateFlow()

    suspend fun refreshUnread() {
        try {
            _unread.value = auth.authorized { token -> api.unreadNotificationCount(token) }
        } catch (_: ApiException) {
            // Rozet yalnızca bilgi amaçlı; hata ekranı bozmaz.
        }
    }

    suspend fun list(page: Int): NotificationsPage =
        auth.authorized { token -> api.listNotifications(token, page) }

    suspend fun markAllRead() {
        auth.authorized { token -> api.markAllNotificationsRead(token) }
        _unread.value = 0
    }
}

/** Şikâyet ve moderasyon (yönetim uç noktaları sunucuda MODERATOR/ADMIN ister). */
class ModerationRepository(
    private val api: FikirhaneApi,
    private val auth: AuthRepository,
    private val posts: PostsRepository,
) {
    suspend fun report(type: ReportTargetType, targetId: String, reason: String) =
        auth.authorized { token -> api.createReport(token, ReportInput(type, targetId, reason.trim())) }

    suspend fun reports(status: ReportStatus, page: Int): Page<AdminReport> =
        auth.authorized { token -> api.listReports(token, status, page) }

    suspend fun resolve(reportId: String, status: ReportStatus) =
        auth.authorized { token -> api.resolveReport(token, reportId, status) }

    suspend fun removePost(postId: String) {
        auth.authorized { token -> api.removePost(token, postId) }
        posts.notifyChanged()
    }

    suspend fun removeComment(commentId: String) =
        auth.authorized { token -> api.removeComment(token, commentId) }

    suspend fun users(q: String?, page: Int): Page<AdminUser> =
        auth.authorized { token -> api.listUsers(token, q, page) }

    suspend fun setBan(userId: String, banned: Boolean) =
        auth.authorized { token -> api.setBan(token, userId, banned) }

    suspend fun setRole(userId: String, role: Role) =
        auth.authorized { token -> api.setRole(token, userId, role) }
}
