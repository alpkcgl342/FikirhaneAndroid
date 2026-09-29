package com.alpkcgl.fikirhane.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.ui.components.Logo
import com.alpkcgl.fikirhane.ui.posts.PostList
import com.alpkcgl.fikirhane.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPostClick: (slug: String) -> Unit,
    onWrite: () -> Unit,
    onMyPosts: () -> Unit,
    onProfile: (username: String) -> Unit,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onCategory: (slug: String) -> Unit,
    onAdmin: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val unread by viewModel.unreadCount.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshUnread()
        onPauseOrDispose {}
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Logo() },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(painterResource(R.drawable.ic_search), contentDescription = "Ara")
                    }
                    IconButton(onClick = onNotifications) {
                        BadgedBox(badge = { if (unread > 0) Badge { Text(minOf(unread, 99).toString()) } }) {
                            Icon(
                                painterResource(R.drawable.ic_notifications),
                                contentDescription = if (unread > 0) "Bildirimler ($unread okunmamış)" else "Bildirimler",
                            )
                        }
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "Menü")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        user?.let { u ->
                            DropdownMenuItem(
                                text = { Text("Profilim") },
                                onClick = { menuOpen = false; onProfile(u.username) },
                            )
                        }
                        DropdownMenuItem(text = { Text("Yazılarım") }, onClick = { menuOpen = false; onMyPosts() })
                        if (viewModel.isModerator) {
                            DropdownMenuItem(text = { Text("Yönetim") }, onClick = { menuOpen = false; onAdmin() })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Çıkış yap") },
                            onClick = { menuOpen = false; viewModel.logout() },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onWrite,
                icon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) },
                text = { Text("Yaz") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        PostList(
            state = list,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            onPostClick = { onPostClick(it.slug) },
            emptyText = feed.empty,
            modifier = Modifier.padding(padding),
            header = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    user?.let { Text("Hoş geldin, ${it.displayName}!", color = TextMuted) }
                    if (categories.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories, key = { it.id }) { c ->
                                FilterChip(selected = false, onClick = { onCategory(c.slug) }, label = { Text(c.name) })
                            }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Feed.entries) { f ->
                            FilterChip(selected = f == feed, onClick = { viewModel.select(f) }, label = { Text(f.label) })
                        }
                    }
                }
            },
        )
    }
}
