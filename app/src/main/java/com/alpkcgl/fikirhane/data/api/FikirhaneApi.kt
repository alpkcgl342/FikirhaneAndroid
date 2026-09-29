package com.alpkcgl.fikirhane.data.api

import com.alpkcgl.fikirhane.data.model.AdminReport
import com.alpkcgl.fikirhane.data.model.AdminUser
import com.alpkcgl.fikirhane.data.model.BanInput
import com.alpkcgl.fikirhane.data.model.BookmarkResult
import com.alpkcgl.fikirhane.data.model.CategoriesResponse
import com.alpkcgl.fikirhane.data.model.Comment
import com.alpkcgl.fikirhane.data.model.CommentInput
import com.alpkcgl.fikirhane.data.model.CommentsResponse
import com.alpkcgl.fikirhane.data.model.FollowResult
import com.alpkcgl.fikirhane.data.model.LikeResult
import com.alpkcgl.fikirhane.data.model.NotificationsPage
import com.alpkcgl.fikirhane.data.model.Profile
import com.alpkcgl.fikirhane.data.model.ProfileInput
import com.alpkcgl.fikirhane.data.model.ReportInput
import com.alpkcgl.fikirhane.data.model.ReportStatus
import com.alpkcgl.fikirhane.data.model.Role
import com.alpkcgl.fikirhane.data.model.RoleInput
import com.alpkcgl.fikirhane.data.model.SearchResponse
import com.alpkcgl.fikirhane.data.model.StatusInput
import com.alpkcgl.fikirhane.data.model.UnreadCount
import com.alpkcgl.fikirhane.data.model.Category
import com.alpkcgl.fikirhane.data.model.EmailRequest
import com.alpkcgl.fikirhane.data.model.Page
import com.alpkcgl.fikirhane.data.model.PostDetail
import com.alpkcgl.fikirhane.data.model.PostInput
import com.alpkcgl.fikirhane.data.model.PostQuery
import com.alpkcgl.fikirhane.data.model.PostSummary
import com.alpkcgl.fikirhane.data.model.UploadResponse
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLPathPart
import com.alpkcgl.fikirhane.data.model.LoginRequest
import com.alpkcgl.fikirhane.data.model.LoginResponse
import com.alpkcgl.fikirhane.data.model.MessageResponse
import com.alpkcgl.fikirhane.data.model.RefreshRequest
import com.alpkcgl.fikirhane.data.model.RefreshResponse
import com.alpkcgl.fikirhane.data.model.RegisterRequest
import com.alpkcgl.fikirhane.data.model.RegisterResponse
import com.alpkcgl.fikirhane.data.model.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import java.io.IOException

/** Fikirhane REST API'si (web sitesiyle aynı NestJS sunucusu). Tüm yollar `/api` altındadır. */
class FikirhaneApi(private val http: HttpClient) {

    suspend fun login(email: String, password: String): LoginResponse =
        call { http.post("auth/login") { json(LoginRequest(email, password)) } }

    suspend fun register(request: RegisterRequest): RegisterResponse =
        call { http.post("auth/register") { json(request) } }

    suspend fun refresh(refreshToken: String): RefreshResponse =
        call { http.post("auth/refresh") { json(RefreshRequest(refreshToken)) } }

    suspend fun me(accessToken: String): UserResponse =
        call { http.get("auth/me") { bearerAuth(accessToken) } }

    suspend fun resendConfirmation(email: String): MessageResponse =
        call { http.post("auth/resend-confirmation") { json(EmailRequest(email)) } }

    // Yazılar

    suspend fun listPosts(accessToken: String, query: PostQuery, page: Int, limit: Int = 10): Page<PostSummary> =
        call {
            http.get("posts") {
                bearerAuth(accessToken)
                parameter("page", page)
                parameter("limit", limit)
                parameter("status", query.status)
                parameter("author", query.author)
                parameter("category", query.category)
                parameter("tag", query.tag)
                parameter("sort", query.sort)
                if (query.following) parameter("following", true)
                if (query.bookmarked) parameter("bookmarked", true)
            }
        }

    suspend fun getPost(accessToken: String, slug: String): PostDetail =
        call { http.get("posts/${slug.encodeURLPathPart()}") { bearerAuth(accessToken) } }

    suspend fun createPost(accessToken: String, input: PostInput): PostDetail =
        call { http.post("posts") { bearerAuth(accessToken); json(input) } }

    suspend fun updatePost(accessToken: String, id: String, input: PostInput): PostDetail =
        call { http.patch("posts/$id") { bearerAuth(accessToken); json(input) } }

    suspend fun deletePost(accessToken: String, id: String): Unit =
        call { http.delete("posts/$id") { bearerAuth(accessToken) } }

    suspend fun listCategories(): List<Category> =
        call<CategoriesResponse> { http.get("categories") }.items

    /** Görseli yükler, herkese açık adresini döner (sunucu en fazla 4 MB kabul eder). */
    suspend fun uploadImage(accessToken: String, bytes: ByteArray, mimeType: String, fileName: String): String =
        call<UploadResponse> {
            http.submitFormWithBinaryData(
                url = "uploads/image",
                formData = formData {
                    append(
                        "file",
                        bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        },
                    )
                },
            ) { bearerAuth(accessToken) }
        }.url

    // Beğeni, kaydetme, yorumlar

    suspend fun toggleLike(accessToken: String, postId: String): LikeResult =
        call { http.post("posts/$postId/like") { bearerAuth(accessToken) } }

    suspend fun toggleBookmark(accessToken: String, postId: String): BookmarkResult =
        call { http.post("posts/$postId/bookmark") { bearerAuth(accessToken) } }

    suspend fun listComments(postId: String): List<Comment> =
        call<CommentsResponse> { http.get("posts/$postId/comments") }.items

    suspend fun createComment(accessToken: String, postId: String, input: CommentInput): Comment =
        call { http.post("posts/$postId/comments") { bearerAuth(accessToken); json(input) } }

    // Profil ve takip

    suspend fun getProfile(accessToken: String, username: String): Profile =
        call { http.get("users/${username.encodeURLPathPart()}") { bearerAuth(accessToken) } }

    suspend fun updateProfile(accessToken: String, input: ProfileInput): UserResponse =
        call { http.patch("users/me") { bearerAuth(accessToken); json(input) } }

    suspend fun toggleFollow(accessToken: String, userId: String): FollowResult =
        call { http.post("users/$userId/follow") { bearerAuth(accessToken) } }

    // Arama

    suspend fun search(q: String, page: Int): SearchResponse =
        call { http.get("search") { parameter("q", q); parameter("page", page) } }

    // Bildirimler

    suspend fun listNotifications(accessToken: String, page: Int): NotificationsPage =
        call { http.get("notifications") { bearerAuth(accessToken); parameter("page", page) } }

    suspend fun unreadNotificationCount(accessToken: String): Int =
        call<UnreadCount> { http.get("notifications/unread-count") { bearerAuth(accessToken) } }.unreadCount

    suspend fun markAllNotificationsRead(accessToken: String) {
        call<JsonObject> { http.patch("notifications/read-all") { bearerAuth(accessToken) } }
    }

    // Şikâyet ve yönetim

    suspend fun createReport(accessToken: String, input: ReportInput) {
        call<JsonObject> { http.post("reports") { bearerAuth(accessToken); json(input) } }
    }

    suspend fun listReports(accessToken: String, status: ReportStatus, page: Int): Page<AdminReport> =
        call {
            http.get("admin/reports") {
                bearerAuth(accessToken)
                parameter("status", status.name)
                parameter("page", page)
            }
        }

    suspend fun resolveReport(accessToken: String, id: String, status: ReportStatus) {
        call<JsonObject> { http.patch("admin/reports/$id") { bearerAuth(accessToken); json(StatusInput(status)) } }
    }

    suspend fun removePost(accessToken: String, id: String): Unit =
        call { http.delete("admin/posts/$id") { bearerAuth(accessToken) } }

    suspend fun removeComment(accessToken: String, id: String): Unit =
        call { http.delete("admin/comments/$id") { bearerAuth(accessToken) } }

    suspend fun listUsers(accessToken: String, q: String?, page: Int): Page<AdminUser> =
        call {
            http.get("admin/users") {
                bearerAuth(accessToken)
                parameter("q", q?.takeIf { it.isNotBlank() })
                parameter("page", page)
            }
        }

    suspend fun setBan(accessToken: String, userId: String, banned: Boolean) {
        call<JsonObject> { http.patch("admin/users/$userId/ban") { bearerAuth(accessToken); json(BanInput(banned)) } }
    }

    suspend fun setRole(accessToken: String, userId: String, role: Role) {
        call<JsonObject> { http.patch("admin/users/$userId/role") { bearerAuth(accessToken); json(RoleInput(role)) } }
    }

    private inline fun <reified T> HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    /** İsteği çalıştırır; başarısız yanıtları ve ağ hatalarını [ApiException]'a çevirir. */
    private suspend inline fun <reified T> call(block: () -> HttpResponse): T {
        val response = try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw ApiException.network(e)
        }

        if (!response.status.isSuccess()) {
            val body = runCatching {
                ApiJson.decodeFromString<JsonObject>(response.bodyAsText())
            }.getOrNull()
            throw ApiException.fromBody(response.status.value, body)
        }
        return try {
            response.body()
        } catch (e: SerializationException) {
            throw ApiException.unexpected(response.status.value, e)
        }
    }
}
