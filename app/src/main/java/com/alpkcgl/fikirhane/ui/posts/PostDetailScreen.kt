package com.alpkcgl.fikirhane.ui.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.core.Format
import com.alpkcgl.fikirhane.data.model.Comment
import com.alpkcgl.fikirhane.data.model.PostDetail
import com.alpkcgl.fikirhane.data.model.PostStatus
import com.alpkcgl.fikirhane.data.model.ReportTargetType
import com.alpkcgl.fikirhane.ui.components.AlertBox
import com.alpkcgl.fikirhane.ui.components.Avatar
import com.alpkcgl.fikirhane.ui.components.ConfirmDialog
import com.alpkcgl.fikirhane.ui.components.ReportDialog
import com.alpkcgl.fikirhane.ui.theme.Danger
import com.alpkcgl.fikirhane.ui.theme.Primary
import com.alpkcgl.fikirhane.ui.theme.PrimarySoft
import com.alpkcgl.fikirhane.ui.theme.TextMuted
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

/** Şikâyet penceresinin hedefi. */
private data class ReportTarget(val type: ReportTargetType, val id: String, val title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    onBack: () -> Unit,
    onEdit: (slug: String) -> Unit,
    onProfile: (username: String) -> Unit,
    onCategory: (slug: String) -> Unit,
    onTag: (tag: String) -> Unit,
    viewModel: PostDetailViewModel = viewModel(factory = PostDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmModRemove by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Geri")
                    }
                },
                actions = {
                    val post = state.post ?: return@TopAppBar
                    if (post.isOwner) {
                        IconButton(onClick = { onEdit(post.slug) }) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = "Düzenle")
                        }
                    }
                    val published = post.status == PostStatus.PUBLISHED
                    val canModerate = !post.isOwner && viewModel.isModerator
                    if (post.isOwner || (published && !post.isOwner)) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "Diğer")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (post.isOwner) {
                                DropdownMenuItem(
                                    text = { Text("Sil", color = Danger) },
                                    onClick = { menuOpen = false; confirmDelete = true },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Şikâyet et") },
                                    onClick = {
                                        menuOpen = false
                                        reportTarget = ReportTarget(ReportTargetType.POST, post.id, "Yazıyı şikâyet et")
                                    },
                                )
                            }
                            if (canModerate) {
                                DropdownMenuItem(
                                    text = { Text("Kaldır (moderasyon)", color = Danger) },
                                    onClick = { menuOpen = false; confirmModRemove = true },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        val post = state.post
        when {
            post != null -> Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                PostBody(post, state.error, onProfile, onCategory, onTag)
                if (post.status == PostStatus.PUBLISHED) {
                    ActionRow(post, busy = state.actionBusy, onLike = viewModel::toggleLike, onBookmark = viewModel::toggleBookmark)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    CommentsSection(
                        state = state,
                        currentUserId = viewModel.currentUserId,
                        isModerator = viewModel.isModerator,
                        onSend = viewModel::sendComment,
                        onReply = viewModel::replyTo,
                        onReport = { c: Comment ->
                            reportTarget = ReportTarget(ReportTargetType.COMMENT, c.id, "Yorumu şikâyet et")
                        },
                        onRemove = { viewModel.moderatorRemoveComment(it.id) },
                        onRetry = viewModel::retryComments,
                        onProfileClick = onProfile,
                    )
                }
            }
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            ) {
                Text(if (state.notFound) "Yazı bulunamadı." else state.error.orEmpty(), color = TextMuted)
                if (!state.notFound) OutlinedButton(onClick = viewModel::load) { Text("Tekrar dene") }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Yazı silinsin mi?",
            text = "Bu işlem geri alınamaz.",
            confirmLabel = "Sil",
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
    if (confirmModRemove) {
        ConfirmDialog(
            title = "Yazı kaldırılsın mı?",
            text = "Bu yazı moderasyon gereği kalıcı olarak kaldırılacak.",
            confirmLabel = "Kaldır",
            onConfirm = viewModel::moderatorRemovePost,
            onDismiss = { confirmModRemove = false },
        )
    }
    reportTarget?.let { target ->
        ReportDialog(
            title = target.title,
            onDismiss = { reportTarget = null },
            onSubmit = { reason -> viewModel.report(target.type, target.id, reason) },
            onDone = { reportTarget = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PostBody(
    post: PostDetail,
    error: String?,
    onProfile: (String) -> Unit,
    onCategory: (String) -> Unit,
    onTag: (String) -> Unit,
) {
    error?.let { AlertBox(it) }
    post.coverUrl?.let {
        AsyncImage(
            model = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PrimarySoft),
        )
    }
    val draft = post.status == PostStatus.DRAFT
    if (draft || post.category != null) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (draft) Badge("Taslak — yalnızca sen görüyorsun", muted = true)
            post.category?.let { c -> Badge(c.name, onClick = { onCategory(c.slug) }) }
        }
    }
    Text(post.title, style = MaterialTheme.typography.headlineMedium)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onProfile(post.author.username) }
            .padding(vertical = 4.dp),
    ) {
        Avatar(post.author.avatarUrl, post.author.displayName, size = 36.dp)
        Column {
            Text(post.author.displayName, style = MaterialTheme.typography.titleSmall)
            Text(
                "${Format.date(post.createdAt)} · ${post.readingTime} dk okuma",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
    // Ham HTML işlenmez (düz metin kalır); bağlantılar tarayıcıda açılır.
    Markdown(
        content = post.content,
        colors = markdownColor(),
        typography = markdownTypography(),
        imageTransformer = Coil3ImageTransformerImpl,
        modifier = Modifier.fillMaxWidth(),
    )
    if (post.tags.isNotEmpty()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        TagRow(post.tags, onTagClick = onTag)
    }
}

@Composable
private fun ActionRow(post: PostDetail, busy: Boolean, onLike: () -> Unit, onBookmark: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onLike, enabled = !busy) {
            Icon(
                painterResource(if (post.likedByMe) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                contentDescription = null,
                tint = if (post.likedByMe) Primary else TextMuted,
            )
            Text(
                "  ${if (post.likedByMe) "Beğenildi" else "Beğen"} · ${post.likeCount}",
                color = if (post.likedByMe) Primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        OutlinedButton(onClick = onBookmark, enabled = !busy) {
            Icon(
                painterResource(if (post.bookmarkedByMe) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border),
                contentDescription = null,
                tint = if (post.bookmarkedByMe) Primary else TextMuted,
            )
            Text(
                if (post.bookmarkedByMe) "  Kaydedildi" else "  Kaydet",
                color = if (post.bookmarkedByMe) Primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

