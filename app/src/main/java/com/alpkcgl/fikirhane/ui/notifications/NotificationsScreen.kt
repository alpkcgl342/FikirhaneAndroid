package com.alpkcgl.fikirhane.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.core.Format
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.model.AppNotification
import com.alpkcgl.fikirhane.data.model.NotificationType
import com.alpkcgl.fikirhane.data.repository.NotificationsRepository
import com.alpkcgl.fikirhane.ui.appContainer
import com.alpkcgl.fikirhane.ui.components.Avatar
import com.alpkcgl.fikirhane.ui.theme.Primary
import com.alpkcgl.fikirhane.ui.theme.PrimarySoft
import com.alpkcgl.fikirhane.ui.theme.TextMuted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val items: List<AppNotification> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

class NotificationsViewModel(private val repo: NotificationsRepository) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()
    private var page = 0

    init {
        load(first = true)
    }

    fun load(first: Boolean = false) {
        val s = _state.value
        if (!first && (s.loadingMore || s.endReached)) return
        _state.update { if (first) it.copy(loading = true, error = null) else it.copy(loadingMore = true, error = null) }
        viewModelScope.launch {
            try {
                val result = repo.list(if (first) 1 else page + 1)
                page = result.page
                _state.update {
                    it.copy(
                        items = if (first) result.items else it.items + result.items,
                        loading = false,
                        loadingMore = false,
                        endReached = result.page >= result.totalPages,
                    )
                }
                // İlk sayfa gösterildikten sonra hepsi okundu sayılır (vurgu bu ziyarette kalır).
                if (first && result.unreadCount > 0) runCatching { repo.markAllRead() }
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, loadingMore = false, error = e.message) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { NotificationsViewModel(appContainer().notificationsRepository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onPostClick: (slug: String) -> Unit,
    onProfile: (username: String) -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Bildirimler") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.items.isEmpty() -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                Text(state.error ?: "Henüz bildirimin yok.", color = TextMuted)
                if (state.error != null) OutlinedButton(onClick = { viewModel.load(first = true) }) { Text("Tekrar dene") }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding),
            ) {
                items(state.items, key = { it.id }) { n ->
                    NotificationItem(n) {
                        val post = n.data.post
                        if (n.type == NotificationType.FOLLOW || post == null) onProfile(n.data.actor.username)
                        else onPostClick(post.slug)
                    }
                }
                item {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        when {
                            state.loadingMore -> CircularProgressIndicator()
                            state.error != null -> OutlinedButton(onClick = { viewModel.load() }) { Text("Tekrar dene") }
                            !state.endReached -> OutlinedButton(onClick = { viewModel.load() }) { Text("Daha fazla") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(n: AppNotification, onClick: () -> Unit) {
    val actor = n.data.actor
    val sentence = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(actor.displayName) }
        when {
            n.type == NotificationType.FOLLOW -> append(" seni takip etmeye başladı.")
            n.type == NotificationType.LIKE -> append(" yazını beğendi: ")
            n.data.reply -> append(" yorumuna yanıt verdi: ")
            else -> append(" yazına yorum yaptı: ")
        }
        n.data.post?.takeIf { n.type != NotificationType.FOLLOW }?.let {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(it.title) }
        }
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (n.isRead) MaterialTheme.colorScheme.surface else PrimarySoft),
        border = BorderStroke(1.dp, if (n.isRead) MaterialTheme.colorScheme.outline else Primary),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Avatar(actor.avatarUrl, actor.displayName)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(sentence)
                Text(Format.date(n.createdAt), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}
