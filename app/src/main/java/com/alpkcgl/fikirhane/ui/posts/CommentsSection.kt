package com.alpkcgl.fikirhane.ui.posts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.core.Format
import com.alpkcgl.fikirhane.data.model.Comment
import com.alpkcgl.fikirhane.ui.components.Avatar
import com.alpkcgl.fikirhane.ui.components.ConfirmDialog
import com.alpkcgl.fikirhane.ui.theme.Danger
import com.alpkcgl.fikirhane.ui.theme.TextMuted
import kotlinx.coroutines.launch

/** Bu derinlikten sonra yanıtlar daha fazla içeri kaydırılmaz (dar ekranda okunabilirlik). */
private const val MAX_INDENT_DEPTH = 3

internal data class CommentNode(val comment: Comment, val replies: List<CommentNode>)

/** parentId ile düz listeyi ağaca çevirir; üst yorumu bulunamayanlar köke eklenir. */
internal fun buildCommentTree(comments: List<Comment>): List<CommentNode> {
    val ids = comments.mapTo(HashSet()) { it.id }
    val children = comments.groupBy { c -> c.parentId?.takeIf { it in ids } }
    fun build(parent: String?): List<CommentNode> =
        children[parent].orEmpty().map { CommentNode(it, build(it.id)) }
    return build(null)
}

@Composable
fun CommentsSection(
    state: PostDetailUiState,
    currentUserId: String?,
    isModerator: Boolean,
    onSend: suspend (String) -> Boolean,
    onReply: (Comment?) -> Unit,
    onReport: (Comment) -> Unit,
    onRemove: (Comment) -> Unit,
    onRetry: () -> Unit,
    onProfileClick: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var toRemove by remember { mutableStateOf<Comment?>(null) }
    val scope = rememberCoroutineScope()
    val count = state.post?.commentCount ?: state.comments.size

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Yorumlar ($count)", style = MaterialTheme.typography.headlineSmall)

        state.replyTo?.let { target ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${target.author.displayName} kişisine yanıt veriyorsun",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onReply(null) }) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Yanıtı iptal et")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(if (state.replyTo != null) "Yanıtını yaz…" else "Düşüncelerini paylaş…") },
                isError = state.commentError != null,
                supportingText = state.commentError?.let { { Text(it) } },
                maxLines = 6,
                enabled = !state.sendingComment,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                enabled = !state.sendingComment,
                onClick = { scope.launch { if (onSend(text)) text = "" } },
            ) {
                if (state.sendingComment) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(
                        painterResource(R.drawable.ic_send),
                        contentDescription = "Gönder",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        when {
            state.commentsLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            state.commentsError != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.commentsError, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetry) { Text("Tekrar dene") }
            }
            state.comments.isEmpty() -> Text("Henüz yorum yok. İlk yorumu sen yap!", color = TextMuted)
            else -> buildCommentTree(state.comments).forEach { node ->
                CommentItem(
                    node = node,
                    depth = 0,
                    currentUserId = currentUserId,
                    isModerator = isModerator,
                    onReply = onReply,
                    onReport = onReport,
                    onRemove = { toRemove = it },
                    onProfileClick = onProfileClick,
                )
            }
        }
    }

    toRemove?.let { comment ->
        ConfirmDialog(
            title = "Yorum kaldırılsın mı?",
            text = "Bu yorum ve tüm yanıtları kaldırılacak.",
            confirmLabel = "Kaldır",
            onConfirm = { onRemove(comment) },
            onDismiss = { toRemove = null },
        )
    }
}

@Composable
private fun CommentItem(
    node: CommentNode,
    depth: Int,
    currentUserId: String?,
    isModerator: Boolean,
    onReply: (Comment) -> Unit,
    onReport: (Comment) -> Unit,
    onRemove: (Comment) -> Unit,
    onProfileClick: (String) -> Unit,
) {
    val c = node.comment
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (depth in 1..MAX_INDENT_DEPTH) 16.dp else 0.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clickable { onProfileClick(c.author.username) },
        ) {
            Avatar(c.author.avatarUrl, c.author.displayName, size = 28.dp)
            Text(c.author.displayName, style = MaterialTheme.typography.titleSmall)
            Text(Format.date(c.createdAt), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        // Yorumlar düz metindir; biçimlendirme ya da HTML işlenmez.
        Text(c.content, modifier = Modifier.padding(start = 36.dp))
        Row(modifier = Modifier.padding(start = 24.dp)) {
            TextButton(onClick = { onReply(c) }) { Text("Yanıtla") }
            if (c.author.id != currentUserId) {
                TextButton(onClick = { onReport(c) }) { Text("Şikâyet et", color = TextMuted) }
            }
            if (isModerator) {
                TextButton(onClick = { onRemove(c) }) { Text("Kaldır", color = Danger) }
            }
        }
        node.replies.forEach {
            CommentItem(it, depth + 1, currentUserId, isModerator, onReply, onReport, onRemove, onProfileClick)
        }
    }
}
