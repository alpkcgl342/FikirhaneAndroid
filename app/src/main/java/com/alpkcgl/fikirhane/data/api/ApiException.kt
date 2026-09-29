package com.alpkcgl.fikirhane.data.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * API hatası. Sunucu (NestJS) hataları `{ "statusCode": 400, "message": "…" | ["…"], "error": "…" }`
 * biçimindedir; mesajlar zaten Türkçedir ve doğrudan kullanıcıya gösterilir.
 */
class ApiException(
    val status: Int,
    override val message: String,
    /** Doğrulama hatalarında sunucunun döndürdüğü tüm mesajlar. */
    val messages: List<String> = listOf(message),
    cause: Throwable? = null,
) : Exception(message, cause) {

    val isNetwork: Boolean get() = status == 0
    val isUnauthorized: Boolean get() = status == 401

    /** 403: e-posta doğrulanmamış ya da hesap askıya alınmış (mesaj hangisi olduğunu söyler). */
    val isForbidden: Boolean get() = status == 403

    companion object {
        fun network(cause: Throwable) =
            ApiException(0, "Sunucuya ulaşılamadı. İnternet bağlantını kontrol et.", cause = cause)

        fun unexpected(status: Int, cause: Throwable? = null) =
            ApiException(status, "Sunucu beklenmeyen bir yanıt verdi.", cause = cause)

        /** Hata gövdesini çözer; beklenen biçimde değilse genel bir mesaj döner. */
        fun fromBody(status: Int, body: JsonObject?): ApiException {
            val messages = when (val message = body?.get("message")) {
                is JsonArray -> message.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                is JsonPrimitive -> listOfNotNull(message.contentOrNull)
                else -> emptyList()
            }
            if (messages.isEmpty()) return unexpected(status)
            return ApiException(status, messages.first(), messages)
        }
    }
}
