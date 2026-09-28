package dev.chungjungsoo.gptmobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.chungjungsoo.gptmobile.data.opencode.AndroidKeyStoreCredentialVault
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeHealthClient
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeReadApi
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeReadResult
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeUrlPolicy
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeAuthMode
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeConnectionState
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Manual CI-only smoke test. Credentials are supplied by GitHub Actions, never committed. */
@RunWith(AndroidJUnit4::class)
class LiveOpenCodeInstrumentedTest {
    @Test fun healthProjectAndModelCatalogAreReadableWithBasicAuthentication() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val url = arguments.getString("opencode_url").orEmpty()
        val username = arguments.getString("opencode_username").orEmpty()
        val password = arguments.getString("opencode_password").orEmpty()
        assumeTrue("Live OpenCode credentials were not provided", url.isNotBlank() && username.isNotBlank() && password.isNotBlank())

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val vault = AndroidKeyStoreCredentialVault(context)
        val serverId = "ci-${UUID.randomUUID()}"
        val credentialRef = "ci_${UUID.randomUUID()}".replace("-", "")
        val policy = OpenCodeUrlPolicy(allowHttpLoopback = false)
        val canonicalUrl = policy.canonicalize(url)
        val profile = OpenCodeServerProfile(serverId, "CI", canonicalUrl, OpenCodeAuthMode.BASIC, credentialRef, 1)
        try {
            vault.save(serverId, credentialRef, canonicalUrl, OpenCodeCredential.Basic(username, password))
            val health = OpenCodeHealthClient(vault, policy).check(profile)
            assertTrue("OpenCode health was not connected", health is OpenCodeConnectionState.Connected)
            val api = OpenCodeReadApi(vault, policy)
            val projects = api.get(profile, listOf("project"), null)
            assertTrue("OpenCode project catalog was unavailable", projects is OpenCodeReadResult.Success)
            val projectRows = Json.parseToJsonElement((projects as OpenCodeReadResult.Success).json).jsonArray
            val directory = projectRows.firstOrNull()?.jsonObject?.get("worktree")?.jsonPrimitive?.content
            assertTrue("OpenCode did not return a project worktree", !directory.isNullOrBlank())
            val models = api.getWithQuery(profile, listOf("api", "model"), mapOf("location[directory]" to requireNotNull(directory)))
            assertTrue("OpenCode model catalog was unavailable", models is OpenCodeReadResult.Success)
        } finally {
            vault.delete(serverId, credentialRef)
            vault.deleteServer(serverId)
        }
    }
}
