package com.alpkcgl.fikirhane.ui.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.model.PostDetail
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
    /** Silindi: ekran kapatılır (tek seferlik olay). */
    val deleted: Boolean = false,
)

class PostDetailViewModel(
    private val slug: String,
    private val posts: PostsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PostDetailUiState())
    val state: StateFlow<PostDetailUiState> = _state.asStateFlow()

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
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, error = e.message, notFound = e.status == 404) }
            }
        }
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route = createSavedStateHandle().toRoute<PostRoute>()
                PostDetailViewModel(route.slug, appContainer().postsRepository)
            }
        }
    }
}
