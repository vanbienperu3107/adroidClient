package dev.chungjungsoo.gptmobile.data.opencode

import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeSessionKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A page is never proof of a complete/consistent snapshot. Callers must not prune from it. */
data class OpenCodeHistoryPage(val messages: List<OpenCodeHistoryMessage>)

data class OpenCodeHistoryMessage(val id: String, val role: String, val parts: List<OpenCodeHistoryPart>)

data class OpenCodeHistoryPart(val id: String, val type: String, val text: String?)

class OpenCodeHistoryDecoder(private val maxChars: Int = 2_000_000) {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(body: String, key: OpenCodeSessionKey): OpenCodeHistoryPage {
        require(body.length <= maxChars) { "History payload exceeds budget" }
        val rows = json.decodeFromString<List<MessageDto>>(body)
        require(rows.map { it.info.id }.distinct().size == rows.size) { "Duplicate message IDs" }
        return OpenCodeHistoryPage(
            rows.map { row ->
                require(row.info.sessionID == key.sessionId) { "History scope mismatch" }
                require(row.info.id.isNotBlank() && row.info.role.isNotBlank()) { "Invalid message" }
                require(row.parts.map { it.id }.distinct().size == row.parts.size) { "Duplicate part IDs" }
                OpenCodeHistoryMessage(
                    row.info.id,
                    row.info.role,
                    row.parts.map { part ->
                        require(part.messageID == row.info.id && part.sessionID == key.sessionId) { "Part ownership mismatch" }
                        require(part.id.isNotBlank() && part.type.isNotBlank()) { "Invalid part" }
                        // Unknown fields/types do not become raw cached payloads or executable content.
                        OpenCodeHistoryPart(part.id, part.type, part.text.takeIf { part.type == "text" || part.type == "reasoning" })
                    }
                )
            }
        )
    }

    @Serializable
    private data class MessageDto(val info: InfoDto, val parts: List<PartDto>)

    @Serializable
    private data class InfoDto(val id: String, val sessionID: String, val role: String)

    @Serializable
    private data class PartDto(val id: String, val sessionID: String, val messageID: String, val type: String, val text: String? = null)
}
