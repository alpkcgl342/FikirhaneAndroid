package com.alpkcgl.fikirhane.ui.posts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.alpkcgl.fikirhane.core.Format
import com.alpkcgl.fikirhane.data.model.Author
import com.alpkcgl.fikirhane.data.model.PostStatus
import com.alpkcgl.fikirhane.data.model.PostSummary
import com.alpkcgl.fikirhane.ui.theme.PrimaryHover
import com.alpkcgl.fikirhane.ui.theme.PrimarySoft
import com.alpkcgl.fikirhane.ui.theme.TextMuted

/** Kategori ya da durum rozeti. */
@Composable
fun Badge(text: String, muted: Boolean = false, onClick: (() -> Unit)? = null) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (muted) TextMuted else PrimaryHover,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(if (muted) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else PrimarySoft)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagRow(tags: List<String>, onTagClick: ((String) -> Unit)? = null) {
    if (tags.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            Text(
                "#$tag",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = if (onTagClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onTagClick(tag) }
                        .padding(vertical = 4.dp)
                } else {
                    Modifier
                },
            )
        }
    }
}

/** "Yazar · 29 Eylül 2026 · 3 dk okuma" (+ yayındaysa beğeni/yorum sayıları). */
fun metaLine(author: Author, createdAt: String, readingTime: Int, counts: Pair<Int, Int>? = null): String =
    buildList {
        add(author.displayName)
        add(Format.date(createdAt))
        add("$readingTime dk okuma")
        counts?.let { (likes, comments) -> add("$likes beğeni"); add("$comments yorum") }
    }.joinToString(" · ")

@Composable
fun PostCard(post: PostSummary, onClick: () -> Unit, showStatus: Boolean = false) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        post.coverUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                // Görsel inene kadar boş beyaz alan yerine yumuşak bir zemin görünür.
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(PrimarySoft),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            val draft = showStatus && post.status == PostStatus.DRAFT
            if (draft || post.category != null) {
                FlowRowBadges(draft = draft, category = post.category?.name)
            }
            Text(
                post.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (post.excerpt.isNotBlank()) {
                Text(
                    post.excerpt,
                    color = TextMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                metaLine(
                    post.author,
                    post.createdAt,
                    post.readingTime,
                    counts = if (post.status == PostStatus.PUBLISHED) post.likeCount to post.commentCount else null,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            TagRow(post.tags)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowBadges(draft: Boolean, category: String?) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (draft) Badge("Taslak", muted = true)
        category?.let { Badge(it) }
    }
}

/**
 * Sayfalı yazı listesi: aşağı çekerek yenileme, sona yaklaşınca devamını yükleme,
 * boş ve hata durumları. [header] listenin en üstünde kayar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostList(
    state: PagedPosts,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onPostClick: (PostSummary) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
    showStatus: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    header: (@Composable () -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, state.items.size) {
        if (nearEnd && state.items.isNotEmpty()) onLoadMore()
    }

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            header?.let { item { it() } }
            items(state.items, key = { it.id }) { post ->
                PostCard(post, onClick = { onPostClick(post) }, showStatus = showStatus)
            }
            item {
                ListFooter(state = state, emptyText = emptyText, onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun ListFooter(state: PagedPosts, emptyText: String, onRetry: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        when {
            state.loading || state.loadingMore -> CircularProgressIndicator()
            state.error != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetry) { Text("Tekrar dene") }
            }
            state.items.isEmpty() -> Text(emptyText, color = TextMuted)
        }
    }
}
