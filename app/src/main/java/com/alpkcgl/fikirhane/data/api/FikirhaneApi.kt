package com.alpkcgl.fikirhane.data.api

import com.alpkcgl.fikirhane.data.model.CategoriesResponse
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
