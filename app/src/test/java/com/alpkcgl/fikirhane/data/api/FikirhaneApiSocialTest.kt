package com.alpkcgl.fikirhane.data.api

import com.alpkcgl.fikirhane.data.model.NotificationType
import com.alpkcgl.fikirhane.data.model.PostQuery
import com.alpkcgl.fikirhane.data.model.ReportInput
import com.alpkcgl.fikirhane.data.model.ReportStatus
import com.alpkcgl.fikirhane.data.model.ReportTargetType
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val EMPTY_PAGE = """{"items":[],"page":1,"pageSize":10,"total":0,"totalPages":0}"""

class FikirhaneApiSocialTest {
    private val requests = mutableListOf<HttpRequestData>()
    private var nextBody = EMPTY_PAGE
    private val api = FikirhaneApi(
        HttpClientFactory.create(
            "https://api.test/api/",
            MockEngine { request ->
                requests += request
                respond(nextBody, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
    )

    @Test
    fun `akış filtreleri sorgu parametresi olur`() = runTest {
        api.listPosts("t", PostQuery(sort = "popular", following = true), page = 1)
        api.listPosts("t", PostQuery(bookmarked = true), page = 1)
        api.listPosts("t", PostQuery(), page = 1)

        val (popular, bookmarked, plain) = requests.map { it.url.parameters }
        assertEquals("popular", popular["sort"])
        assertEquals("true", popular["following"])
        assertEquals("true", bookmarked["bookmarked"])
        assertNull(plain["following"])
        assertNull(plain["bookmarked"])
        assertNull(plain["sort"])
    }

    @Test
    fun `bildirimler çözülür (yanıt ve takip)`() = runTest {
        nextBody = """
            {"items":[
              {"id":"n1","type":"COMMENT","isRead":false,"createdAt":"2026-09-29T12:00:00Z",
               "data":{"actor":{"id":"a","username":"ayse","displayName":"Ayşe","avatarUrl":null},
                       "post":{"id":"p","slug":"yazi","title":"Yazı"},"commentId":"c","reply":true}},
              {"id":"n2","type":"FOLLOW","isRead":true,"createdAt":"2026-09-29T12:00:00Z",
               "data":{"actor":{"id":"b","username":"can","displayName":"Can","avatarUrl":null}}}
            ],"unreadCount":1,"page":1,"pageSize":20,"total":2,"totalPages":1}
        """

        val page = api.listNotifications("t", 1)

        assertEquals(1, page.unreadCount)
        val (reply, follow) = page.items
        assertTrue(reply.data.reply)
        assertEquals("yazi", reply.data.post?.slug)
        assertEquals(NotificationType.FOLLOW, follow.type)
        assertNull(follow.data.post)
    }

    @Test
    fun `şikâyet listesinde silinmiş hedef null, var olan hedef okunabilir`() = runTest {
        nextBody = """
            {"items":[
              {"id":"r1","targetType":"POST","targetId":"p","reason":"Spam","status":"PENDING","createdAt":"2026-09-29T12:00:00Z",
               "reporter":{"id":"a","username":"ayse","displayName":"Ayşe"},
               "target":{"id":"p","slug":"yazi","title":"Yazı","author":{"id":"b","username":"can","displayName":"Can"}}},
              {"id":"r2","targetType":"COMMENT","targetId":"c","reason":"Hakaret","status":"PENDING","createdAt":"2026-09-29T12:00:00Z",
               "reporter":{"id":"a","username":"ayse","displayName":"Ayşe"},"target":null}
            ],"page":1,"pageSize":20,"total":2,"totalPages":1}
        """

        val page = api.listReports("t", ReportStatus.PENDING, 1)

        assertEquals("PENDING", requests.single().url.parameters["status"])
        val (post, deleted) = page.items
        assertEquals("Yazı", post.target!!["title"]!!.jsonPrimitive.content)
        assertEquals("b", post.target["author"]!!.jsonObject["id"]!!.jsonPrimitive.content)
        assertNull(deleted.target)
    }

    @Test
    fun `şikâyet gövdesi doğru gönderilir`() = runTest {
        nextBody = """{"id":"r1","status":"PENDING","createdAt":"2026-09-29T12:00:00Z"}"""

        api.createReport("t", ReportInput(ReportTargetType.USER, "u1", "Sahte hesap"))

        val json = (requests.single().body as TextContent).text
        assertTrue(json, "\"targetType\":\"USER\"" in json)
        assertTrue(json, "\"reason\":\"Sahte hesap\"" in json)
    }

    @Test
    fun `arama kişileri ve yazıları birlikte döner`() = runTest {
        nextBody = """{"query":"ali","items":[],"page":1,"pageSize":10,"total":0,"totalPages":0,
            "users":[{"username":"ali","displayName":"Ali","avatarUrl":null,"bio":null}]}"""

        val result = api.search("ali", 1)

        assertEquals("ali", requests.single().url.parameters["q"])
        assertEquals(listOf("ali"), result.users.map { it.username })
    }
}
