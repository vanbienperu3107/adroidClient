package dev.chungjungsoo.gptmobile.opencode

import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeCredentialVault
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeHealthClient
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeUrlPolicy
import dev.chungjungsoo.gptmobile.data.opencode.VaultResult
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeAuthMode
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeConnectionState
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenCodeHealthClientTest {

    @Test
    fun rejectsMissingOrWrongHealthFieldTypes() = runBlocking {
        for (body in listOf("{\"version\":\"1\"}", "{\"version\":123,\"healthy\":true}", "{\"version\":\"1\",\"healthy\":\"true\"}")) {
            assertEquals(OpenCodeConnectionState.Incompatible, client(200, body).check(profile))
        }
    }

    @Test
    fun restoredProfileRequiresCredentialsWithoutNetwork() = runBlocking {
        assertEquals(OpenCodeConnectionState.ReauthenticationRequired, client(200, "{}").check(profile.copy(credentialRef = "")))
    }

    @Test
    fun storedNonLoopbackHttpProfileIsRejected() = runBlocking {
        assertEquals(OpenCodeConnectionState.Incompatible, client(200, "{}").check(profile.copy(baseUrl = "http://example.test")))
    }

    @Test
    fun mapsHealthyResponseToConnected() = runBlocking {
        val state = client(200, "{\"healthy\":true,\"version\":\"1.18.30\"}").check(profile)

        assertEquals(OpenCodeConnectionState.Connected("1.18.30"), state)
    }

    @Test
    fun mapsUnauthorizedWithoutDeletingCredential() = runBlocking {
        val state = client(401, "").check(profile)

        assertEquals(OpenCodeConnectionState.Unauthorized, state)
    }

    @Test
    fun mapsHtmlSuccessToIncompatible() = runBlocking {
        val state = client(200, "<html>fallback</html>").check(profile)

        assertEquals(OpenCodeConnectionState.Incompatible, state)
    }

    private fun client(status: Int, body: String): OpenCodeHealthClient {
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(status)
                .message("test")
                .body(body.toResponseBody())
                .build()
        }.build()
        return OpenCodeHealthClient(FakeVault, http, OpenCodeUrlPolicy(allowHttpLoopback = true))
    }

    private object FakeVault : OpenCodeCredentialVault {
        override fun save(serverId: String, reference: String, endpointBinding: String, credential: OpenCodeCredential) = Unit
        override fun load(serverId: String, reference: String, endpointBinding: String) = VaultResult.Success(OpenCodeCredential.Basic("opencode", "not-a-real-password"))
        override fun delete(serverId: String, reference: String) = Unit
        override fun deleteServer(serverId: String) = Unit
    }

    private companion object {
        val profile = OpenCodeServerProfile(
            serverId = "server-a",
            displayName = "Test",
            baseUrl = "https://example.test",
            authMode = OpenCodeAuthMode.BASIC,
            credentialRef = "cred_test",
            profileRevision = 1
        )
    }
}
