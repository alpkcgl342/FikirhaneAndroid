package com.alpkcgl.fikirhane.data.api

import com.alpkcgl.fikirhane.data.model.EmailRequest
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
