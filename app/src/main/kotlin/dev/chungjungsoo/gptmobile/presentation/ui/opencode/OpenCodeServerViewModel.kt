package dev.chungjungsoo.gptmobile.presentation.ui.opencode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeHealthClient
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeProfileRepository
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeConnectionState
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

@HiltViewModel
class OpenCodeServerViewModel @Inject constructor(
    private val repository: OpenCodeProfileRepository,
    private val healthClient: OpenCodeHealthClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val editingId = savedStateHandle.get<String>("serverId")?.takeUnless { it == "new" }
    private val _profiles = MutableStateFlow<List<OpenCodeServerProfile>>(emptyList())
    val profiles = _profiles.asStateFlow()
    private val _editingProfile = MutableStateFlow<OpenCodeServerProfile?>(null)
    val editingProfile = _editingProfile.asStateFlow()
    private val _state = MutableStateFlow<OpenCodeConnectionState>(OpenCodeConnectionState.NotChecked)
    val state = _state.asStateFlow()
    private val _formError = MutableStateFlow<String?>(null)
    val formError = _formError.asStateFlow()
    private val requestGeneration = AtomicLong(0)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _profiles.value = repository.profiles()
        _editingProfile.value = _profiles.value.firstOrNull { it.serverId == editingId }
    }

    fun save(name: String, url: String, username: String, password: String, onDone: () -> Unit) = viewModelScope.launch {
        try {
            val generation = requestGeneration.incrementAndGet()
            val existing = _editingProfile.value
            val credential = if (password.isBlank()) null else OpenCodeCredential.Basic(username, password)
            val profile = repository.save(existing?.serverId, name, url, credential)
            refresh()
            val result = healthClient.check(profile)
            if (requestGeneration.get() != generation) return@launch
            _state.value = result
            recordHealth(profile, result)
            onDone()
        } catch (error: IllegalArgumentException) {
            _formError.value = error.message
        }
    }

    fun test(name: String, url: String, username: String, password: String) = viewModelScope.launch {
        try {
            val generation = requestGeneration.incrementAndGet()
            val existing = _editingProfile.value
            val result = if (password.isBlank() && existing != null && existing.baseUrl == url) {
                healthClient.check(existing)
            } else if (password.isNotBlank()) {
                healthClient.checkDraft(url, OpenCodeCredential.Basic(username, password))
            } else {
                OpenCodeConnectionState.ReauthenticationRequired
            }
            if (requestGeneration.get() == generation) {
                _state.value = result
                existing?.let { recordHealth(it, result) }
            }
        } catch (error: IllegalArgumentException) {
            _formError.value = error.message
        }
    }

    fun delete(profile: OpenCodeServerProfile) = viewModelScope.launch {
        requestGeneration.incrementAndGet()
        repository.delete(profile.serverId)
        refresh()
    }

    private suspend fun recordHealth(profile: OpenCodeServerProfile, result: OpenCodeConnectionState) {
        repository.recordHealth(
            profile.serverId,
            (result as? OpenCodeConnectionState.Connected)?.version,
            System.currentTimeMillis()
        )
        _profiles.value = repository.profiles()
        _editingProfile.value = _profiles.value.firstOrNull { it.serverId == editingId }
    }
}
