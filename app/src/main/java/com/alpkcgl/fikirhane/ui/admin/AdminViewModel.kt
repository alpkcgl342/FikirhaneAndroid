package com.alpkcgl.fikirhane.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.model.AdminReport
import com.alpkcgl.fikirhane.data.model.AdminUser
import com.alpkcgl.fikirhane.data.model.ReportStatus
import com.alpkcgl.fikirhane.data.model.ReportTargetType
import com.alpkcgl.fikirhane.data.model.Role
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.data.repository.ModerationRepository
import com.alpkcgl.fikirhane.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminList<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val loading: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

class AdminViewModel(
    private val moderation: ModerationRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    val currentUserId: String? get() = auth.currentUser.value?.id
    val isAdmin: Boolean get() = auth.currentUser.value?.role == Role.ADMIN

    private val _status = MutableStateFlow(ReportStatus.PENDING)
    val status: StateFlow<ReportStatus> = _status.asStateFlow()

    private val _reports = MutableStateFlow(AdminList<AdminReport>())
    val reports: StateFlow<AdminList<AdminReport>> = _reports.asStateFlow()

    private val _userQuery = MutableStateFlow("")
    val userQuery: StateFlow<String> = _userQuery.asStateFlow()

    private val _users = MutableStateFlow(AdminList<AdminUser>())
    val users: StateFlow<AdminList<AdminUser>> = _users.asStateFlow()

    /** Kısa bilgi/hata mesajı (snackbar). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var usersLoaded = false

    init {
        loadReports(reset = true)
    }

    fun onMessageShown() {
        _message.value = null
    }

    // Şikâyetler

    fun selectStatus(status: ReportStatus) {
        if (status == _status.value) return
        _status.value = status
        loadReports(reset = true)
    }

    fun loadReports(reset: Boolean = false) {
        val current = _reports.value
        if (!reset && (current.loading || current.endReached)) return
        val requested = _status.value
        _reports.update { if (reset) AdminList(loading = true) else it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = moderation.reports(requested, if (reset) 1 else current.page + 1)
                if (requested != _status.value) return@launch // Bu arada sekme değişti
                _reports.update {
                    AdminList(
                        items = (if (reset) emptyList() else it.items) + result.items,
                        page = result.page,
                        endReached = result.page >= result.totalPages,
                    )
                }
            } catch (e: ApiException) {
                _reports.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    private fun dropReport(id: String) = _reports.update { it.copy(items = it.items.filterNot { r -> r.id == id }) }

    private fun act(success: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                success?.let { _message.value = it }
            } catch (e: ApiException) {
                _message.value = e.message
            }
        }
    }

    fun resolve(report: AdminReport, status: ReportStatus) = act(null) {
        moderation.resolve(report.id, status)
        dropReport(report.id)
    }

    fun removeTarget(report: AdminReport) = act("İçerik kaldırıldı.") {
        when (report.targetType) {
            ReportTargetType.POST -> moderation.removePost(report.targetId)
            ReportTargetType.COMMENT -> moderation.removeComment(report.targetId)
            ReportTargetType.USER -> Unit
        }
        // Kaldırılan içeriğe ait bekleyen şikâyetler sunucuda çözüldü sayılır.
        dropReport(report.id)
    }

    fun banFromReport(report: AdminReport, userId: String, closesReport: Boolean) = act("Kullanıcı engellendi.") {
        moderation.setBan(userId, true)
        if (closesReport) dropReport(report.id)
    }

    // Kullanıcılar

    fun onUserQueryChange(value: String) {
        _userQuery.value = value
    }

    fun ensureUsersLoaded() {
        if (!usersLoaded) {
            usersLoaded = true
            loadUsers(reset = true)
        }
    }

    fun loadUsers(reset: Boolean = false) {
        val current = _users.value
        if (!reset && (current.loading || current.endReached)) return
        val q = _userQuery.value.trim()
        _users.update { if (reset) AdminList(loading = true) else it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = moderation.users(q, if (reset) 1 else current.page + 1)
                if (q != _userQuery.value.trim()) return@launch
                _users.update {
                    AdminList(
                        items = (if (reset) emptyList() else it.items) + result.items,
                        page = result.page,
                        endReached = result.page >= result.totalPages,
                    )
                }
            } catch (e: ApiException) {
                _users.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    private fun updateUser(id: String, block: (AdminUser) -> AdminUser) =
        _users.update { list -> list.copy(items = list.items.map { if (it.id == id) block(it) else it }) }

    fun toggleBan(user: AdminUser) = act(if (user.isBanned) "Engel kaldırıldı." else "Kullanıcı engellendi.") {
        moderation.setBan(user.id, !user.isBanned)
        updateUser(user.id) { it.copy(isBanned = !user.isBanned) }
    }

    fun setRole(user: AdminUser, role: Role) = act("Rol güncellendi.") {
        moderation.setRole(user.id, role)
        updateUser(user.id) { it.copy(role = role) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                AdminViewModel(c.moderationRepository, c.authRepository)
            }
        }
    }
}
