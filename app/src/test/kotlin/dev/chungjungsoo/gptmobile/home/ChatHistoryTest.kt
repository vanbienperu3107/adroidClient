package dev.chungjungsoo.gptmobile.home

import dev.chungjungsoo.gptmobile.presentation.ui.home.homeHistory
import dev.chungjungsoo.gptmobile.presentation.ui.home.matchingTitles
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatHistoryTest {
    @Test fun searchIsCaseInsensitiveAndOnlyUsesTheProvidedTitle() {
        val values = listOf("Release notes", "SSE backoff", "release audit")
        assertEquals(listOf("Release notes", "release audit"), values.matchingTitles("RELEASE") { it })
    }

    @Test fun homeHistoryIsCappedAtFiveWithoutReordering() {
        val values = (1..7).toList()
        assertEquals(listOf(1, 2, 3, 4, 5), values.homeHistory())
    }
}
