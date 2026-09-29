package com.alpkcgl.fikirhane.data.repository

import com.alpkcgl.fikirhane.data.api.FikirhaneApi
import com.alpkcgl.fikirhane.data.model.BookmarkResult
import com.alpkcgl.fikirhane.data.model.Category
import com.alpkcgl.fikirhane.data.model.Comment
import com.alpkcgl.fikirhane.data.model.CommentInput
import com.alpkcgl.fikirhane.data.model.LikeResult
import com.alpkcgl.fikirhane.data.model.SearchResponse
import com.alpkcgl.fikirhane.data.model.Page
import com.alpkcgl.fikirhane.data.model.PostDetail
import com.alpkcgl.fikirhane.data.model.PostInput
import com.alpkcgl.fikirhane.data.model.PostQuery
import com.alpkcgl.fikirhane.data.model.PostSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Yazı işlemleri; tüm istekler oturumdaki kullanıcı adına yapılır. */
class PostsRepository(
    private val api: FikirhaneApi,
    private val auth: AuthRepository,
) {
    private val categoriesMutex = Mutex()
    private var categories: List<Category>? = null

    /** Bir yazı oluşturulduğunda, güncellendiğinde ya da silindiğinde artar; listeler yenilenir. */
    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes.asStateFlow()

    suspend fun list(query: PostQuery, page: Int): Page<PostSummary> =
        auth.authorized { token -> api.listPosts(token, query, page) }

    suspend fun get(slug: String): PostDetail =
        auth.authorized { token -> api.getPost(token, slug) }

    suspend fun create(input: PostInput): PostDetail =
        auth.authorized { token -> api.createPost(token, input) }.also { _changes.update { it + 1 } }

    suspend fun update(id: String, input: PostInput): PostDetail =
        auth.authorized { token -> api.updatePost(token, id, input) }.also { _changes.update { it + 1 } }

    suspend fun delete(id: String) {
        auth.authorized { token -> api.deletePost(token, id) }
        _changes.update { it + 1 }
    }

    /** Moderasyonla bir yazı kaldırıldığında listelerin yenilenmesi için. */
    fun notifyChanged() = _changes.update { it + 1 }

    suspend fun toggleLike(postId: String): LikeResult =
        auth.authorized { token -> api.toggleLike(token, postId) }

    suspend fun toggleBookmark(postId: String): BookmarkResult =
        auth.authorized { token -> api.toggleBookmark(token, postId) }

    suspend fun comments(postId: String): List<Comment> = api.listComments(postId)

    suspend fun addComment(postId: String, content: String, parentId: String?): Comment =
        auth.authorized { token -> api.createComment(token, postId, CommentInput(content.trim(), parentId)) }

    suspend fun search(q: String, page: Int): SearchResponse = api.search(q, page)

    suspend fun uploadImage(image: PreparedImage): String =
        auth.authorized { token -> api.uploadImage(token, image.bytes, image.mimeType, image.fileName) }

    /** Kategoriler nadiren değişir; uygulama açıkken bir kez yüklenir. */
    suspend fun categories(): List<Category> = categoriesMutex.withLock {
        categories ?: api.listCategories().also { categories = it }
    }
}

/** Yüklemeye hazır görsel. */
class PreparedImage(val bytes: ByteArray, val mimeType: String, val fileName: String)
