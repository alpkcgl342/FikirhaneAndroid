package com.alpkcgl.fikirhane.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.alpkcgl.fikirhane.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.fikirhane.ui.components.Logo
import com.alpkcgl.fikirhane.ui.posts.PostList
import com.alpkcgl.fikirhane.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPostClick: (slug: String) -> Unit,
    onWrite: () -> Unit,
    onMyPosts: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Logo() },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "Menü")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Yazılarım") },
                            onClick = { menuOpen = false; onMyPosts() },
                        )
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
            state = feed,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            onPostClick = { onPostClick(it.slug) },
            emptyText = "Henüz yayınlanmış bir yazı yok. İlk yazıyı sen yaz!",
            modifier = Modifier.padding(padding),
            header = {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    user?.let {
                        Text("Hoş geldin, ${it.displayName}!", color = TextMuted)
                    }
                    Text("Son yazılar", style = MaterialTheme.typography.headlineSmall)
                }
            },
        )
    }
}
