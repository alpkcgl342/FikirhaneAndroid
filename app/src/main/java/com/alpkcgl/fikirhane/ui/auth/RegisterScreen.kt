package com.alpkcgl.fikirhane.ui.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.fikirhane.ui.components.AlertBox
import com.alpkcgl.fikirhane.ui.components.FormField
import com.alpkcgl.fikirhane.ui.components.LoadingButton

@Composable
fun RegisterScreen(
    onLoginClick: () -> Unit,
    /** Kayıt alındı ama e-posta doğrulaması gerekiyor: giriş ekranına bilgiyle dönülür. */
    onConfirmationRequired: (email: String) -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.confirmationEmail) {
        state.confirmationEmail?.let {
            viewModel.onConfirmationHandled()
            onConfirmationRequired(it)
        }
    }

    val enabled = !state.loading
    AuthLayout(
        title = "Aramıza katıl",
        subtitle = "Bir hesap oluştur, fikirlerini paylaşmaya başla.",
    ) {
        state.alert?.let { AlertBox(it) }

        FormField(
            value = state.displayName,
            onValueChange = { viewModel.onChange(RegisterField.DisplayName, it) },
            label = "Görünen ad",
            error = state.errors[RegisterField.DisplayName],
            enabled = enabled,
        )
        FormField(
            value = state.username,
            onValueChange = { viewModel.onChange(RegisterField.Username, it) },
            label = "Kullanıcı adı",
            error = state.errors[RegisterField.Username],
            supportingText = "Profilinde @kullaniciadi olarak görünür (e-posta değil)",
            keyboardType = KeyboardType.Ascii,
            enabled = enabled,
        )
        FormField(
            value = state.email,
            onValueChange = { viewModel.onChange(RegisterField.Email, it) },
            label = "E-posta",
            error = state.errors[RegisterField.Email],
            keyboardType = KeyboardType.Email,
            enabled = enabled,
        )
        FormField(
            value = state.password,
            onValueChange = { viewModel.onChange(RegisterField.Password, it) },
            label = "Şifre",
            error = state.errors[RegisterField.Password],
            supportingText = "En az 8 karakter",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            enabled = enabled,
        )
        FormField(
            value = state.passwordConfirm,
            onValueChange = { viewModel.onChange(RegisterField.PasswordConfirm, it) },
            label = "Şifre (tekrar)",
            error = state.errors[RegisterField.PasswordConfirm],
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::submit,
            isPassword = true,
            enabled = enabled,
        )

        LoadingButton(text = "Hesap oluştur", loading = state.loading, onClick = viewModel::submit)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Zaten hesabın var mı?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onLoginClick, enabled = enabled) { Text("Giriş yap") }
        }
    }
}
