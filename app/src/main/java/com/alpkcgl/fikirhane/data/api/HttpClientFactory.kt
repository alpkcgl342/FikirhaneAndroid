package com.alpkcgl.fikirhane.data.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Sunucu yeni alan eklerse eski uygulama sürümleri bozulmasın.
 * `null` alanlar isteğe açıkça yazılır: PATCH /posts'ta kategori ya da kapağı kaldırmak
 * için `"categoryId": null` gönderilmesi gerekir (alan hiç yoksa sunucu eski değeri korur).
 */
val ApiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

object HttpClientFactory {
    /** Vercel fonksiyonları soğuk başlangıçta birkaç saniye gecikebilir. */
    private const val REQUEST_TIMEOUT_MS = 20_000L

    fun create(
        baseUrl: String,
        engine: HttpClientEngine,
        enableLogging: Boolean = false,
    ): HttpClient = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(ApiJson) }
        install(HttpTimeout) { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
        if (enableLogging) {
            install(Logging) {
                level = LogLevel.INFO
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("FikirhaneApi", message)
                    }
                }
                // Token'lar loglara yazılmaz.
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
        }
        defaultRequest { url(baseUrl) }
    }
}
