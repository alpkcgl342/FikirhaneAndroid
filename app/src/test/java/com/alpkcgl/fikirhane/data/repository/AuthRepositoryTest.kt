package com.alpkcgl.fikirhane.data.repository

import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.api.FikirhaneApi
import com.alpkcgl.fikirhane.data.api.HttpClientFactory
import com.alpkcgl.fikirhane.data.model.PublicUser
import com.alpkcgl.fikirhane.data.model.SessionTokens
import com.alpkcgl.fikirhane.data.session.SessionStore
import com.alpkcgl.fikirhane.data.session.StoredSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class MemoryStore(var value: StoredSession? = null) : SessionStore {
    override fun load() = value
    override fun save(session: StoredSession) {
        value = session
    }
    override fun clear() {
        value = null
    }
}

private const val NOW = 1_000_000L

private val USER_JSON = """
    {"id":"u1","username":"ayse","email":"ayse@example.com","displayName":"Ayşe",
     "bio":null,"avatarUrl":null,"role":"USER","isBanned":false,"createdAt":"2026-09-29T00:00:00Z"}
""".trimIndent()

private fun sessionJson(access: String, expiresAt: Long = NOW + 3600) =
    """{"accessToken":"$access","refreshToken":"r-$access","expiresIn":3600,"expiresAt":$expiresAt}"""

private val user = PublicUser(
    id = "u1",
    username = "ayse",
    email = "ayse@example.com",
    displayName = "Ayşe",
    createdAt = "2026-09-29T00:00:00Z",
)

private fun stored(access: String, expiresAt: Long) =
    StoredSession(SessionTokens(access, "r-$access", 3600, expiresAt), user)

/** Sahte sunucu: istekleri kaydeder, yanıtı [handler] belirler. */
private class FakeServer(
    val handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) {
    val requests = mutableListOf<HttpRequestData>()
    val engine = MockEngine { request ->
        requests += request
        handler(request)
    }
    val api = FikirhaneApi(HttpClientFactory.create("https://api.test/api/", engine))

    fun paths() = requests.map { it.url.encodedPath }
}

private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

private fun HttpRequestData.bearer() = headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")

class AuthRepositoryTest {

    @Test
    fun `giriş oturumu kaydeder ve kullanıcıyı yayınlar`() = runTest {
        val server = FakeServer { json("""{"user":$USER_JSON,"session":${sessionJson("a1")}}""") }
        val store = MemoryStore()
        val repo = AuthRepository(server.api, store) { NOW }

        repo.login("  Ayse@Example.com ", "sifre1234")

        assertEquals("ayse", repo.currentUser.value?.username)
        assertEquals("a1", store.value?.tokens?.accessToken)
        assertEquals(listOf("/api/auth/login"), server.paths())
    }

    @Test
    fun `hatalı girişte Türkçe sunucu mesajı iletilir, oturum açılmaz`() = runTest {
        val server = FakeServer {
            json("""{"message":"E-posta veya şifre hatalı","statusCode":401}""", HttpStatusCode.Unauthorized)
        }
        val repo = AuthRepository(server.api, MemoryStore()) { NOW }

        val e = runCatching { repo.login("a@b.co", "x") }.exceptionOrNull() as ApiException

        assertEquals("E-posta veya şifre hatalı", e.message)
        assertNull(repo.currentUser.value)
    }

    @Test
    fun `e-posta doğrulaması gerekiyorsa oturum açılmaz`() = runTest {
        val server = FakeServer {
            json("""{"user":$USER_JSON,"session":null,"emailConfirmationRequired":true}""", HttpStatusCode.Created)
        }
        val repo = AuthRepository(server.api, MemoryStore()) { NOW }

        val result = repo.register("Ayse@Example.com", "sifre1234", "Ayse", " Ayşe ")

        assertEquals(RegisterResult.ConfirmationRequired("ayse@example.com"), result)
        assertNull(repo.currentUser.value)
    }

    @Test
    fun `süresi dolmak üzere olan token istekten önce yenilenir`() = runTest {
        val server = FakeServer { request ->
            when (request.url.encodedPath) {
                "/api/auth/refresh" -> json("""{"session":${sessionJson("a2")}}""")
                else -> json("""{"user":$USER_JSON}""")
            }
        }
        val store = MemoryStore(stored("a1", expiresAt = NOW + 30))
        val repo = AuthRepository(server.api, store) { NOW }

        repo.refreshUser()

        assertEquals(listOf("/api/auth/refresh", "/api/auth/me"), server.paths())
        assertEquals("a2", server.requests.last().bearer())
        assertEquals("a2", store.value?.tokens?.accessToken)
    }

    @Test
    fun `401 alınırsa bir kez yenilenip tekrar denenir`() = runTest {
        val server = FakeServer { request ->
            when {
                request.url.encodedPath == "/api/auth/refresh" -> json("""{"session":${sessionJson("a2")}}""")
                request.bearer() == "a1" -> json("""{"message":"Oturum geçersiz","statusCode":401}""", HttpStatusCode.Unauthorized)
                else -> json("""{"user":$USER_JSON}""")
            }
        }
        val repo = AuthRepository(server.api, MemoryStore(stored("a1", NOW + 3600))) { NOW }

        repo.refreshUser()

        assertEquals(listOf("/api/auth/me", "/api/auth/refresh", "/api/auth/me"), server.paths())
    }

    @Test
    fun `eşzamanlı isteklerde yalnızca bir yenileme yapılır`() = runTest {
        val server = FakeServer { request ->
            when (request.url.encodedPath) {
                "/api/auth/refresh" -> {
                    delay(50)
                    json("""{"session":${sessionJson("a2")}}""")
                }
                else -> json("""{"user":$USER_JSON}""")
            }
        }
        val repo = AuthRepository(server.api, MemoryStore(stored("a1", NOW + 10))) { NOW }

        (1..3).map { async { repo.refreshUser() } }.awaitAll()

        assertEquals(1, server.paths().count { it == "/api/auth/refresh" })
        assertTrue(server.requests.filter { it.url.encodedPath == "/api/auth/me" }.all { it.bearer() == "a2" })
    }

    @Test
    fun `yenileme reddedilirse oturum kapatılır`() = runTest {
        val server = FakeServer {
            json("""{"message":"Oturum sona erdi, lütfen tekrar giriş yapın","statusCode":401}""", HttpStatusCode.Unauthorized)
        }
        val store = MemoryStore(stored("a1", NOW + 10))
        val repo = AuthRepository(server.api, store) { NOW }

        val e = runCatching { repo.refreshUser() }.exceptionOrNull() as ApiException

        assertTrue(e.isUnauthorized)
        assertNull(repo.currentUser.value)
        assertNull(store.value)
    }

    @Test
    fun `ağ hatasında oturum korunur`() = runTest {
        val server = FakeServer { throw IOException("bağlantı yok") }
        val store = MemoryStore(stored("a1", NOW + 10))
        val repo = AuthRepository(server.api, store) { NOW }

        val e = runCatching { repo.refreshUser() }.exceptionOrNull() as ApiException

        assertTrue(e.isNetwork)
        assertNotNull(repo.currentUser.value)
        assertNotNull(store.value)
    }
}
