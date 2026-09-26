package dev.chungjungsoo.gptmobile.opencode

import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeUrlPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenCodeUrlPolicyTest {

    private val releasePolicy = OpenCodeUrlPolicy(allowHttpLoopback = false)
    private val debugPolicy = OpenCodeUrlPolicy(allowHttpLoopback = true)

    @Test
    fun canonicalizesHttpsAndRetainsPathPrefix() {
        assertEquals(
            "https://example.test/api/opencode",
            releasePolicy.canonicalize("https://EXAMPLE.test:443/api/opencode/")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsHttpForRelease() {
        releasePolicy.canonicalize("http://127.0.0.1:14096")
    }

    @Test
    fun acceptsHttpLoopbackForDebugOnly() {
        assertEquals("http://10.0.2.2:14096", debugPolicy.canonicalize("http://10.0.2.2:14096/"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsHttpNonLoopbackForDebug() {
        debugPolicy.canonicalize("http://example.test")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmbeddedCredentialsQueryAndFragment() {
        releasePolicy.canonicalize("https://user:password@example.test/?token=secret#fragment")
    }
}
