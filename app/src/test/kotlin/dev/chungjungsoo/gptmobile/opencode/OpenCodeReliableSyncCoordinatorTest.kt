package dev.chungjungsoo.gptmobile.opencode

import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeDirtyScope
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenCodeReliableSyncCoordinatorTest {
    @Test fun dirtyScopeKeepsServerAndDirectoryIsolation() {
        val scopeA = OpenCodeDirtyScope("server-a", "/workspace/A")
        val scopeB = OpenCodeDirtyScope("server-a", "/workspace/B")
        val scopeC = OpenCodeDirtyScope("server-b", "/workspace/A")
        assertEquals(3, setOf(scopeA, scopeB, scopeC).size)
    }
}
