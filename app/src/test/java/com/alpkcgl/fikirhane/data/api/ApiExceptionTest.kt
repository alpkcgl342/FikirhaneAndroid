package com.alpkcgl.fikirhane.data.api

import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiExceptionTest {

    private fun body(json: String) = ApiJson.decodeFromString<JsonObject>(json)

    @Test
    fun `tek mesajlı Nest hatası`() {
        val e = ApiException.fromBody(
            401,
            body("""{"message":"E-posta veya şifre hatalı","error":"Unauthorized","statusCode":401}"""),
        )
        assertEquals(401, e.status)
        assertEquals("E-posta veya şifre hatalı", e.message)
        assertTrue(e.isUnauthorized)
    }

    @Test
    fun `doğrulama hatası listesinde ilk mesaj gösterilir, hepsi saklanır`() {
        val e = ApiException.fromBody(
            400,
            body("""{"message":["Geçerli bir e-posta adresi girin","Şifre en az 8 karakter olmalı"],"statusCode":400}"""),
        )
        assertEquals("Geçerli bir e-posta adresi girin", e.message)
        assertEquals(2, e.messages.size)
    }

    @Test
    fun `beklenmeyen gövdede genel mesaj`() {
        assertEquals("Sunucu beklenmeyen bir yanıt verdi.", ApiException.fromBody(502, null).message)
        assertEquals("Sunucu beklenmeyen bir yanıt verdi.", ApiException.fromBody(500, body("""{"x":1}""")).message)
    }
}
