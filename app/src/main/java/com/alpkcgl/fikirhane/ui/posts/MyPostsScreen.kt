package com.alpkcgl.fikirhane.ui.posts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.data.model.PostQuery
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.data.repository.PostsRepository
import com.alpkcgl.fikirhane.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

enum class MyPostsFilter(val label: String, val empty: String) {
    All("Tümü", "Henüz hiç yazın yok."),
    Drafts("Taslaklar", "Taslak yazın yok."),
    Published("Yayınlananlar", "Henüz yayınlanmış yazın yok."),
}

class MyPostsViewModel(
    private val posts: PostsRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _filter = MutableStateFlow(MyPostsFilter.All)
    val filter: StateFlow<MyPostsFilter> = _filter.asStateFlow()

    // Taslak ve "tümü" sunucuda her zaman isteyenin kendi yazılarıyla sınırlıdır;
    // yayınlananlar herkese açık listeden yazar adıyla filtrelenir.
    private val pager = PostPager(viewModelScope) { page ->
        val query = when (_filter.value) {
            MyPostsFilter.All -> PostQuery(status = "ALL")
            MyPostsFilter.Drafts -> PostQuery(status = "DRAFT")
            MyPostsFilter.Published -> PostQuery(author = auth.currentUser.value?.username)
        }
        posts.list(query, page)
    }
    val list = pager.state

    init {
        pager.refresh()
        viewModelScope.launch { posts.changes.drop(1).collect { pager.refresh() } }
    }

    fun select(filter: MyPostsFilter) {
        if (filter == _filter.value) return
        _filter.value = filter
        pager.refresh()
    }

    fun refresh() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retry() = pager.retry()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                MyPostsViewModel(container.postsRepository, container.authRepository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    onBack: () -> Unit,
    onPostClick: (slug: String) -> Unit,
    viewModel: MyPostsViewModel = viewModel(factory = MyPostsViewModel.Factory),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Yazılarım") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = filter.ordinal, containerColor = MaterialTheme.colorScheme.surface) {
                MyPostsFilter.entries.forEach { f ->
                    Tab(selected = f == filter, onClick = { viewModel.select(f) }, text = { Text(f.label) })
                }
            }
            PostList(
                state = list,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                onRetry = viewModel::retry,
                onPostClick = { onPostClick(it.slug) },
                emptyText = filter.empty,
                showStatus = true,
            )
        }
    }
}
