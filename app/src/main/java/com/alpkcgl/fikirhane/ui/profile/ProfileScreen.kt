package com.alpkcgl.fikirhane.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.core.Format
import com.alpkcgl.fikirhane.data.model.Profile
import com.alpkcgl.fikirhane.ui.components.Avatar
import com.alpkcgl.fikirhane.ui.components.ConfirmDialog
import com.alpkcgl.fikirhane.ui.components.ReportDialog
import com.alpkcgl.fikirhane.ui.posts.Badge
import com.alpkcgl.fikirhane.ui.posts.PostList
import com.alpkcgl.fikirhane.ui.theme.Danger
import com.alpkcgl.fikirhane.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onPostClick: (slug: String) -> Unit,
    onEditProfile: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    var confirmBan by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

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
                title = { Text(state.profile?.let { "@${it.username}" } ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Geri")
                    }
                },
                actions = {
                    val profile = state.profile
                    if (profile != null && !profile.isMe) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "Diğer")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Şikâyet et") },
                                onClick = { menuOpen = false; reporting = true },
                            )
                            if (viewModel.isModerator) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (profile.isBanned) "Engeli kaldır" else "Engelle",
                                            color = if (profile.isBanned) MaterialTheme.colorScheme.onSurface else Danger,
                                        )
                                    },
                                    onClick = { menuOpen = false; confirmBan = true },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        val profile = state.profile
        when {
            profile != null -> PostList(
                state = posts,
                onRefresh = { viewModel.load(); viewModel.refreshPosts() },
                onLoadMore = viewModel::loadMore,
                onRetry = viewModel::retry,
                onPostClick = { onPostClick(it.slug) },
                emptyText = if (profile.isMe) "Henüz yayınlanmış yazın yok." else "Henüz yayınlanmış yazısı yok.",
                modifier = Modifier.padding(padding),
                header = {
                    ProfileCard(
                        profile = profile,
                        busy = state.busy,
                        onFollow = viewModel::toggleFollow,
                        onEdit = onEditProfile,
                    )
                },
            )
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Text(if (state.notFound) "Kullanıcı bulunamadı." else state.error.orEmpty(), color = TextMuted)
                if (!state.notFound) OutlinedButton(onClick = viewModel::load) { Text("Tekrar dene") }
            }
        }
    }

    if (reporting) {
        ReportDialog(
            title = "Kullanıcıyı şikâyet et",
            onDismiss = { reporting = false },
            onSubmit = viewModel::report,
            onDone = { reporting = false },
        )
    }
    if (confirmBan) {
        val banned = state.profile?.isBanned == true
        ConfirmDialog(
            title = if (banned) "Engel kaldırılsın mı?" else "Kullanıcı engellensin mi?",
            text = if (banned) "Kullanıcı yeniden giriş yapıp içerik paylaşabilecek."
            else "Engellenen kullanıcı giriş yapamaz ve içerik paylaşamaz.",
            confirmLabel = if (banned) "Engeli kaldır" else "Engelle",
            destructive = !banned,
            onConfirm = viewModel::toggleBan,
            onDismiss = { confirmBan = false },
        )
    }
}

@Composable
private fun ProfileCard(profile: Profile, busy: Boolean, onFollow: () -> Unit, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Avatar(profile.avatarUrl, profile.displayName, size = 88.dp)
            if (profile.isBanned) Badge("Hesap askıya alındı", muted = true)
            Text(profile.displayName, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text("@${profile.username}", color = TextMuted)
            profile.bio?.takeIf { it.isNotBlank() }?.let { Text(it, textAlign = TextAlign.Center) }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Stat(profile.postCount, "yazı")
                Stat(profile.followerCount, "takipçi")
                Stat(profile.followingCount, "takip")
            }
            Text(
                "${Format.date(profile.createdAt).substringAfter(' ')} tarihinde katıldı",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            if (profile.isMe) {
                OutlinedButton(onClick = onEdit) { Text("Profili düzenle") }
            } else if (profile.isFollowing) {
                OutlinedButton(onClick = onFollow, enabled = !busy) { Text("Takibi bırak") }
            } else {
                Button(onClick = onFollow, enabled = !busy) { Text("Takip et") }
            }
        }
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}
