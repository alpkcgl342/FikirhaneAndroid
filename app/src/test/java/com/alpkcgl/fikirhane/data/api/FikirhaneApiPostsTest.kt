package com.alpkcgl.fikirhane.data.api

import com.alpkcgl.fikirhane.data.model.PostInput
import com.alpkcgl.fikirhane.data.model.PostQuery
import com.alpkcgl.fikirhane.data.model.PostStatus
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DETAIL = """
{"id":"p1","slug":"yazi","title":"Yazı","content":"x","coverUrl":null,"status":"DRAFT","readingTime":1,
 "createdAt":"2026-09-29T12:00:00Z","updatedAt":"2026-09-29T12:00:00Z",
 "author":{"username":"ayse","displayName":"Ayşe","avatarUrl":null},"category":null,"tags":[],
 "likeCount":0,"commentCount":0,"isOwner":true,"yeniAlan":"eski sürümler bozulmasın"}
"""

class FikirhaneApiPostsTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val api = FikirhaneApi(
        HttpClientFactory.create(
            "https://api.test/api/",
            MockEngine { request ->
                requests += request
                val body = when (request.url.encodedPath) {
                    "/api/posts" -> """{"items":[],"page":1,"pageSize":10,"total":0,"totalPages":0}"""
                    "/api/uploads/image" -> """{"url":"https://cdn.test/a.jpg"}"""
                    else -> DETAIL
                }
                respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
    )

    @Test
    fun `liste filtreleri sorgu parametresi olur, boş olanlar gönderilmez`() = runTest {
        api.listPosts("t", PostQuery(status = "DRAFT"), page = 2)

        val url = requests.single().url
        assertEquals("2", url.parameters["page"])
        assertEquals("DRAFT", url.parameters["status"])
        assertNull(url.parameters["author"])
        assertEquals("Bearer t", requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `güncellemede kaldırılan kategori ve kapak açıkça null gönderilir`() = runTest {
        val detail = api.updatePost(
            "t",
            "p1",
            PostInput("Yazı", "x", categoryId = null, tags = emptyList(), coverUrl = null, status = PostStatus.DRAFT),
        )

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        val json = (request.body as TextContent).text
        assertTrue(json, "\"categoryId\":null" in json)
        assertTrue(json, "\"coverUrl\":null" in json)
        assertTrue("bilinmeyen alan yok sayılır", detail.isOwner)
    }

    @Test
    fun `görsel çok parçalı form olarak yüklenir`() = runTest {
        val url = api.uploadImage("t", byteArrayOf(1, 2, 3), "image/jpeg", "gorsel.jpg")

        assertEquals("https://cdn.test/a.jpg", url)
        val body = requests.single().body as OutgoingContent
        assertTrue(body.contentType.toString().startsWith("multipart/form-data"))
    }
}
