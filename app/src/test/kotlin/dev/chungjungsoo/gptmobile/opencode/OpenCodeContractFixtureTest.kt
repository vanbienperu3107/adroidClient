package dev.chungjungsoo.gptmobile.opencode

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract-spike fixture tests (WI-00c/WI-00g evidence).
 *
 * These tests validate that the redacted fixtures captured from
 * OpenCode 1.18.30 stay parseable and keep the schema shape the
 * future data/opencode layer will rely on. They intentionally do
 * NOT talk to a live server.
 */
class OpenCodeContractFixtureTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun fixture(name: String): File {
        // Unit tests run with module dir or repo root as workdir depending on invoker.
        val candidates = listOf(
            File("../docs/features/00-contract-spike/fixtures/$name"),
            File("docs/features/00-contract-spike/fixtures/$name")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Fixture not found: $name (cwd=${File(".").absolutePath})")
    }

    @Test
    fun healthFixtureHasVersionAndHealthyFlag() {
        val root = json.parseToJsonElement(fixture("rest/health.json").readText()).jsonObject
        val response = root.getValue("response").jsonObject
        assertEquals(true, response.getValue("healthy").jsonPrimitive.content.toBoolean())
        assertTrue(response.getValue("version").jsonPrimitive.content.isNotBlank())
    }

    @Test
    fun sessionCreateFixtureKeepsDirectoryAndIdShape() {
        val root = json.parseToJsonElement(fixture("rest/session-create.json").readText()).jsonObject
        val response = root.getValue("response").jsonObject
        assertTrue(response.getValue("id").jsonPrimitive.content.startsWith("ses_"))
        // directory is the project scope key confirmed by the spike (project-scope-matrix.md)
        assertTrue(response.containsKey("directory"))
        assertTrue(response.containsKey("projectID"))
        assertTrue(response.containsKey("title"))
    }

    @Test
    fun statusFixtureOnlyListsNonIdleSessions() {
        val root = json.parseToJsonElement(fixture("rest/session-status-busy.json").readText()).jsonObject
        val busy = root.getValue("response_busy").jsonObject
        assertEquals(1, busy.size)
        val entry = busy.values.first().jsonObject
        assertEquals("busy", entry.getValue("type").jsonPrimitive.content)
        // Empty map == everything idle (endpoint-matrix.md finding #3)
        val allIdle = root.getValue("response_all_idle").jsonObject
        assertTrue(allIdle.isEmpty())
    }

    @Test
    fun messageHistoryFixtureKeepsAbortEvidenceAndParentLink() {
        val root = json.parseToJsonElement(
            fixture("rest/message-history-after-abort.json").readText()
        ).jsonObject
        val messages = root.getValue("response").jsonArray
        assertEquals(2, messages.size)

        val user = messages[0].jsonObject.getValue("info").jsonObject
        val assistant = messages[1].jsonObject.getValue("info").jsonObject

        assertEquals("user", user.getValue("role").jsonPrimitive.content)
        assertEquals("assistant", assistant.getValue("role").jsonPrimitive.content)
        // parentID links assistant -> user message (prompt-correlation.md)
        assertEquals(
            user.getValue("id").jsonPrimitive.content,
            assistant.getValue("parentID").jsonPrimitive.content
        )
        // Abort evidence survives in history (endpoint-matrix.md finding #4)
        val error = assistant.getValue("error").jsonObject
        assertEquals("MessageAbortedError", error.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun sseFixtureFramesAreValidJsonWithTypeEnvelope() {
        val lines = fixture("sse/event-stream-sample.txt").readLines()
        val dataLines = lines.filter { it.startsWith("data: ") }
        assertTrue("expected at least 5 SSE frames", dataLines.size >= 5)

        val types = dataLines.map { line ->
            json.parseToJsonElement(line.removePrefix("data: ")).jsonObject
                .getValue("type").jsonPrimitive.content
        }
        // Observed catalog from the spike (sse-event-catalog.md)
        assertTrue(types.contains("server.connected"))
        assertTrue(types.contains("message.updated"))
        assertTrue(types.contains("session.status"))
        assertTrue(types.contains("session.idle"))
        assertFalse(types.any { it.isBlank() })
    }

    @Test
    fun fixturesContainNoSecrets() {
        val forbidden = listOf("Authorization:", "Bearer ey", "spike-test-pw", "password\"")
        val fixtureDirCandidates = listOf(
            File("../docs/features/00-contract-spike/fixtures"),
            File("docs/features/00-contract-spike/fixtures")
        )
        val dir = fixtureDirCandidates.firstOrNull { it.exists() } ?: error("fixtures dir missing")
        dir.walkTopDown().filter { it.isFile }.forEach { file ->
            val text = file.readText()
            forbidden.forEach { needle ->
                assertFalse("secret-like string '$needle' in ${file.name}", text.contains(needle))
            }
        }
    }
}
