package dev.chungjungsoo.gptmobile.data.opencode

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

data class OpenCodeDirtyScope(val serverId: String, val directory: String)

/** One process-scoped owner per directory; SSE is only a coalesced REST refresh trigger. */
class OpenCodeReliableSyncCoordinator(private val repository: OpenCodeBrowseRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val owners = ConcurrentHashMap<OpenCodeDirtyScope, Job>()
    private val pendingRefreshes = ConcurrentHashMap<OpenCodeDirtyScope, Job>()
    private val desiredScopes = ConcurrentHashMap.newKeySet<OpenCodeDirtyScope>()

    @Volatile private var background = false
    private val dirty = MutableSharedFlow<OpenCodeDirtyScope>(extraBufferCapacity = 100)
    val refreshes: SharedFlow<OpenCodeDirtyScope> = dirty

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                background = true
                owners.values.forEach { it.cancel() }
                owners.clear()
                pendingRefreshes.values.forEach { it.cancel() }
                pendingRefreshes.clear()
            }

            override fun onStart(owner: LifecycleOwner) {
                background = false
                desiredScopes.forEach { activate(it.serverId, it.directory) }
            }
        })
    }

    fun activate(serverId: String, directory: String) {
        val key = OpenCodeDirtyScope(serverId, directory)
        desiredScopes.add(key)
        if (background) return
        if (owners[key]?.isActive == true) return
        owners[key] = scope.launch {
            val started = System.currentTimeMillis()
            var attempt = 0
            while (true) {
                repository.reconcileScope(serverId, directory)
                markDirty(key) // REST reconciliation always precedes each stream attempt.
                val result = repository.events(serverId, directory) { markDirty(key) }
                if (result is OpenCodeReadResult.ReauthenticationRequired || result is OpenCodeReadResult.VaultUnavailable || (result is OpenCodeReadResult.HttpFailure && result.status in setOf(401, 403))) break
                if (System.currentTimeMillis() - started >= 5 * 60_000) break // REST_ONLY until next activate.
                delay(500) // Debounce duplicate event bursts before the next authoritative refresh.
                val ceiling = min(60_000L, 1_000L shl attempt.coerceAtMost(5))
                delay(Random.nextLong(1_000L, ceiling + 1))
                attempt++
            }
            owners.remove(key)
        }
    }

    fun deactivate(serverId: String, directory: String) {
        val key = OpenCodeDirtyScope(serverId, directory)
        desiredScopes.remove(key)
        owners.remove(key)?.cancel()
        pendingRefreshes.remove(key)?.cancel()
    }

    private fun markDirty(key: OpenCodeDirtyScope) {
        if (background || key !in desiredScopes) return
        if (pendingRefreshes[key]?.isActive == true) return
        pendingRefreshes[key] = scope.launch {
            delay(500)
            dirty.emit(key)
            pendingRefreshes.remove(key)
        }
    }
}
