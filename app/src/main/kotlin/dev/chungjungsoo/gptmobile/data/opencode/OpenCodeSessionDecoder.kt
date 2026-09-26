package dev.chungjungsoo.gptmobile.data.opencode

import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenCodeSessionDecoder {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(body: String, statusBody: String?, scope: OpenCodeScope, fetchedAt: Long): List<CachedOpenCodeSession> {
        val sessions = json.decodeFromString<List<SessionDto>>(body)
        require(sessions.map { it.id }.distinct().size == sessions.size) { "Duplicate sessions" }
        val status = statusBody?.let { json.parseToJsonElement(it).jsonObject }
        return sessions.filter { it.directory == scope.directory }.map {
            require(it.id.startsWith("ses") && it.title.length <= 4096)
            val state = if (status == null) {
                "unknown"
            } else {
                val entry = status[it.id]
                if (entry == null) "idle" else entry.jsonObject["type"]?.jsonPrimitive?.content ?: "unknown"
            }
            CachedOpenCodeSession(
                scope.serverId,
                scope.directory,
                it.id,
                scope.profileRevision,
                it.title,
                it.time.updated,
                state.takeIf { s -> s in setOf("idle", "busy", "retry") } ?: "unknown",
                fetchedAt
            )
        }
    }

    @Serializable private data class SessionDto(val id: String, val directory: String, val title: String, val time: TimeDto)

    @Serializable private data class TimeDto(val updated: Long)
}
