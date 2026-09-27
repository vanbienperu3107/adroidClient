package dev.chungjungsoo.gptmobile.presentation.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.chungjungsoo.gptmobile.data.database.entity.ChatRoom
import dev.chungjungsoo.gptmobile.data.dto.Platform
import dev.chungjungsoo.gptmobile.data.model.ChatStartDestination
import dev.chungjungsoo.gptmobile.data.opencode.CachedOpenCodeSession
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeBrowseRepository
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeProfileRepository
import dev.chungjungsoo.gptmobile.data.repository.ChatRepository
import dev.chungjungsoo.gptmobile.data.repository.SettingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val settingRepository: SettingRepository,
    private val openCode: OpenCodeBrowseRepository,
    private val profiles: OpenCodeProfileRepository
) : ViewModel() {

    data class ChatListState(
        val chats: List<ChatRoom> = listOf(),
        val isSelectionMode: Boolean = false,
        val selected: List<Boolean> = listOf()
    )

    private val _chatListState = MutableStateFlow(ChatListState())
    val chatListState: StateFlow<ChatListState> = _chatListState.asStateFlow()

    private val _platformState = MutableStateFlow(listOf<Platform>())
    val platformState: StateFlow<List<Platform>> = _platformState.asStateFlow()

    private val _chatStartDestination = MutableStateFlow(ChatStartDestination.OPEN_CODE)
    val chatStartDestination: StateFlow<ChatStartDestination> = _chatStartDestination.asStateFlow()

    data class OpenCodeHomeState(
        val serverId: String? = null,
        val directory: String? = null,
        val sessions: List<CachedOpenCodeSession> = emptyList(),
        val stale: Boolean = false,
        val error: String? = null,
        val locked: Boolean = false
    )

    private val _openCodeState = MutableStateFlow(OpenCodeHomeState())
    val openCodeState: StateFlow<OpenCodeHomeState> = _openCodeState.asStateFlow()
    private val _openCodeNavigation = MutableSharedFlow<Triple<String, String, String>>()
    val openCodeNavigation = _openCodeNavigation

    private val _showSelectModelDialog = MutableStateFlow(false)
    val showSelectModelDialog: StateFlow<Boolean> = _showSelectModelDialog.asStateFlow()

    private val _showDeleteWarningDialog = MutableStateFlow(false)
    val showDeleteWarningDialog: StateFlow<Boolean> = _showDeleteWarningDialog.asStateFlow()

    fun updateCheckedState(platform: Platform) {
        val index = _platformState.value.indexOf(platform)

        if (index >= 0) {
            _platformState.update {
                it.mapIndexed { i, p ->
                    if (index == i) {
                        p.copy(selected = p.selected.not())
                    } else {
                        p
                    }
                }
            }
        }
    }

    fun openDeleteWarningDialog() {
        closeSelectModelDialog()
        _showDeleteWarningDialog.update { true }
    }

    fun closeDeleteWarningDialog() {
        _showDeleteWarningDialog.update { false }
    }

    fun openSelectModelDialog() {
        _showSelectModelDialog.update { true }
        disableSelectionMode()
    }

    fun closeSelectModelDialog() {
        _showSelectModelDialog.update { false }
    }

    fun deleteSelectedChats() {
        viewModelScope.launch {
            val selectedChats = _chatListState.value.chats.filterIndexed { index, _ ->
                _chatListState.value.selected[index]
            }

            chatRepository.deleteChats(selectedChats)
            _chatListState.update { it.copy(chats = chatRepository.fetchChatList()) }
            disableSelectionMode()
        }
    }

    fun disableSelectionMode() {
        _chatListState.update {
            it.copy(
                selected = List(it.chats.size) { false },
                isSelectionMode = false
            )
        }
    }

    fun enableSelectionMode() {
        _chatListState.update { it.copy(isSelectionMode = true) }
    }

    fun fetchChats() {
        viewModelScope.launch {
            val chats = chatRepository.fetchChatList()

            _chatListState.update {
                it.copy(
                    chats = chats,
                    selected = List(chats.size) { false },
                    isSelectionMode = false
                )
            }

            Log.d("chats", "${_chatListState.value.chats}")
        }
    }

    fun fetchPlatformStatus() {
        viewModelScope.launch {
            val platforms = settingRepository.fetchPlatforms()
            _platformState.update { platforms }
            _chatStartDestination.value = settingRepository.fetchChatStartDestination()
        }
    }

    fun fetchOpenCodeSessions() {
        viewModelScope.launch {
            try {
                val profile = profiles.profiles().firstOrNull()
                    ?: run {
                        _openCodeState.value = OpenCodeHomeState()
                        return@launch
                    }
                val directory = openCode.defaultDirectory(profile.serverId)
                    ?: run {
                        _openCodeState.value = OpenCodeHomeState(serverId = profile.serverId, error = "No OpenCode project is available")
                        return@launch
                    }
                val sessions = openCode.sessions(profile.serverId, directory)
                _openCodeState.value = OpenCodeHomeState(
                    serverId = profile.serverId,
                    directory = directory,
                    sessions = sessions.sessions,
                    stale = sessions.stale,
                    error = sessions.error,
                    locked = sessions.locked
                )
            } catch (_: Exception) {
                _openCodeState.value = OpenCodeHomeState(error = "Could not load OpenCode sessions")
            }
        }
    }

    fun createOpenCodeSession() {
        viewModelScope.launch {
            val state = _openCodeState.value
            val server = state.serverId ?: return@launch
            val directory = state.directory ?: return@launch
            val session = openCode.createSession(server, directory)
            if (session == null) {
                _openCodeState.value = state.copy(error = "Could not create an OpenCode session")
            } else {
                _openCodeNavigation.emit(Triple(server, directory, session))
            }
        }
    }

    fun selectChat(chatRoomIdx: Int) {
        if (chatRoomIdx < 0 || chatRoomIdx > _chatListState.value.chats.size) return

        _chatListState.update {
            it.copy(
                selected = it.selected.mapIndexed { index, b ->
                    if (index == chatRoomIdx) {
                        !b
                    } else {
                        b
                    }
                }
            )
        }

        if (_chatListState.value.selected.count { it } == 0) {
            disableSelectionMode()
        }
    }
}
