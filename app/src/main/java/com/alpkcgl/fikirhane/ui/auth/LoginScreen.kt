package com.alpkcgl.fikirhane.ui.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.fikirhane.ui.components.AlertBox
import com.alpkcgl.fikirhane.ui.components.FormField
import com.alpkcgl.fikirhane.ui.components.LoadingButton

@Composable
fun LoginScreen(
    onRegisterClick: () -> Unit,
    /** Kayıttan gelindiyse gösterilecek bilgi ve e-posta (doğrulama bekleniyor). */
    infoMessage: String? = null,
    prefillEmail: String? = null,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(infoMessage) {
        if (infoMessage != null) viewModel.showInfo(infoMessage, prefillEmail.orEmpty())
    }

    AuthLayout(
        title = "Tekrar hoş geldin",
        subtitle = "Yazmaya ve tartışmaya kaldığın yerden devam et.",
    ) {
        state.alert?.let { AlertBox(it, state.alertKind) }

        FormField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "E-posta",
            error = state.emailError,
            keyboardType = KeyboardType.Email,
            enabled = !state.loading,
        )
        FormField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Şifre",
            error = state.passwordError,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::submit,
            isPassword = true,
            enabled = !state.loading,
        )

        LoadingButton(text = "Giriş yap", loading = state.loading, onClick = viewModel::submit)

        if (state.canResendConfirmation) {
            OutlinedButton(
                onClick = viewModel::resendConfirmation,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Doğrulama e-postasını tekrar gönder")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Hesabın yok mu?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRegisterClick, enabled = !state.loading) { Text("Kayıt ol") }
        }
    }
}
