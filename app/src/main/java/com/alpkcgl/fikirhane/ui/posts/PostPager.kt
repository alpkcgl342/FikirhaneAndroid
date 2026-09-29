package com.alpkcgl.fikirhane.ui.posts

import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.model.Page
import com.alpkcgl.fikirhane.data.model.PostSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PagedPosts(
    val items: List<PostSummary> = emptyList(),
    /** İlk sayfa yükleniyor (liste boş). */
    val loading: Boolean = true,
    /** Aşağı çekerek yenileme sürüyor (liste dolu kalır). */
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

/**
 * Sayfalı yazı listesi: ilk sayfa, "aşağı çek yenile" ve liste sonunda devamı.
 * Yenileme sürerken gelen eski sayfa sonuçları yok sayılır.
 */
class PostPager(
    private val scope: CoroutineScope,
    private val load: suspend (page: Int) -> Page<PostSummary>,
) {
    private val _state = MutableStateFlow(PagedPosts())
    val state: StateFlow<PagedPosts> = _state.asStateFlow()

    private var page = 0
    private var job: Job? = null

    /** İlk sayfadan yeniden yükler; liste doluysa "yenileniyor" gösterilir. */
    fun refresh() {
        job?.cancel()
        _state.update {
            it.copy(loading = it.items.isEmpty(), refreshing = it.items.isNotEmpty(), loadingMore = false, error = null)
        }
        job = scope.launch {
            try {
                val result = load(1)
                page = result.page
                _state.value = PagedPosts(
                    items = result.items,
                    loading = false,
                    endReached = result.page >= result.totalPages,
                )
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, refreshing = false, error = e.message) }
            }
        }
    }

    /** Hatadan sonra "Tekrar dene": liste boşsa baştan, doluysa kaldığı sayfadan. */
    fun retry() {
        if (_state.value.items.isEmpty()) {
            refresh()
        } else {
            _state.update { it.copy(error = null) }
            loadMore()
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.refreshing || s.loadingMore || s.endReached || s.error != null) return
        _state.update { it.copy(loadingMore = true) }
        job = scope.launch {
            try {
                val result = load(page + 1)
                page = result.page
                _state.update { current ->
                    // Sayfalar arasında yeni yazı eklendiyse tekrar edenler atılır.
                    val known = current.items.mapTo(HashSet()) { it.id }
                    current.copy(
                        items = current.items + result.items.filter { it.id !in known },
                        loadingMore = false,
                        endReached = result.page >= result.totalPages,
                    )
                }
            } catch (e: ApiException) {
                _state.update { it.copy(loadingMore = false, error = e.message) }
            }
        }
    }
}
