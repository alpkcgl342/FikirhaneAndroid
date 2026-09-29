package com.alpkcgl.fikirhane.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.model.Category
import com.alpkcgl.fikirhane.data.model.PostQuery
import com.alpkcgl.fikirhane.data.model.PublicUser
import com.alpkcgl.fikirhane.data.model.Role
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.data.repository.NotificationsRepository
import com.alpkcgl.fikirhane.data.repository.PostsRepository
import com.alpkcgl.fikirhane.ui.appContainer
import com.alpkcgl.fikirhane.ui.posts.PagedPosts
import com.alpkcgl.fikirhane.ui.posts.PostPager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

enum class Feed(val label: String, val empty: String) {
    New("En yeniler", "Henüz yayınlanmış bir yazı yok. İlk yazıyı sen yaz!"),
    Popular("Popüler", "Henüz yayınlanmış bir yazı yok. İlk yazıyı sen yaz!"),
    Following("Takip ettiklerim", "Takip ettiğin kişilerin henüz yazısı yok. Yazarların profilinden onları takip edebilirsin."),
}

/** Ana akış: en yeniler, popüler ya da takip edilenler. */
class HomeViewModel(
    private val auth: AuthRepository,
    private val notifications: NotificationsRepository,
    posts: PostsRepository,
) : ViewModel() {
    val user: StateFlow<PublicUser?> = auth.currentUser
    val unreadCount: StateFlow<Int> = notifications.unread

    private val _feed = MutableStateFlow(Feed.New)
    val feed: StateFlow<Feed> = _feed.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val pager = PostPager(viewModelScope) { page ->
        val query = when (_feed.value) {
            Feed.New -> PostQuery()
            Feed.Popular -> PostQuery(sort = "popular")
            Feed.Following -> PostQuery(following = true)
        }
        posts.list(query, page)
    }
    val list: StateFlow<PagedPosts> = pager.state

    val isModerator: Boolean
        get() = user.value?.role.let { it == Role.MODERATOR || it == Role.ADMIN }

    init {
        pager.refresh()
        // Bir yazı eklenince/güncellenince/silinince akış yenilenir.
        viewModelScope.launch { posts.changes.drop(1).collect { pager.refresh() } }
        viewModelScope.launch { runCatching { posts.categories() }.onSuccess { _categories.value = it } }
        // Kullanıcı bilgisi (ör. rol) sunucudan tazelenir; 401'de oturum kapanır, NavHost yönlendirir.
        viewModelScope.launch {
            try {
                auth.refreshUser()
            } catch (_: ApiException) {
                // Ağ hatasında kayıtlı bilgiyle devam edilir; akış kendi hatasını gösterir.
            }
        }
    }

    fun select(feed: Feed) {
        if (feed == _feed.value) return
        _feed.value = feed
        pager.refresh()
    }

    /** Ekrana her dönüldüğünde bildirim rozeti tazelenir. */
    fun refreshUnread() {
        viewModelScope.launch { notifications.refreshUnread() }
    }

    fun refresh() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retry() = pager.retry()
    fun logout() = auth.logout()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                HomeViewModel(c.authRepository, c.notificationsRepository, c.postsRepository)
            }
        }
    }
}
