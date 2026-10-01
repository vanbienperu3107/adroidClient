package dev.chungjungsoo.gptmobile.chat

import dev.chungjungsoo.gptmobile.presentation.ui.chat.directChatTailIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatScrollIndexTest {
    @Test fun idleDirectChatTargetsTheLastExistingGroup() {
        assertEquals(0, directChatTailIndex(1, isIdle = true))
        assertEquals(2, directChatTailIndex(3, isIdle = true))
    }

    @Test fun activeDirectChatIncludesThePendingUserAndProviderRows() {
        assertEquals(4, directChatTailIndex(3, isIdle = false))
    }

    @Test fun emptyDirectChatNeverRequestsAnInvalidIndex() {
        assertEquals(0, directChatTailIndex(0, isIdle = true))
    }
}
