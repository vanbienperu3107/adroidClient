package dev.chungjungsoo.gptmobile.data.opencode

import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeScope
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeSessionKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class OpenCodeBrowseData(
    val projects: List<CachedOpenCodeProject> = emptyList(),
    val sessions: List<CachedOpenCodeSession> = emptyList(),
    val messages: List<OpenCodeHistoryMessage> = emptyList(),
    val pending: Map<String, Int>? = null,
    val stale: Boolean = false,
    val error: String? = null,
    val locked: Boolean = false
)

/** Single writer for browsing; no implicit mutation retries or pruning from partial pages. */
class OpenCodeBrowseRepository(
    private val profiles: OpenCodeProfileRepository,
    private val api: OpenCodeReadApi,
    private val dao: OpenCodeCacheDao
) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private fun blocked(result: OpenCodeReadResult) = result == OpenCodeReadResult.ReauthenticationRequired ||
        result == OpenCodeReadResult.VaultUnavailable ||
        (result is OpenCodeReadResult.HttpFailure && result.status in setOf(401, 403))

    private suspend fun profile(id: String): OpenCodeServerProfile? {
        val all = profiles.profiles()
        for (cachedId in dao.serverIds()) {
            // Deletion races are guarded again at every commit/read below.
            val owner = all.firstOrNull { it.serverId == cachedId }
            if (owner == null) {
                dao.purgeObsolete(cachedId, null)
            } else {
                profiles.withCurrentProfile(owner) { dao.purgeObsolete(cachedId, owner.profileRevision) }
            }
        }
        return all.firstOrNull { it.serverId == id && it.credentialRef.isNotBlank() }
    }

    private suspend fun denied(p: OpenCodeServerProfile): Boolean = dao.access(p.serverId)?.let { it.revision == p.profileRevision && it.denied } == true

    private suspend fun observeAuth(p: OpenCodeServerProfile, result: OpenCodeReadResult): Boolean {
        if (blocked(result)) {
            if (!profiles.withCurrentProfile(p) { dao.setAccess(OpenCodeCacheAccess(p.serverId, p.profileRevision, blocked(result))) }) return true
        }
        return denied(p)
    }

    suspend fun projects(serverId: String): OpenCodeBrowseData = mutex.withLock {
        val p = profile(serverId) ?: return@withLock OpenCodeBrowseData(locked = true)
        val result = api.get(p, listOf("project"), null)
        if (observeAuth(p, result)) return@withLock OpenCodeBrowseData(locked = true)
        if (result is OpenCodeReadResult.Success) {
            val rows = try {
                json.parseToJsonElement(result.json).jsonArray.map { item ->
                    val obj = item.jsonObject
                    val dir = obj.getValue("worktree").jsonPrimitive.content
                    require(dir.isNotBlank())
                    CachedOpenCodeProject(serverId, dir, obj.getValue("id").jsonPrimitive.content, p.profileRevision)
                }.distinctBy { it.directory }
            } catch (_: Exception) {
                return@withLock OpenCodeBrowseData(error = "Invalid project response")
            }
            if (!profiles.withCurrentProfile(p) { dao.upsertProjects(rows) }) return@withLock OpenCodeBrowseData(locked = true)
        }
        var rows = emptyList<CachedOpenCodeProject>()
        if (!profiles.withCurrentProfile(p) { rows = dao.projects(serverId, p.profileRevision) }) return@withLock OpenCodeBrowseData(locked = true)
        OpenCodeBrowseData(projects = rows, stale = result !is OpenCodeReadResult.Success)
    }

    suspend fun sessions(serverId: String, directory: String, limit: Int = 100): OpenCodeBrowseData = mutex.withLock {
        require(limit in 1..10000)
        val p = profile(serverId) ?: return@withLock OpenCodeBrowseData(locked = true)
        val result = api.get(p, listOf("session"), directory, limit)
        if (observeAuth(p, result)) return@withLock OpenCodeBrowseData(locked = true)
        var pending: Map<String, Int>? = null
        if (result is OpenCodeReadResult.Success) {
            val statuses = api.get(p, listOf("session", "status"), directory)
            if (observeAuth(p, statuses)) return@withLock OpenCodeBrowseData(locked = true)
            val rows = try {
                OpenCodeSessionDecoder().decode(result.json, (statuses as? OpenCodeReadResult.Success)?.json, OpenCodeScope(serverId, p.profileRevision, directory), System.currentTimeMillis())
            } catch (_: Exception) {
                return@withLock OpenCodeBrowseData(error = "Invalid session response")
            }
            if (!profiles.withCurrentProfile(p) { dao.upsertPage(serverId, directory, p.profileRevision, rows) }) return@withLock OpenCodeBrowseData(locked = true)
            val permissions = api.get(p, listOf("permission"), directory)
            if (observeAuth(p, permissions)) return@withLock OpenCodeBrowseData(locked = true)
            val questions = api.get(p, listOf("question"), directory)
            if (observeAuth(p, questions)) return@withLock OpenCodeBrowseData(locked = true)
            if (permissions is OpenCodeReadResult.Success && questions is OpenCodeReadResult.Success) {
                pending = try {
                    (json.parseToJsonElement(permissions.json).jsonArray + json.parseToJsonElement(questions.json).jsonArray)
                        .map { it.jsonObject.getValue("sessionID").jsonPrimitive.content }.groupingBy { it }.eachCount()
                } catch (_: Exception) {
                    null
                }
            }
        }
        var rows = emptyList<CachedOpenCodeSession>()
        if (!profiles.withCurrentProfile(p) { rows = dao.sessions(serverId, directory, p.profileRevision).first() }) return@withLock OpenCodeBrowseData(locked = true)
        OpenCodeBrowseData(sessions = rows, pending = pending, stale = result !is OpenCodeReadResult.Success)
    }

    private suspend fun owned(p: OpenCodeServerProfile, directory: String, session: String): Boolean {
        val response = api.get(p, listOf("session", session), directory)
        observeAuth(p, response)
        if (response !is OpenCodeReadResult.Success) return false
        return try {
            val obj = json.parseToJsonElement(response.json).jsonObject
            obj.getValue("directory").jsonPrimitive.content == directory && obj.getValue("id").jsonPrimitive.content == session
        } catch (_: Exception) {
            false
        }
    }

    suspend fun history(serverId: String, directory: String, session: String, before: String? = null): OpenCodeBrowseData = mutex.withLock {
        val p = profile(serverId) ?: return@withLock OpenCodeBrowseData(locked = true)
        // Scope ownership must be verified before reading a server ID from a route.
        val ownership = owned(p, directory, session)
        if (denied(p)) return@withLock OpenCodeBrowseData(locked = true)
        val response = if (ownership) api.get(p, listOf("session", session, "message"), directory, 50, before) else OpenCodeReadResult.NetworkFailure
        if (observeAuth(p, response)) return@withLock OpenCodeBrowseData(locked = true)
        if (response is OpenCodeReadResult.Success) {
            val page = try {
                OpenCodeHistoryDecoder().decode(response.json, OpenCodeSessionKey(OpenCodeScope(serverId, p.profileRevision, directory), session))
            } catch (_: Exception) {
                return@withLock OpenCodeBrowseData(error = "Invalid history response")
            }
            if (!profiles.withCurrentProfile(p) { dao.storeHistory(serverId, directory, session, p.profileRevision, page) }) return@withLock OpenCodeBrowseData(locked = true)
            // Absence in a page is not deletion. Verify cached misses individually.
            // Bound work per refresh to avoid unbounded network fan-out.
            val present = page.messages.map { it.id }.toSet()
            val candidates = dao.messages(serverId, directory, session, p.profileRevision)
                .filter { it.messageId !in present }.take(20)
            for (candidate in candidates) {
                val point = api.get(p, listOf("session", session, "message", candidate.messageId), directory)
                if (observeAuth(p, point)) return@withLock OpenCodeBrowseData(locked = true)
                if (point is OpenCodeReadResult.HttpFailure && point.status == 404 && owned(p, directory, session)) {
                    profiles.withCurrentProfile(p) { dao.deleteMessage(serverId, directory, session, candidate.messageId) }
                }
            }
        }
        var messages = emptyList<OpenCodeHistoryMessage>()
        if (!profiles.withCurrentProfile(p) {
                val rows = dao.messages(serverId, directory, session, p.profileRevision)
                val parts = dao.parts(serverId, directory, session).groupBy { it.messageId }
                messages = rows.map { m -> OpenCodeHistoryMessage(m.messageId, m.role, parts[m.messageId].orEmpty().map { OpenCodeHistoryPart(it.partId, it.type, it.text) }) }
            }
        ) {
            return@withLock OpenCodeBrowseData(locked = true)
        }
        OpenCodeBrowseData(messages = messages, stale = response !is OpenCodeReadResult.Success)
    }

    suspend fun mutate(serverId: String, directory: String, session: String, title: String?): String? = mutex.withLock {
        val p = profile(serverId) ?: return@withLock "Reauthentication required"
        if (!owned(p, directory, session)) return@withLock "Cannot verify session ownership; refresh before retrying"
        var current = false
        profiles.withCurrentProfile(p) { current = true }
        if (!current) return@withLock "Profile changed"
        if (title != null && (title.isBlank() || title.length > 4096)) return@withLock "Invalid title"
        val payload = title?.let { buildJsonObject { put("title", it) }.toString() }
        val result = api.mutate(p, listOf("session", session), directory, if (title == null) "DELETE" else "PATCH", payload)
        observeAuth(p, result)
        if (result !is OpenCodeReadResult.Success) return@withLock "Mutation not confirmed; refresh before deciding to retry"
        if (title == null) {
            if (result.json.trim() != "true") return@withLock "Delete not confirmed; refresh before retrying"
            profiles.withCurrentProfile(p) { dao.deleteSession(serverId, directory, session) }
        }
        null
    }
}
