package com.alpkcgl.fikirhane.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.ui.appContainer
import com.alpkcgl.fikirhane.ui.components.AlertKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val loading: Boolean = false,
    val alert: String? = null,
    val alertKind: AlertKind = AlertKind.Error,
    /** E-posta doğrulanmamışsa "tekrar gönder" seçeneği gösterilir. */
    val canResendConfirmation: Boolean = false,
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null) }

    /** Kayıttan sonra (e-posta doğrulaması beklenirken) giriş ekranına bilgi mesajıyla gelinir. */
    fun showInfo(message: String, email: String) =
        _state.update { it.copy(alert = message, alertKind = AlertKind.Success, email = email) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val emailError = if (s.email.isBlank()) "E-posta gerekli" else null
        val passwordError = if (s.password.isEmpty()) "Şifre gerekli" else null
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _state.update { it.copy(loading = true, alert = null, canResendConfirmation = false) }
        viewModelScope.launch {
            try {
                // Başarılı girişte AuthRepository.currentUser değişir; yönlendirmeyi NavHost yapar.
                auth.login(s.email, s.password)
            } catch (e: ApiException) {
                _state.update {
                    it.copy(
                        loading = false,
                        alert = e.message,
                        alertKind = AlertKind.Error,
                        // 403 hem "e-posta doğrulanmamış" hem "hesap askıya alındı" olabilir.
                        canResendConfirmation = e.isForbidden && "doğrula" in e.message,
                    )
                }
            }
        }
    }

    fun resendConfirmation() {
        val email = _state.value.email
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val message = auth.resendConfirmation(email)
                _state.update {
                    it.copy(loading = false, alert = message, alertKind = AlertKind.Success, canResendConfirmation = false)
                }
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, alert = e.message, alertKind = AlertKind.Error) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { LoginViewModel(appContainer().authRepository) }
        }
    }
}
