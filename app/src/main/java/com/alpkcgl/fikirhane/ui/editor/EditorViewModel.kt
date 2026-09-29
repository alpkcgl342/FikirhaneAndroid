package com.alpkcgl.fikirhane.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.alpkcgl.fikirhane.core.PostRules
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.image.ImageException
import com.alpkcgl.fikirhane.data.image.ImagePreparer
import com.alpkcgl.fikirhane.data.model.Category
import com.alpkcgl.fikirhane.data.model.PostDetail
import com.alpkcgl.fikirhane.data.model.PostInput
import com.alpkcgl.fikirhane.data.model.PostStatus
import com.alpkcgl.fikirhane.data.repository.PostsRepository
import com.alpkcgl.fikirhane.ui.appContainer
import com.alpkcgl.fikirhane.ui.components.AlertKind
import com.alpkcgl.fikirhane.ui.navigation.EditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditorField { Title, Content, Tags }

data class EditorUiState(
    /** Düzenlenen yazı (yeni yazıda null). */
    val post: PostDetail? = null,
    val loading: Boolean = false,
    val title: String = "",
    val content: String = "",
    val categoryId: String? = null,
    val tagsText: String = "",
    val coverUrl: String? = null,
    val categories: List<Category> = emptyList(),
    val errors: Map<EditorField, String> = emptyMap(),
    val saving: Boolean = false,
    val uploading: Boolean = false,
    val uploadError: String? = null,
    val alert: String? = null,
    val alertKind: AlertKind = AlertKind.Error,
    /** Kaydedilmemiş değişiklik var mı (geri tuşunda onay istenir)? */
    val dirty: Boolean = false,
    /** Başkasının yazısı açıldıysa form kilitlenir. */
    val readOnly: Boolean = false,
    /** Yayınlandı: yazı sayfasına gidilir (tek seferlik olay). */
    val publishedSlug: String? = null,
) {
    val busy: Boolean get() = saving || uploading || loading
    val readingTime: Int get() = PostRules.readingTime(content)
}

class EditorViewModel(
    private val slug: String?,
    private val posts: PostsRepository,
    private val images: ImagePreparer,
) : ViewModel() {
    private val _state = MutableStateFlow(EditorUiState(loading = slug != null))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Kategoriler yüklenemezse yazı kategorisiz kaydedilebilir.
            runCatching { posts.categories() }.onSuccess { list -> _state.update { it.copy(categories = list) } }
        }
        if (slug != null) loadPost(slug)
    }

    private fun loadPost(slug: String) {
        viewModelScope.launch {
            try {
                val post = posts.get(slug)
                if (!post.isOwner) {
                    _state.update { it.copy(loading = false, readOnly = true, alert = "Bu yazıyı düzenleme yetkin yok.") }
                    return@launch
                }
                _state.update { it.fill(post).copy(loading = false) }
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, readOnly = true, alert = e.message) }
            }
        }
    }

    private fun EditorUiState.fill(post: PostDetail) = copy(
        post = post,
        title = post.title,
        content = post.content,
        categoryId = post.category?.id,
        tagsText = post.tags.joinToString(", "),
        coverUrl = post.coverUrl,
        dirty = false,
    )

    private fun edit(block: (EditorUiState) -> EditorUiState) =
        _state.update { block(it).copy(dirty = true) }

    fun onTitleChange(value: String) = edit { it.copy(title = value, errors = it.errors - EditorField.Title) }
    fun onContentChange(value: String) = edit { it.copy(content = value, errors = it.errors - EditorField.Content) }
    fun onTagsChange(value: String) = edit { it.copy(tagsText = value, errors = it.errors - EditorField.Tags) }
    fun onCategoryChange(id: String?) = edit { it.copy(categoryId = id) }
    fun removeCover() = edit { it.copy(coverUrl = null, uploadError = null) }

    fun onCoverPicked(uri: Uri) {
        _state.update { it.copy(uploading = true, uploadError = null) }
        viewModelScope.launch {
            try {
                val url = posts.uploadImage(images.prepare(uri))
                edit { it.copy(coverUrl = url, uploading = false) }
            } catch (e: ImageException) {
                _state.update { it.copy(uploading = false, uploadError = e.message) }
            } catch (e: ApiException) {
                _state.update { it.copy(uploading = false, uploadError = e.message) }
            }
        }
    }

    fun onPublishedHandled() = _state.update { it.copy(publishedSlug = null) }

    fun save(status: PostStatus) {
        val s = _state.value
        if (s.busy || s.readOnly) return

        val tags = PostRules.parseTags(s.tagsText)
        val errors = buildMap {
            PostRules.title(s.title)?.let { put(EditorField.Title, it) }
            PostRules.content(s.content)?.let { put(EditorField.Content, it) }
            PostRules.tags(tags)?.let { put(EditorField.Tags, it) }
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }

        val input = PostInput(
            title = s.title.trim(),
            content = s.content,
            categoryId = s.categoryId,
            tags = tags,
            coverUrl = s.coverUrl,
            status = status,
        )
        _state.update { it.copy(saving = true, alert = null) }
        viewModelScope.launch {
            try {
                val existing = s.post
                val saved = if (existing != null) posts.update(existing.id, input) else posts.create(input)
                if (saved.status == PostStatus.PUBLISHED) {
                    _state.update { it.fill(saved).copy(saving = false, publishedSlug = saved.slug) }
                } else {
                    // Taslak: ekranda kalınır; sonraki kayıtlar bu yazıyı günceller.
                    _state.update {
                        it.fill(saved).copy(saving = false, alert = "Taslak kaydedildi.", alertKind = AlertKind.Success)
                    }
                }
            } catch (e: ApiException) {
                _state.update { it.copy(saving = false, alert = e.message, alertKind = AlertKind.Error) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route = createSavedStateHandle().toRoute<EditorRoute>()
                val container = appContainer()
                EditorViewModel(route.slug, container.postsRepository, container.imagePreparer)
            }
        }
    }
}
