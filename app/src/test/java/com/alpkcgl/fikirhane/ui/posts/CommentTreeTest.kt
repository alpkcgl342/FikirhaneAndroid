package com.alpkcgl.fikirhane.ui.posts

import com.alpkcgl.fikirhane.data.model.Comment
import com.alpkcgl.fikirhane.data.model.CommentAuthor
import org.junit.Assert.assertEquals
import org.junit.Test

private fun comment(id: String, parent: String? = null) = Comment(
    id = id,
    parentId = parent,
    content = "yorum $id",
    createdAt = "2026-09-29T12:00:00Z",
    author = CommentAuthor("u", "ayse", "Ayşe"),
)

class CommentTreeTest {
    private val comments = listOf(
        comment("a"),
        comment("b", "a"),
        comment("c", "b"),
        comment("d"),
        comment("e", "a"),
        comment("yetim", "silinmis"),
    )

    @Test
    fun `düz liste ağaca çevrilir, üst yorumu olmayanlar köke eklenir`() {
        val tree = buildCommentTree(comments)

        assertEquals(listOf("a", "d", "yetim"), tree.map { it.comment.id })
        assertEquals(listOf("b", "e"), tree[0].replies.map { it.comment.id })
        assertEquals(listOf("c"), tree[0].replies[0].replies.map { it.comment.id })
    }

    @Test
    fun `kaldırılan yorumun tüm yanıtları bulunur`() {
        assertEquals(setOf("b", "c", "e"), descendantsOf("a", comments))
        assertEquals(emptySet<String>(), descendantsOf("d", comments))
    }
}
