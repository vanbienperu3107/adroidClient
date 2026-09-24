package dev.chungjungsoo.gptmobile.opencode

import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeCredentialVault
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeReadApi
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeReadResult
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeUrlPolicy
import dev.chungjungsoo.gptmobile.data.opencode.VaultResult
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeAuthMode
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenCodeReadApiTest {
    private val profile = OpenCodeServerProfile("s", "test", "https://example.test/prefix", OpenCodeAuthMode.BASIC, "ref", 1)
    private val vault = object : OpenCodeCredentialVault {
        override fun save(serverId: String, reference: String, endpointBinding: String, credential: OpenCodeCredential) = Unit
        override fun load(serverId: String, reference: String, endpointBinding: String) = VaultResult.Success(OpenCodeCredential.Basic("test", "synthetic"))
        override fun delete(serverId: String, reference: String) = Unit
        override fun deleteServer(serverId: String) = Unit
    }

    @Test fun preservesPrefixAndEncodesDirectoryWithoutChangingCase() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor {
            assertEquals("/prefix/session", it.request().url.encodedPath)
            assertEquals("/Project A/日本", it.request().url.queryParameter("directory"))
            Response.Builder().request(it.request()).protocol(Protocol.HTTP_1_1).code(200).message("test")
                .body("[]".toResponseBody("application/json".toMediaType())).build()
        }.build()
        assertEquals(OpenCodeReadResult.Success("[]"), OpenCodeReadApi(vault, OpenCodeUrlPolicy(false), client).get(profile, listOf("session"), "/Project A/日本"))
    }

    @Test fun rejectsHtmlFallbackAndOversizedPayload() = runBlocking {
        for ((type, body, expected) in listOf(
            Triple("text/html", "<html>", OpenCodeReadResult.InvalidResponse),
            Triple("application/json", "[123456789]", OpenCodeReadResult.TooLarge)
        )) {
            val client = OkHttpClient.Builder().addInterceptor {
                Response.Builder().request(it.request()).protocol(Protocol.HTTP_1_1).code(200).message("test")
                    .body(body.toResponseBody(type.toMediaType())).build()
            }.build()
            assertEquals(expected, OpenCodeReadApi(vault, OpenCodeUrlPolicy(false), client, 5).get(profile, listOf("session"), null))
        }
    }

    @Test fun unauthorizedIsNotAnEmptySuccessfulSnapshot() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor {
            Response.Builder().request(it.request()).protocol(Protocol.HTTP_1_1).code(401).message("test")
                .body("[]".toResponseBody("application/json".toMediaType())).build()
        }.build()
        assertEquals(OpenCodeReadResult.HttpFailure(401), OpenCodeReadApi(vault, OpenCodeUrlPolicy(false), client).get(profile, listOf("session"), null))
    }
}
