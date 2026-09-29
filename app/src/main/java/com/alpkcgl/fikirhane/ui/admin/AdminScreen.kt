package com.alpkcgl.fikirhane.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.core.Format
import com.alpkcgl.fikirhane.data.model.AdminReport
import com.alpkcgl.fikirhane.data.model.AdminUser
import com.alpkcgl.fikirhane.data.model.ReportStatus
import com.alpkcgl.fikirhane.data.model.ReportTargetType
import com.alpkcgl.fikirhane.data.model.Role
import com.alpkcgl.fikirhane.ui.components.Avatar
import com.alpkcgl.fikirhane.ui.components.ConfirmDialog
import com.alpkcgl.fikirhane.ui.posts.Badge
import com.alpkcgl.fikirhane.ui.theme.Danger
import com.alpkcgl.fikirhane.ui.theme.TextMuted
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

private val TYPE_LABELS = mapOf(
    ReportTargetType.POST to "Yazı",
    ReportTargetType.COMMENT to "Yorum",
    ReportTargetType.USER to "Kullanıcı",
)
private val STATUS_LABELS = mapOf(
    ReportStatus.PENDING to "Bekleyen",
    ReportStatus.RESOLVED to "Çözülen",
    ReportStatus.DISMISSED to "Reddedilen",
)
private val ROLE_LABELS = mapOf(Role.USER to "Kullanıcı", Role.MODERATOR to "Moderatör", Role.ADMIN to "Yönetici")

private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
private fun JsonObject.bool(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull == true

/** Onay bekleyen işlem. */
private data class Pending(val title: String, val text: String, val label: String, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onPostClick: (slug: String) -> Unit,
    onProfile: (username: String) -> Unit,
    viewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<Pending?>(null) }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(tab) { if (tab == 1) viewModel.ensureUsersLoaded() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Yönetim") },
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
            PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Şikâyetler") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Kullanıcılar") })
            }
            if (tab == 0) {
                ReportsTab(viewModel, onPostClick, onProfile) { pending = it }
            } else {
                UsersTab(viewModel, onProfile) { pending = it }
            }
        }
    }

    pending?.let { p ->
        ConfirmDialog(
            title = p.title,
            text = p.text,
            confirmLabel = p.label,
            onConfirm = p.action,
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun ReportsTab(
    viewModel: AdminViewModel,
    onPostClick: (String) -> Unit,
    onProfile: (String) -> Unit,
    confirm: (Pending) -> Unit,
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val list by viewModel.reports.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReportStatus.entries) { s ->
                    FilterChip(selected = s == status, onClick = { viewModel.selectStatus(s) }, label = { Text(STATUS_LABELS.getValue(s)) })
                }
            }
        }
        items(list.items, key = { it.id }) { report ->
            ReportCard(report, viewModel, onPostClick, onProfile, confirm)
        }
        item {
            ListFooter(
                loading = list.loading,
                error = list.error,
                empty = list.items.isEmpty(),
                endReached = list.endReached,
                emptyText = "Bu durumda şikâyet yok.",
                onMore = { viewModel.loadReports() },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportCard(
    report: AdminReport,
    viewModel: AdminViewModel,
    onPostClick: (String) -> Unit,
    onProfile: (String) -> Unit,
    confirm: (Pending) -> Unit,
) {
    val t = report.target
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Badge(TYPE_LABELS.getValue(report.targetType))
                Text(
                    "${Format.date(report.createdAt)} · Şikâyet eden: ${report.reporter.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            Text(report.reason, style = MaterialTheme.typography.titleSmall)

            // Hedef içeriğin önizlemesi (düz metin)
            val preview: String
            var open: (() -> Unit)? = null
            var author: JsonObject? = null
            when {
                t == null -> preview = "İçerik artık yok (silinmiş)."
                report.targetType == ReportTargetType.POST -> {
                    author = t.obj("author")
                    preview = "${t.str("title")} — ${author?.str("displayName")}"
                    t.str("slug")?.let { slug -> open = { onPostClick(slug) } }
                }
                report.targetType == ReportTargetType.COMMENT -> {
                    author = t.obj("author")
                    val post = t.obj("post")
                    preview = "“${t.str("content")}”\n${author?.str("displayName")} · ${post?.str("title")}"
                    post?.str("slug")?.let { slug -> open = { onPostClick(slug) } }
                }
                else -> {
                    preview = "${t.str("displayName")} (@${t.str("username")})" + if (t.bool("isBanned")) " · Engelli" else ""
                    t.str("username")?.let { u -> open = { onProfile(u) } }
                }
            }
            Text(
                preview,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(12.dp),
            )

            if (report.status == ReportStatus.PENDING) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    open?.let { TextButton(onClick = it) { Text("Görüntüle") } }
                    if (t != null && report.targetType != ReportTargetType.USER) {
                        TextButton(onClick = {
                            confirm(Pending("İçerik kaldırılsın mı?", "Bu işlem geri alınamaz.", "Kaldır") { viewModel.removeTarget(report) })
                        }) { Text(if (report.targetType == ReportTargetType.POST) "Yazıyı kaldır" else "Yorumu kaldır", color = Danger) }
                    }
                    // Kullanıcı şikâyetinde kişinin kendisi, diğerlerinde içeriğin yazarı engellenir.
                    val person = if (report.targetType == ReportTargetType.USER) t else author
                    val personId = person?.str("id")
                    if (personId != null && !(report.targetType == ReportTargetType.USER && t?.bool("isBanned") == true)) {
                        TextButton(onClick = {
                            confirm(
                                Pending("${person.str("displayName")} engellensin mi?", "Engellenen kullanıcı giriş yapamaz ve içerik paylaşamaz.", "Engelle") {
                                    viewModel.banFromReport(report, personId, closesReport = report.targetType == ReportTargetType.USER)
                                },
                            )
                        }) { Text(if (report.targetType == ReportTargetType.USER) "Engelle" else "Yazarı engelle", color = Danger) }
                    }
                    TextButton(onClick = { viewModel.resolve(report, ReportStatus.RESOLVED) }) { Text("Çözüldü") }
                    TextButton(onClick = { viewModel.resolve(report, ReportStatus.DISMISSED) }) { Text("Reddet") }
                }
            } else {
                Badge(if (report.status == ReportStatus.RESOLVED) "Çözüldü" else "Reddedildi", muted = true)
            }
        }
    }
}

@Composable
private fun UsersTab(viewModel: AdminViewModel, onProfile: (String) -> Unit, confirm: (Pending) -> Unit) {
    val query by viewModel.userQuery.collectAsStateWithLifecycle()
    val list by viewModel.users.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onUserQueryChange,
                placeholder = { Text("Ad, kullanıcı adı veya e-posta") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.loadUsers(reset = true) }) {
                        Icon(painterResource(R.drawable.ic_search), contentDescription = "Ara")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.loadUsers(reset = true) }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        items(list.items, key = { it.id }) { user ->
            UserRow(user, viewModel, onProfile, confirm)
        }
        item {
            ListFooter(
                loading = list.loading,
                error = list.error,
                empty = list.items.isEmpty(),
                endReached = list.endReached,
                emptyText = "Kullanıcı bulunamadı.",
                onMore = { viewModel.loadUsers() },
            )
        }
    }
}

@Composable
private fun UserRow(user: AdminUser, viewModel: AdminViewModel, onProfile: (String) -> Unit, confirm: (Pending) -> Unit) {
    val isSelf = user.id == viewModel.currentUserId
    var roleMenu by remember { mutableStateOf(false) }
    Card(
        onClick = { onProfile(user.username) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Avatar(user.avatarUrl, user.displayName)
            Column(Modifier.weight(1f)) {
                Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                Text("@${user.username} · ${user.email}", style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Badge(ROLE_LABELS.getValue(user.role))
                    if (user.isBanned) Badge("Engelli", muted = true)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                // Rolü yalnızca yönetici, kendisi dışındakiler için değiştirebilir.
                if (viewModel.isAdmin && !isSelf) {
                    Box {
                        TextButton(onClick = { roleMenu = true }) { Text("Rol") }
                        DropdownMenu(expanded = roleMenu, onDismissRequest = { roleMenu = false }) {
                            Role.entries.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(ROLE_LABELS.getValue(role) + if (role == user.role) " ✓" else "") },
                                    onClick = { roleMenu = false; if (role != user.role) viewModel.setRole(user, role) },
                                )
                            }
                        }
                    }
                }
                if (!isSelf && user.role != Role.ADMIN) {
                    if (user.isBanned) {
                        TextButton(onClick = { viewModel.toggleBan(user) }) { Text("Engeli kaldır") }
                    } else {
                        TextButton(onClick = {
                            confirm(Pending("${user.displayName} engellensin mi?", "Engellenen kullanıcı giriş yapamaz ve içerik paylaşamaz.", "Engelle") { viewModel.toggleBan(user) })
                        }) { Text("Engelle", color = Danger) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListFooter(
    loading: Boolean,
    error: String?,
    empty: Boolean,
    endReached: Boolean,
    emptyText: String,
    onMore: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
        when {
            loading -> CircularProgressIndicator()
            error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onMore) { Text("Tekrar dene") }
            }
            empty -> Text(emptyText, color = TextMuted)
            !endReached -> OutlinedButton(onClick = onMore) { Text("Daha fazla") }
        }
    }
}
