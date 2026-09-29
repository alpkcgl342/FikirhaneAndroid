package com.alpkcgl.fikirhane.ui.posts

import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.model.Author
import com.alpkcgl.fikirhane.data.model.Page
import com.alpkcgl.fikirhane.data.model.PostStatus
import com.alpkcgl.fikirhane.data.model.PostSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun post(id: String) = PostSummary(
    id = id,
    slug = "yazi-$id",
    title = "Yazı $id",
    status = PostStatus.PUBLISHED,
    readingTime = 1,
    createdAt = "2026-09-29T12:00:00Z",
    updatedAt = "2026-09-29T12:00:00Z",
    author = Author("ayse", "Ayşe"),
)

private fun page(n: Int, total: Int, vararg ids: String) =
    Page(ids.map(::post), page = n, pageSize = ids.size, total = total * ids.size, totalPages = total)

@OptIn(ExperimentalCoroutinesApi::class)
class PostPagerTest {

    private fun TestScope.pager(load: suspend (Int) -> Page<PostSummary>) =
        PostPager(TestScope(StandardTestDispatcher(testScheduler)), load)

    @Test
    fun `sayfalar eklenir, tekrar edenler atılır, son sayfada durur`() = runTest {
        val requested = mutableListOf<Int>()
        val pager = pager { n ->
            requested += n
            if (n == 1) page(1, 2, "a", "b") else page(2, 2, "b", "c")
        }

        pager.refresh(); advanceUntilIdle()
        pager.loadMore(); advanceUntilIdle()
        pager.loadMore(); advanceUntilIdle() // son sayfa: istek atılmaz

        val s = pager.state.value
        assertEquals(listOf("a", "b", "c"), s.items.map { it.id })
        assertTrue(s.endReached)
        assertEquals(listOf(1, 2), requested)
    }

    @Test
    fun `hata gösterilir, tekrar denenince kaldığı sayfadan devam eder`() = runTest {
        var fail = true
        val pager = pager { n ->
            when {
                n == 1 -> page(1, 2, "a")
                fail -> throw ApiException(0, "Sunucuya ulaşılamadı.")
                else -> page(2, 2, "b")
            }
        }

        pager.refresh(); advanceUntilIdle()
        pager.loadMore(); advanceUntilIdle()
        assertEquals("Sunucuya ulaşılamadı.", pager.state.value.error)

        fail = false
        pager.retry(); advanceUntilIdle()

        val s = pager.state.value
        assertNull(s.error)
        assertEquals(listOf("a", "b"), s.items.map { it.id })
    }

    @Test
    fun `yenileme sırasında liste korunur ve yenileniyor durumu gösterilir`() = runTest {
        var round = 0
        val pager = pager { round++; page(1, 1, if (round == 1) "a" else "yeni") }

        pager.refresh(); advanceUntilIdle()
        pager.refresh()
        assertTrue(pager.state.value.refreshing)
        assertFalse(pager.state.value.loading)
        assertEquals(listOf("a"), pager.state.value.items.map { it.id })

        advanceUntilIdle()
        assertEquals(listOf("yeni"), pager.state.value.items.map { it.id })
        assertFalse(pager.state.value.refreshing)
    }
}
