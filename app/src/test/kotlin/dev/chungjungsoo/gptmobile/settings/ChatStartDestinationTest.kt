package dev.chungjungsoo.gptmobile.settings

import dev.chungjungsoo.gptmobile.data.model.ChatStartDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatStartDestinationTest {
    @Test fun missingOrInvalidStoredDestinationDefaultsToOpenCode() {
        assertEquals(ChatStartDestination.OPEN_CODE, ChatStartDestination.fromStored(null))
        assertEquals(ChatStartDestination.OPEN_CODE, ChatStartDestination.fromStored("unexpected"))
    }

    @Test fun directStoredDestinationIsRestored() {
        assertEquals(ChatStartDestination.DIRECT, ChatStartDestination.fromStored("DIRECT"))
    }
}
