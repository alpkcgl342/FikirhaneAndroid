package com.alpkcgl.fikirhane.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.model.PostQuery
import com.alpkcgl.fikirhane.data.model.PublicUser
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.data.repository.PostsRepository
import com.alpkcgl.fikirhane.ui.appContainer
import com.alpkcgl.fikirhane.ui.posts.PagedPosts
import com.alpkcgl.fikirhane.ui.posts.PostPager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Ana akış: yayındaki yazılar, en yeniden eskiye. */
class HomeViewModel(
    private val auth: AuthRepository,
    posts: PostsRepository,
) : ViewModel() {
    val user: StateFlow<PublicUser?> = auth.currentUser

    private val pager = PostPager(viewModelScope) { page -> posts.list(PostQuery(), page) }
    val feed: StateFlow<PagedPosts> = pager.state

    init {
        pager.refresh()
        // Bir yazı eklenince/güncellenince/silinince akış yenilenir.
        viewModelScope.launch { posts.changes.drop(1).collect { pager.refresh() } }
        // Kullanıcı bilgisi (ör. rol) sunucudan tazelenir; 401'de oturum kapanır, NavHost yönlendirir.
        viewModelScope.launch {
            try {
                auth.refreshUser()
            } catch (_: ApiException) {
                // Ağ hatasında kayıtlı bilgiyle devam edilir; akış kendi hatasını gösterir.
            }
        }
    }

    fun refresh() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retry() = pager.retry()
    fun logout() = auth.logout()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HomeViewModel(container.authRepository, container.postsRepository)
            }
        }
    }
}
