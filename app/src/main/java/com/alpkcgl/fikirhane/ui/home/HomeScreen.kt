package com.alpkcgl.fikirhane.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.fikirhane.data.model.PublicUser
import com.alpkcgl.fikirhane.data.model.Role
import com.alpkcgl.fikirhane.ui.components.AlertBox
import com.alpkcgl.fikirhane.ui.components.Logo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Logo() },
                actions = {
                    OutlinedButton(
                        onClick = viewModel::logout,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text("Çıkış yap") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            notice?.let { AlertBox(it) }
            user?.let { WelcomeCard(it) }
            InfoCard(
                title = "Akış çok yakında",
                text = "Yazılar, yorumlar, arama ve bildirimler sonraki adımlarda burada olacak.",
            )
        }
    }
}

@Composable
private fun WelcomeCard(user: PublicUser) {
    InfoCard(title = "Hoş geldin, ${user.displayName}!") {
        Text("@${user.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (user.role) {
            Role.ADMIN -> Text("Rol: Yönetici", color = MaterialTheme.colorScheme.primary)
            Role.MODERATOR -> Text("Rol: Moderatör", color = MaterialTheme.colorScheme.primary)
            Role.USER -> Unit
        }
    }
}

@Composable
private fun InfoCard(title: String, text: String? = null, extra: @Composable () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            text?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            extra()
        }
    }
}
