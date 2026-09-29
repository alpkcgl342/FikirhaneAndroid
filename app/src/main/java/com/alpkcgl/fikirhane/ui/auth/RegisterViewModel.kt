package com.alpkcgl.fikirhane.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.fikirhane.core.AuthValidation
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.data.repository.RegisterResult
import com.alpkcgl.fikirhane.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RegisterField { DisplayName, Username, Email, Password, PasswordConfirm }

data class RegisterUiState(
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val errors: Map<RegisterField, String> = emptyMap(),
    val loading: Boolean = false,
    val alert: String? = null,
    /** Dolu ise kayıt alındı, e-posta doğrulaması bekleniyor (tek seferlik olay). */
    val confirmationEmail: String? = null,
)

class RegisterViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onChange(field: RegisterField, value: String) = _state.update {
        val v = if (field == RegisterField.Username) value.lowercase() else value
        val next = when (field) {
            RegisterField.DisplayName -> it.copy(displayName = v)
            RegisterField.Username -> it.copy(username = v)
            RegisterField.Email -> it.copy(email = v)
            RegisterField.Password -> it.copy(password = v)
            RegisterField.PasswordConfirm -> it.copy(passwordConfirm = v)
        }
        next.copy(errors = it.errors - field)
    }

    fun onConfirmationHandled() = _state.update { it.copy(confirmationEmail = null) }

    fun submit() {
        val s = _state.value
        if (s.loading) return

        val errors = buildMap {
            AuthValidation.displayName(s.displayName)?.let { put(RegisterField.DisplayName, it) }
            AuthValidation.username(s.username)?.let { put(RegisterField.Username, it) }
            AuthValidation.email(s.email)?.let { put(RegisterField.Email, it) }
            AuthValidation.newPassword(s.password)?.let { put(RegisterField.Password, it) }
            AuthValidation.passwordConfirm(s.password, s.passwordConfirm)
                ?.let { put(RegisterField.PasswordConfirm, it) }
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }

        _state.update { it.copy(loading = true, alert = null) }
        viewModelScope.launch {
            try {
                when (val result = auth.register(s.email, s.password, s.username, s.displayName)) {
                    // Oturum açıldıysa AuthRepository.currentUser değişir; yönlendirmeyi NavHost yapar.
                    is RegisterResult.LoggedIn -> Unit
                    is RegisterResult.ConfirmationRequired ->
                        _state.update { it.copy(loading = false, confirmationEmail = result.email) }
                }
            } catch (e: ApiException) {
                _state.update { current ->
                    // 409: çakışan alanı işaretle (mesaj hangisi olduğunu söyler).
                    val fieldError = when {
                        e.status == 409 && "kullanıcı adı" in e.message -> RegisterField.Username
                        e.status == 409 -> RegisterField.Email
                        else -> null
                    }
                    if (fieldError != null) {
                        current.copy(loading = false, errors = current.errors + (fieldError to e.message))
                    } else {
                        current.copy(loading = false, alert = e.message)
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { RegisterViewModel(appContainer().authRepository) }
        }
    }
}
