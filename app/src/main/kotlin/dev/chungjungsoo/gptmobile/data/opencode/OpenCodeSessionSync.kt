package dev.chungjungsoo.gptmobile.data.opencode

import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeScope
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Network is outside both locks; commit is ordered with refresh generations and profile mutations. */
class OpenCodeSessionSync(
    private val profiles: OpenCodeProfileRepository,
    private val api: OpenCodeReadApi,
    private val cache: OpenCodeCacheDao
) {
    private val guard = Mutex()
    private val generations = mutableMapOf<Pair<String, String>, Long>()

    suspend fun refresh(profile: OpenCodeServerProfile, directory: String): OpenCodeReadResult {
        val key = profile.serverId to directory
        val generation = guard.withLock { ((generations[key] ?: 0) + 1).also { generations[key] = it } }
        val response = api.get(profile, listOf("session"), directory, 100)
        if (response !is OpenCodeReadResult.Success) return response
        val status = api.get(profile, listOf("session", "status"), directory)
        if (status is OpenCodeReadResult.HttpFailure && status.status in setOf(401, 403)) return status
        val rows = try {
            OpenCodeSessionDecoder().decode(
                response.json,
                (status as? OpenCodeReadResult.Success)?.json,
                OpenCodeScope(profile.serverId, profile.profileRevision, directory),
                System.currentTimeMillis()
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return OpenCodeReadResult.InvalidResponse
        }
        val committed = guard.withLock {
            if (generations[key] != generation) return@withLock false
            profiles.withCurrentProfile(profile) {
                cache.removeOldRevisions(profile.serverId, profile.profileRevision)
                cache.upsertPage(profile.serverId, directory, profile.profileRevision, rows)
            }
        }
        return if (committed) response else OpenCodeReadResult.StaleScope
    }
}
