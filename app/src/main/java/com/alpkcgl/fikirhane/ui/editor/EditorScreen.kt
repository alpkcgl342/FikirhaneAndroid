package com.alpkcgl.fikirhane.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.data.model.PostStatus
import com.alpkcgl.fikirhane.ui.components.AlertBox
import com.alpkcgl.fikirhane.ui.theme.TextMuted
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    onPublished: (slug: String) -> Unit,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }

    LaunchedEffect(state.publishedSlug) {
        state.publishedSlug?.let {
            viewModel.onPublishedHandled()
            onPublished(it)
        }
    }

    val leave = { if (state.dirty) confirmLeave = true else onBack() }
    BackHandler(enabled = state.dirty) { confirmLeave = true }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onCoverPicked)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (state.post == null) "Yeni yazı" else "Yazıyı düzenle")
                        state.post?.let {
                            Text(
                                if (it.status == PostStatus.PUBLISHED) "Yayında" else "Taslak",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            state.alert?.let { AlertBox(it, state.alertKind) }
            val enabled = !state.busy && !state.readOnly

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Başlık") },
                isError = EditorField.Title in state.errors,
                supportingText = state.errors[EditorField.Title]?.let { { Text(it) } },
                textStyle = MaterialTheme.typography.titleLarge,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )

            CategoryPicker(state, enabled, viewModel::onCategoryChange)

            OutlinedTextField(
                value = state.tagsText,
                onValueChange = viewModel::onTagsChange,
                label = { Text("Etiketler") },
                placeholder = { Text("ör. yapay zeka, eğitim") },
                isError = EditorField.Tags in state.errors,
                supportingText = { Text(state.errors[EditorField.Tags] ?: "Virgülle ayır; en fazla 5 etiket.") },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )

            CoverSection(
                state = state,
                enabled = enabled,
                onPick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = viewModel::removeCover,
            )

            ContentSection(state, enabled, viewModel::onContentChange)

            Text("Tahmini okuma süresi: ${state.readingTime} dk", style = MaterialTheme.typography.bodySmall, color = TextMuted)

            SaveButtons(state, enabled, onSave = viewModel::save)
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Değişiklikler kaydedilmedi") },
            text = { Text("Çıkarsan kaydedilmemiş değişikliklerin kaybolacak.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onBack() }) { Text("Çık") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Düzenlemeye devam et") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(state: EditorUiState, enabled: Boolean, onChange: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.categories.firstOrNull { it.id == state.categoryId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "Kategori seç (isteğe bağlı)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategori") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Kategori yok") }, onClick = { onChange(null); expanded = false })
            state.categories.forEach { category ->
                DropdownMenuItem(text = { Text(category.name) }, onClick = { onChange(category.id); expanded = false })
            }
        }
    }
}

@Composable
private fun CoverSection(state: EditorUiState, enabled: Boolean, onPick: () -> Unit, onRemove: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Kapak görseli", style = MaterialTheme.typography.titleSmall)
        state.coverUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = "Kapak görseli önizlemesi",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onPick, enabled = enabled) {
                Text(if (state.coverUrl == null) "Görsel seç" else "Değiştir")
            }
            if (state.coverUrl != null) {
                TextButton(onClick = onRemove, enabled = enabled) { Text("Kaldır") }
            }
            if (state.uploading) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        }
        state.uploadError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ContentSection(state: EditorUiState, enabled: Boolean, onChange: (String) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Yaz") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Önizleme") })
        }
        if (tab == 0) {
            OutlinedTextField(
                value = state.content,
                onValueChange = onChange,
                label = { Text("İçerik (Markdown)") },
                placeholder = { Text("# Başlık\n\n**Kalın**, _italik_, [bağlantı](https://…), listeler ve `kod`…") },
                isError = EditorField.Content in state.errors,
                supportingText = state.errors[EditorField.Content]?.let { { Text(it) } },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                minLines = 12,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (state.content.isBlank()) {
            Text("Önizleme burada görünecek", color = TextMuted, modifier = Modifier.padding(vertical = 24.dp))
        } else {
            Markdown(
                content = state.content,
                colors = markdownColor(),
                typography = markdownTypography(),
                imageTransformer = Coil3ImageTransformerImpl,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SaveButtons(state: EditorUiState, enabled: Boolean, onSave: (PostStatus) -> Unit) {
    val published = state.post?.status == PostStatus.PUBLISHED
    val draftLabel = when {
        state.post == null -> "Taslak olarak kaydet"
        published -> "Yayından kaldır"
        else -> "Taslağı kaydet"
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { onSave(PostStatus.DRAFT) },
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) { Text(draftLabel) }
        Button(
            onClick = { onSave(PostStatus.PUBLISHED) },
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            if (state.saving) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text(if (published) "Güncelle" else "Yayınla")
            }
        }
    }
}
