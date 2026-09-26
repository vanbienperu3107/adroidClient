package dev.chungjungsoo.gptmobile.presentation.ui.opencode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeBrowseData
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeBrowseRepository
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeProfileRepository
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeReliableSyncCoordinator
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@HiltViewModel
class OpenCodeBrowseViewModel @Inject constructor(
    private val repository: OpenCodeBrowseRepository,
    private val sync: OpenCodeReliableSyncCoordinator,
    private val profiles: OpenCodeProfileRepository,
    private val state: SavedStateHandle
) : ViewModel() {
    private val serverId = requireNotNull(state.get<String>("serverId"))
    var directory: String? = state["selectedDirectory"]
        private set
    var sessionId: String? = state["selectedSession"]
        private set
    private val _data = MutableStateFlow(OpenCodeBrowseData())
    val data = _data.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private var loadJob: Job? = null
    private var actionJob: Job? = null
    private var generation = 0L
    private var sessionLimit = 100

    fun moreSessions() {
        sessionLimit = (sessionLimit + 100).coerceAtMost(10000)
        refresh()
    }

    init {
        refresh()
        directory?.let { sync.activate(serverId, it) }
        viewModelScope.launch {
            sync.refreshes.collect { dirty ->
                if (dirty.serverId == serverId && dirty.directory == directory && actionJob?.isActive != true) refresh(fromSync = true)
            }
        }
        viewModelScope.launch {
            var initialized = false
            var previous: Long? = null
            profiles.revisions().collect { revisions ->
                val next = revisions[serverId]
                if (initialized && next != previous) {
                    loadJob?.cancel()
                    actionJob?.cancel()
                    directory?.let { sync.deactivate(serverId, it) }
                    ++generation
                    _data.value = OpenCodeBrowseData(locked = true)
                    _loading.value = false
                }
                initialized = true
                previous = next
            }
        }
    }

    fun project(path: String) {
        directory?.let { sync.deactivate(serverId, it) }
        directory = path
        sessionId = null
        sessionLimit = 100
        refresh()
        sync.activate(serverId, path)
    }
    fun session(id: String) {
        sessionId = id
        refresh()
        sync.activate(serverId, directory ?: return)
    }
    fun up(): Boolean {
        if (sessionId != null) {
            sessionId = null
        } else if (directory != null) {
            sync.deactivate(serverId, requireNotNull(directory))
            directory = null
        } else {
            return false
        }
        refresh()
        return true
    }
    fun refresh(before: String? = null, fromSync: Boolean = false) {
        // Only navigation identifiers, never credential or message contents.
        state["selectedDirectory"] = directory
        state["selectedSession"] = sessionId
        loadJob?.cancel()
        val current = ++generation
        _loading.value = true
        if (!fromSync) _data.value = OpenCodeBrowseData()
        val dir = directory
        val session = sessionId
        loadJob = viewModelScope.launch {
            try {
                val result = when {
                    dir == null -> repository.projects(serverId)
                    session == null -> repository.sessions(serverId, dir, sessionLimit)
                    else -> repository.history(serverId, dir, session, before)
                }
                if (current == generation) _data.value = result
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (current == generation) _data.value = OpenCodeBrowseData(error = "Could not load OpenCode data")
            } finally {
                if (current == generation) _loading.value = false
            }
        }
    }
    fun mutate(id: String, title: String?) {
        if (_loading.value) return
        val dir = directory ?: return
        _loading.value = true
        val current = ++generation
        actionJob = viewModelScope.launch {
            try {
                val error = repository.mutate(serverId, dir, id, title)
                if (current != generation) return@launch
                if (error == null) refresh() else _data.value = _data.value.copy(error = error)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (current == generation) _data.value = _data.value.copy(error = "Result uncertain; refresh before retrying")
            } finally {
                if (current == generation) _loading.value = false
            }
        }
    }
    fun prompt(content: String) {
        if (_loading.value) return
        val dir = directory ?: return
        val session = sessionId ?: return
        _loading.value = true
        val current = ++generation
        actionJob = viewModelScope.launch {
            try {
                val error = repository.prompt(serverId, dir, session, content)
                if (current != generation) return@launch
                if (error == null) refresh() else _data.value = _data.value.copy(error = error)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (current == generation) _data.value = _data.value.copy(error = "Prompt outcome uncertain; refresh before sending again")
            } finally {
                if (current == generation) _loading.value = false
            }
        }
    }
    fun abort() {
        if (_loading.value) return
        val dir = directory ?: return
        val session = sessionId ?: return
        _loading.value = true
        val current = ++generation
        actionJob = viewModelScope.launch {
            try {
                val error = repository.abort(serverId, dir, session)
                if (current != generation) return@launch
                if (error == null) refresh() else _data.value = _data.value.copy(error = error)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (current == generation) _data.value = _data.value.copy(error = "Abort outcome uncertain; refresh session status")
            } finally {
                if (current == generation) _loading.value = false
            }
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        actionJob?.cancel()
        directory?.let { sync.deactivate(serverId, it) }
        super.onCleared()
    }
}
