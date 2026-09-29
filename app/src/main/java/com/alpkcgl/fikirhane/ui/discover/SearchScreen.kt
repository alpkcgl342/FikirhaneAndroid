package com.alpkcgl.fikirhane.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.data.model.Page
import com.alpkcgl.fikirhane.data.model.SearchUser
import com.alpkcgl.fikirhane.data.repository.PostsRepository
import com.alpkcgl.fikirhane.ui.appContainer
import com.alpkcgl.fikirhane.ui.components.Avatar
import com.alpkcgl.fikirhane.ui.posts.PostList
import com.alpkcgl.fikirhane.ui.posts.PostPager
import com.alpkcgl.fikirhane.ui.theme.TextMuted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchViewModel(private val posts: PostsRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Son yapılan arama (boşsa henüz arama yok). */
    private val _submitted = MutableStateFlow("")
    val submitted: StateFlow<String> = _submitted.asStateFlow()

    private val _users = MutableStateFlow<List<SearchUser>>(emptyList())
    val users: StateFlow<List<SearchUser>> = _users.asStateFlow()

    private val _hint = MutableStateFlow<String?>(null)
    val hint: StateFlow<String?> = _hint.asStateFlow()

    private val pager = PostPager(viewModelScope) { page ->
        val q = _submitted.value
        val result = posts.search(q, page)
        // Kişiler yalnızca ilk sayfada döner.
        if (page == 1) _users.value = result.users
        Page(result.items, result.page, result.pageSize, result.total, result.totalPages)
    }
    val list = pager.state

    fun onQueryChange(value: String) {
        _query.value = value
        _hint.value = null
    }

    fun submit() {
        val q = _query.value.trim().replace(Regex("\\s+"), " ")
        if (q.length < 2) {
            _hint.value = "En az 2 karakter yaz"
            return
        }
        _submitted.value = q
        _users.value = emptyList()
        pager.refresh()
    }

    fun loadMore() = pager.loadMore()
    fun retry() = pager.retry()
    fun refresh() = if (_submitted.value.isNotEmpty()) pager.refresh() else Unit

    companion object {
        val Factory = viewModelFactory {
            initializer { SearchViewModel(appContainer().postsRepository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onPostClick: (slug: String) -> Unit,
    onProfile: (username: String) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val submitted by viewModel.submitted.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val hint by viewModel.hint.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { if (submitted.isEmpty()) focus.requestFocus() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Yazı, yazar veya etiket ara…") },
                        singleLine = true,
                        isError = hint != null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); viewModel.submit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .focusRequester(focus),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { keyboard?.hide(); viewModel.submit() }) {
                        Icon(painterResource(R.drawable.ic_search), contentDescription = "Ara")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        if (submitted.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    hint ?: "Yazılarda başlık, içerik, yazar ve etiket; kişilerde ad ve kullanıcı adı üzerinden arar.",
                    color = TextMuted,
                )
            }
            return@Scaffold
        }
        PostList(
            state = list,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            onPostClick = { onPostClick(it.slug) },
            emptyText = if (users.isEmpty()) "\"$submitted\" için sonuç bulunamadı." else "Eşleşen yazı bulunamadı.",
            modifier = Modifier.padding(padding),
            header = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    hint?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (users.isNotEmpty()) {
                        Text("Kişiler", style = MaterialTheme.typography.titleMedium)
                        users.forEach { u ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onProfile(u.username) }
                                    .padding(vertical = 6.dp),
                            ) {
                                Avatar(u.avatarUrl, u.displayName)
                                Column {
                                    Text(u.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text("@${u.username}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                    }
                    if (!list.loading && list.items.isNotEmpty()) {
                        Text("Yazılar", style = MaterialTheme.typography.titleMedium)
                    }
                }
            },
        )
    }
}
