package com.alpkcgl.fikirhane.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.model.PublicUser
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val auth: AuthRepository) : ViewModel() {
    val user: StateFlow<PublicUser?> = auth.currentUser

    /** Oturum doğrulanamadıysa (ör. ağ yok) gösterilecek bilgi. */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    init {
        // Kayıtlı bilgiyle hemen gösterilir, ardından sunucudan doğrulanır (rol değişmiş olabilir).
        viewModelScope.launch {
            try {
                auth.refreshUser()
                _notice.value = null
            } catch (e: ApiException) {
                // 401'de AuthRepository oturumu kapatır ve NavHost giriş ekranına döner.
                if (!e.isUnauthorized) _notice.value = e.message
            }
        }
    }

    fun logout() = auth.logout()

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel(appContainer().authRepository) }
        }
    }
}
