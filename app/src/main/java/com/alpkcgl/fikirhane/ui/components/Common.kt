package com.alpkcgl.fikirhane.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.ui.theme.Danger
import com.alpkcgl.fikirhane.ui.theme.DangerSoft
import com.alpkcgl.fikirhane.ui.theme.Primary
import com.alpkcgl.fikirhane.ui.theme.Success
import com.alpkcgl.fikirhane.ui.theme.SuccessSoft

/** Logo + "Fikirhane" yazısı. */
@Composable
fun Logo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Primary),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
        Text("Fikirhane", style = MaterialTheme.typography.titleLarge)
    }
}

enum class AlertKind { Error, Success }

/** Form üstündeki uyarı kutusu; ekran okuyucular değişikliği duyurur. */
@Composable
fun AlertBox(message: String, kind: AlertKind = AlertKind.Error, modifier: Modifier = Modifier) {
    val (bg, fg) = when (kind) {
        AlertKind.Error -> DangerSoft to Danger
        AlertKind.Success -> SuccessSoft to Success
    }
    Text(
        text = message,
        color = fg,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Etiketli metin alanı; hata varsa altında gösterilir. */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = (error ?: supportingText)?.let { text -> { Text(text) } },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Yükleniyor durumunda metni döndürme göstergesiyle değiştiren birincil buton. */
@Composable
fun LoadingButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(text)
        }
    }
}
