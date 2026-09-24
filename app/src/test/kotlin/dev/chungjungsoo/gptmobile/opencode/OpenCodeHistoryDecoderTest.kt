package dev.chungjungsoo.gptmobile.opencode

import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeHistoryDecoder
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeScope
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeSessionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenCodeHistoryDecoderTest {
    private val key = OpenCodeSessionKey(OpenCodeScope("server-a", 1, "/Project A"), "ses_test")
    private val row = """{"info":{"id":"msg_a","sessionID":"ses_test","role":"assistant"},"parts":[{"id":"prt_a","sessionID":"ses_test","messageID":"msg_a","type":"text","text":"hello"}]}"""

    @Test fun keepsTextAndServerOrder() {
        assertEquals("hello", OpenCodeHistoryDecoder().decode("[$row]", key).messages.single().parts.single().text)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsForeignSession() {
        OpenCodeHistoryDecoder().decode("[${row.replace("ses_test", "ses_other")}]", key)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateMessages() {
        OpenCodeHistoryDecoder().decode("[$row,$row]", key)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsForeignPartParent() {
        OpenCodeHistoryDecoder().decode("[${row.replace("\"messageID\":\"msg_a\"", "\"messageID\":\"msg_b\"")}]", key)
    }

    @Test fun unknownPartDoesNotExposeRawText() {
        val body = "[${row.replace("\"type\":\"text\"", "\"type\":\"future-tool\"")}]"
        assertNull(OpenCodeHistoryDecoder().decode(body, key).messages.single().parts.single().text)
    }

    @Test fun preservesRemoteDirectoryCase() {
        assertNotEquals(key.scope, key.scope.copy(directory = "/project a"))
        assertNotEquals(key.scope, key.scope.copy(serverId = "server-b"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedBodyBeforeParsing() {
        OpenCodeHistoryDecoder(10).decode("[$row]", key)
    }
}
