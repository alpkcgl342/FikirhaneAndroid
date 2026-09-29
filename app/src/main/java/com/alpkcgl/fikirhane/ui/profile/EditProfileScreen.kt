package com.alpkcgl.fikirhane.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.fikirhane.R
import com.alpkcgl.fikirhane.core.AuthValidation
import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.image.ImageException
import com.alpkcgl.fikirhane.data.image.ImagePreparer
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.data.repository.PostsRepository
import com.alpkcgl.fikirhane.data.repository.UsersRepository
import com.alpkcgl.fikirhane.ui.appContainer
import com.alpkcgl.fikirhane.ui.components.AlertBox
import com.alpkcgl.fikirhane.ui.components.Avatar
import com.alpkcgl.fikirhane.ui.components.LoadingButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val displayNameError: String? = null,
    val uploading: Boolean = false,
    val uploadError: String? = null,
    val saving: Boolean = false,
    val alert: String? = null,
    /** Kaydedildi: ekran kapatılır (tek seferlik olay). */
    val saved: Boolean = false,
)

class EditProfileViewModel(
    auth: AuthRepository,
    private val users: UsersRepository,
    private val posts: PostsRepository,
    private val images: ImagePreparer,
) : ViewModel() {
    private val _state = MutableStateFlow(
        auth.currentUser.value.let { user ->
            EditProfileUiState(
                displayName = user?.displayName.orEmpty(),
                bio = user?.bio.orEmpty(),
                avatarUrl = user?.avatarUrl,
            )
        },
    )
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    fun onDisplayName(value: String) = _state.update { it.copy(displayName = value, displayNameError = null) }
    fun onBio(value: String) = _state.update { if (value.length <= 300) it.copy(bio = value) else it }
    fun removeAvatar() = _state.update { it.copy(avatarUrl = null) }

    fun onAvatarPicked(uri: Uri) {
        _state.update { it.copy(uploading = true, uploadError = null) }
        viewModelScope.launch {
            try {
                val url = posts.uploadImage(images.prepare(uri))
                _state.update { it.copy(avatarUrl = url, uploading = false) }
            } catch (e: ImageException) {
                _state.update { it.copy(uploading = false, uploadError = e.message) }
            } catch (e: ApiException) {
                _state.update { it.copy(uploading = false, uploadError = e.message) }
            }
        }
    }

    fun save() {
        val s = _state.value
        if (s.saving || s.uploading) return
        AuthValidation.displayName(s.displayName)?.let { error ->
            _state.update { it.copy(displayNameError = error) }
            return
        }
        _state.update { it.copy(saving = true, alert = null) }
        viewModelScope.launch {
            try {
                users.updateProfile(s.displayName, s.bio, s.avatarUrl)
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: ApiException) {
                _state.update { it.copy(saving = false, alert = e.message) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                EditProfileViewModel(c.authRepository, c.usersRepository, c.postsRepository, c.imagePreparer)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = viewModel(factory = EditProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onAvatarPicked)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Profili düzenle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        val enabled = !state.saving
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            state.alert?.let { AlertBox(it) }

            Text("Profil fotoğrafı", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(state.avatarUrl, state.displayName, size = 72.dp)
                OutlinedButton(
                    onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = enabled && !state.uploading,
                ) { Text("Fotoğraf seç") }
                if (state.avatarUrl != null) {
                    TextButton(onClick = viewModel::removeAvatar, enabled = enabled) { Text("Kaldır") }
                }
                if (state.uploading) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
            state.uploadError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            OutlinedTextField(
                value = state.displayName,
                onValueChange = viewModel::onDisplayName,
                label = { Text("Görünen ad") },
                isError = state.displayNameError != null,
                supportingText = state.displayNameError?.let { { Text(it) } },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.bio,
                onValueChange = viewModel::onBio,
                label = { Text("Biyografi") },
                supportingText = { Text("${state.bio.length}/300") },
                minLines = 3,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            LoadingButton(text = "Kaydet", loading = state.saving, onClick = viewModel::save)
        }
    }
}
