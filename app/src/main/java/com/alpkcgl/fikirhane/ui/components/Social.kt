package com.alpkcgl.fikirhane.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.ui.theme.Danger
import com.alpkcgl.fikirhane.ui.theme.PrimaryHover
import com.alpkcgl.fikirhane.ui.theme.PrimarySoft
import com.alpkcgl.fikirhane.ui.theme.TextMuted
import kotlinx.coroutines.launch

/** Profil fotoğrafı; yoksa görünen adın baş harfi. */
@Composable
fun Avatar(avatarUrl: String?, name: String, size: Dp = 40.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(PrimarySoft),
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Text(
                text = name.trim().take(1).uppercase(java.util.Locale.forLanguageTag("tr")).ifEmpty { "?" },
                color = PrimaryHover,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.45f).sp,
            )
        }
    }
}

/**
 * Şikâyet penceresi. [onSubmit] başarılı olursa pencere kapanır ve [onDone] çağrılır;
 * sunucu hatası (ör. "zaten şikâyet ettiniz") pencerede gösterilir.
 */
@Composable
fun ReportDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: suspend (reason: String) -> Unit,
    onDone: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Şikâyetin moderatörler tarafından incelenir. Kim olduğun şikâyet edilen kişiye gösterilmez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 1000) reason = it; error = null },
                    label = { Text("Neden?") },
                    placeholder = { Text("ör. hakaret, spam, yanıltıcı içerik…") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    minLines = 3,
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sending,
                onClick = {
                    if (reason.trim().length < 5) {
                        error = "Lütfen nedenini en az 5 karakterle yaz"
                        return@TextButton
                    }
                    sending = true
                    scope.launch {
                        try {
                            onSubmit(reason)
                            onDone()
                        } catch (e: ApiException) {
                            error = e.message
                        } finally {
                            sending = false
                        }
                    }
                },
            ) { Text("Şikâyet et", color = Danger) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !sending) { Text("Vazgeç") } },
    )
}

/** Basit onay penceresi (silme, engelleme…). */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text(confirmLabel, color = if (destructive) Danger else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
