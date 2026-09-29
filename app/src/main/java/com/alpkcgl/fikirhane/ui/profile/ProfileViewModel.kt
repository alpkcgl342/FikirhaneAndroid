package com.alpkcgl.fikirhane.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.model.PostQuery
import com.alpkcgl.fikirhane.data.model.Profile
import com.alpkcgl.fikirhane.data.model.ReportTargetType
import com.alpkcgl.fikirhane.data.model.Role
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.data.repository.ModerationRepository
import com.alpkcgl.fikirhane.data.repository.PostsRepository
import com.alpkcgl.fikirhane.data.repository.UsersRepository
import com.alpkcgl.fikirhane.ui.appContainer
import com.alpkcgl.fikirhane.ui.navigation.ProfileRoute
import com.alpkcgl.fikirhane.ui.posts.PagedPosts
import com.alpkcgl.fikirhane.ui.posts.PostPager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: Profile? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val notFound: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
)

class ProfileViewModel(
    private val username: String,
    private val users: UsersRepository,
    private val moderation: ModerationRepository,
    private val auth: AuthRepository,
    postsRepository: PostsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val pager = PostPager(viewModelScope) { page -> postsRepository.list(PostQuery(author = username), page) }
    val posts: StateFlow<PagedPosts> = pager.state

    val isModerator: Boolean
        get() = auth.currentUser.value?.role.let { it == Role.MODERATOR || it == Role.ADMIN }

    init {
        load()
        pager.refresh()
        viewModelScope.launch { postsRepository.changes.drop(1).collect { pager.refresh() } }
        // Profil düzenlenince (kendi profilimizse) güncel bilgi gösterilir.
        viewModelScope.launch { auth.currentUser.drop(1).collect { if (_state.value.profile?.isMe == true) load() } }
    }

    fun load() {
        _state.update { it.copy(loading = it.profile == null, error = null) }
        viewModelScope.launch {
            try {
                val profile = users.profile(username)
                _state.update { it.copy(profile = profile, loading = false) }
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, error = e.message, notFound = e.status == 404) }
            }
        }
    }

    fun toggleFollow() = action { profile ->
        val result = users.toggleFollow(profile.id)
        _state.update {
            it.copy(profile = profile.copy(isFollowing = result.following, followerCount = result.followerCount))
        }
    }

    /** Moderatör: kullanıcıyı engeller ya da engelini kaldırır. */
    fun toggleBan() = action { profile ->
        val banned = !profile.isBanned
        moderation.setBan(profile.id, banned)
        _state.update {
            it.copy(
                profile = profile.copy(isBanned = banned),
                message = if (banned) "Kullanıcı engellendi." else "Engel kaldırıldı.",
            )
        }
    }

    suspend fun report(reason: String) {
        val profile = _state.value.profile ?: return
        moderation.report(ReportTargetType.USER, profile.id, reason)
        _state.update { it.copy(message = "Şikâyetin alındı, teşekkürler.") }
    }

    private fun action(block: suspend (Profile) -> Unit) {
        val profile = _state.value.profile ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                block(profile)
            } catch (e: ApiException) {
                _state.update { it.copy(message = e.message) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun onMessageShown() = _state.update { it.copy(message = null) }
    fun refreshPosts() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retry() = pager.retry()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route = createSavedStateHandle().toRoute<ProfileRoute>()
                val c = appContainer()
                ProfileViewModel(route.username, c.usersRepository, c.moderationRepository, c.authRepository, c.postsRepository)
            }
        }
    }
}
