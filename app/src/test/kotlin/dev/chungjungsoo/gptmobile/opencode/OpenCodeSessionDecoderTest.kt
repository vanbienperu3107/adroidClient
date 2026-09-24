package dev.chungjungsoo.gptmobile.opencode

import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeSessionDecoder
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeScope
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenCodeSessionDecoderTest {
    private val scope = OpenCodeScope("s", 1, "/A")
    private val row = """{"id":"ses_a","directory":"/A","title":"Test","time":{"updated":1}}"""

    @Test fun missingStatusResponseIsUnknownNotIdle() {
        assertEquals("unknown", OpenCodeSessionDecoder().decode("[$row]", null, scope, 1).single().status)
        assertEquals("idle", OpenCodeSessionDecoder().decode("[$row]", "{}", scope, 1).single().status)
    }

    @Test fun unknownServerStatusHasFallback() {
        assertEquals("unknown", OpenCodeSessionDecoder().decode("[$row]", """{"ses_a":{"type":"future"}}""", scope, 1).single().status)
    }

    @Test fun filtersOtherDirectoryWithoutChangingCase() {
        assertEquals(0, OpenCodeSessionDecoder().decode("[${row.replace("/A", "/a")}]", "{}", scope, 1).size)
    }
}
