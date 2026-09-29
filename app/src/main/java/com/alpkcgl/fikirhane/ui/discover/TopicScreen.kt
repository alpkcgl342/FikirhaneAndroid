package com.alpkcgl.fikirhane.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.data.model.PostQuery
import com.alpkcgl.fikirhane.data.repository.PostsRepository
import com.alpkcgl.fikirhane.ui.appContainer
import com.alpkcgl.fikirhane.ui.navigation.TopicRoute
import com.alpkcgl.fikirhane.ui.posts.PostList
import com.alpkcgl.fikirhane.ui.posts.PostPager
import com.alpkcgl.fikirhane.ui.theme.TextMuted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Kategori (slug) ya da etiket (tag) sayfası. */
class TopicViewModel(
    private val slug: String?,
    private val tag: String?,
    posts: PostsRepository,
) : ViewModel() {
    private val _sort = MutableStateFlow("new")
    val sort: StateFlow<String> = _sort.asStateFlow()

    private val _title = MutableStateFlow(tag?.let { "#$it" } ?: "")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow<String?>(null)
    val description: StateFlow<String?> = _description.asStateFlow()

    val kind: String = if (tag != null) "Etiket" else "Kategori"

    private val pager = PostPager(viewModelScope) { page ->
        posts.list(PostQuery(category = slug, tag = tag, sort = _sort.value), page)
    }
    val list = pager.state

    init {
        pager.refresh()
        if (slug != null) {
            viewModelScope.launch {
                runCatching { posts.categories() }.onSuccess { list ->
                    val category = list.firstOrNull { it.slug == slug }
                    _title.value = category?.name ?: "Kategori bulunamadı"
                    _description.value = category?.description
                }
            }
        }
    }

    fun setSort(value: String) {
        if (value == _sort.value) return
        _sort.value = value
        pager.refresh()
    }

    fun refresh() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retry() = pager.retry()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route = createSavedStateHandle().toRoute<TopicRoute>()
                TopicViewModel(route.slug, route.tag, appContainer().postsRepository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicScreen(
    onBack: () -> Unit,
    onPostClick: (slug: String) -> Unit,
    viewModel: TopicViewModel = viewModel(factory = TopicViewModel.Factory),
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        PostList(
            state = list,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            onPostClick = { onPostClick(it.slug) },
            emptyText = "Burada henüz yayınlanmış bir yazı yok.",
            modifier = Modifier.padding(padding),
            header = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(viewModel.kind.uppercase(), style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    Text(title, style = MaterialTheme.typography.headlineMedium)
                    description?.let { Text(it, color = TextMuted) }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("new" to "En yeniler", "popular" to "Popüler")) { (value, label) ->
                            FilterChip(selected = sort == value, onClick = { viewModel.setSort(value) }, label = { Text(label) })
                        }
                    }
                }
            },
        )
    }
}
