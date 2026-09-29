package com.alpkcgl.fikirhane.ui.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.model.Comment
import com.alpkcgl.fikirhane.data.model.PostDetail
import com.alpkcgl.fikirhane.data.model.ReportTargetType
import com.alpkcgl.fikirhane.data.model.Role
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.data.repository.ModerationRepository
import com.alpkcgl.fikirhane.data.repository.PostsRepository
import com.alpkcgl.fikirhane.ui.appContainer
import com.alpkcgl.fikirhane.ui.navigation.PostRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostDetailUiState(
    val post: PostDetail? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val notFound: Boolean = false,
    val deleting: Boolean = false,
    /** Silindi ya da kaldırıldı: ekran kapatılır (tek seferlik olay). */
    val deleted: Boolean = false,
    val actionBusy: Boolean = false,
    /** Kısa bilgi mesajı (ör. "Şikâyetin alındı"). */
    val message: String? = null,
    val comments: List<Comment> = emptyList(),
    val commentsLoading: Boolean = false,
    val commentsError: String? = null,
    val sendingComment: Boolean = false,
    val commentError: String? = null,
    /** Yanıt verilen yorum (null: yazıya yorum). */
    val replyTo: Comment? = null,
)

class PostDetailViewModel(
    private val slug: String,
    private val posts: PostsRepository,
    private val moderation: ModerationRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PostDetailUiState())
    val state: StateFlow<PostDetailUiState> = _state.asStateFlow()

    val currentUserId: String? get() = auth.currentUser.value?.id

    /** Arayüzde moderasyon seçeneklerini göstermek için (asıl yetki kontrolü sunucudadır). */
    val isModerator: Boolean
        get() = auth.currentUser.value?.role.let { it == Role.MODERATOR || it == Role.ADMIN }

    init {
        load()
        // Düzenleyiciden dönülünce güncel hâli gösterilir.
        viewModelScope.launch { posts.changes.drop(1).collect { if (!_state.value.deleted) load() } }
    }

    fun load() {
        _state.update { it.copy(loading = it.post == null, error = null) }
        viewModelScope.launch {
            try {
                val post = posts.get(slug)
                _state.update { it.copy(post = post, loading = false) }
                if (post.status.name == "PUBLISHED") loadComments(post.id)
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, error = e.message, notFound = e.status == 404) }
            }
        }
    }

    private fun loadComments(postId: String) {
        _state.update { it.copy(commentsLoading = it.comments.isEmpty(), commentsError = null) }
        viewModelScope.launch {
            try {
                val comments = posts.comments(postId)
                _state.update { it.copy(comments = comments, commentsLoading = false) }
            } catch (e: ApiException) {
                _state.update { it.copy(commentsLoading = false, commentsError = e.message) }
            }
        }
    }

    fun retryComments() = _state.value.post?.let { loadComments(it.id) }

    fun toggleLike() = postAction { post ->
        val result = posts.toggleLike(post.id)
        _state.update { it.copy(post = post.copy(likedByMe = result.liked, likeCount = result.likeCount)) }
    }

    fun toggleBookmark() = postAction { post ->
        val result = posts.toggleBookmark(post.id)
        _state.update {
            it.copy(
                post = post.copy(bookmarkedByMe = result.bookmarked),
                message = if (result.bookmarked) "Kaydedildi — Yazılarım › Kaydedilenler" else null,
            )
        }
    }

    private fun postAction(block: suspend (PostDetail) -> Unit) {
        val post = _state.value.post ?: return
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true, message = null) }
        viewModelScope.launch {
            try {
                block(post)
            } catch (e: ApiException) {
                _state.update { it.copy(message = e.message) }
            } finally {
                _state.update { it.copy(actionBusy = false) }
            }
        }
    }

    fun replyTo(comment: Comment?) = _state.update { it.copy(replyTo = comment, commentError = null) }

    /** Yorumu gönderir; başarılıysa true (giriş alanı temizlenir). */
    suspend fun sendComment(content: String): Boolean {
        val post = _state.value.post ?: return false
        if (content.isBlank()) {
            _state.update { it.copy(commentError = "Yorum boş olamaz") }
            return false
        }
        if (content.length > 2000) {
            _state.update { it.copy(commentError = "Yorum en fazla 2000 karakter olabilir") }
            return false
        }
        _state.update { it.copy(sendingComment = true, commentError = null) }
        return try {
            val created = posts.addComment(post.id, content, _state.value.replyTo?.id)
            _state.update {
                it.copy(
                    comments = it.comments + created,
                    post = it.post?.copy(commentCount = it.post.commentCount + 1),
                    sendingComment = false,
                    replyTo = null,
                )
            }
            true
        } catch (e: ApiException) {
            _state.update { it.copy(sendingComment = false, commentError = e.message) }
            false
        }
    }

    suspend fun report(type: ReportTargetType, targetId: String, reason: String) {
        moderation.report(type, targetId, reason)
        _state.update { it.copy(message = "Şikâyetin alındı, teşekkürler.") }
    }

    fun delete() {
        val post = _state.value.post ?: return
        _state.update { it.copy(deleting = true, error = null) }
        viewModelScope.launch {
            try {
                posts.delete(post.id)
                _state.update { it.copy(deleting = false, deleted = true) }
            } catch (e: ApiException) {
                _state.update { it.copy(deleting = false, error = e.message) }
            }
        }
    }

    /** Moderatör: başkasının yazısını kaldırır. */
    fun moderatorRemovePost() = postAction { post ->
        moderation.removePost(post.id)
        _state.update { it.copy(deleted = true) }
    }

    /** Moderatör: yorumu ve tüm yanıtlarını kaldırır. */
    fun moderatorRemoveComment(commentId: String) {
        viewModelScope.launch {
            try {
                moderation.removeComment(commentId)
                _state.update { s ->
                    val removed = descendantsOf(commentId, s.comments) + commentId
                    s.copy(
                        comments = s.comments.filterNot { it.id in removed },
                        post = s.post?.copy(commentCount = (s.post.commentCount - removed.size).coerceAtLeast(0)),
                    )
                }
            } catch (e: ApiException) {
                _state.update { it.copy(message = e.message) }
            }
        }
    }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route = createSavedStateHandle().toRoute<PostRoute>()
                val c = appContainer()
                PostDetailViewModel(route.slug, c.postsRepository, c.moderationRepository, c.authRepository)
            }
        }
    }
}

/** Bir yorumun (dolaylı) tüm yanıtlarının kimlikleri. */
internal fun descendantsOf(id: String, comments: List<Comment>): Set<String> {
    val result = mutableSetOf<String>()
    var frontier = setOf(id)
    while (frontier.isNotEmpty()) {
        frontier = comments.filter { it.parentId in frontier }.map { it.id }.toSet() - result
        result += frontier
    }
    return result
}
