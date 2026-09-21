package dev.chungjungsoo.gptmobile.data.opencode

import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeConnectionState
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class OpenCodeHealthClient internal constructor(
    private val vault: OpenCodeCredentialVault,
    private val client: OkHttpClient,
    private val urlPolicy: OpenCodeUrlPolicy
) {

    private val json = Json { ignoreUnknownKeys = true }

    @Inject
    constructor(vault: OpenCodeCredentialVault, urlPolicy: OpenCodeUrlPolicy) : this(
        vault,
        OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build(),
        urlPolicy
    )

    suspend fun check(profile: OpenCodeServerProfile): OpenCodeConnectionState = withContext(Dispatchers.IO) {
        when (val result = vault.load(profile.serverId, profile.credentialRef, profile.baseUrl)) {
            is VaultResult.Success -> checkRequest(profile.baseUrl, result.value)

            VaultResult.ReauthenticationRequired -> OpenCodeConnectionState.ReauthenticationRequired
            VaultResult.Unavailable -> OpenCodeConnectionState.VaultUnavailable
        }
    }

    suspend fun checkDraft(baseUrl: String, credential: OpenCodeCredential.Basic): OpenCodeConnectionState = withContext(Dispatchers.IO) {
        try {
            checkRequest(urlPolicy.canonicalize(baseUrl), credential)
        } catch (_: IllegalArgumentException) {
            OpenCodeConnectionState.Incompatible
        }
    }

    private fun checkRequest(baseUrl: String, credential: OpenCodeCredential): OpenCodeConnectionState {
        val request = Request.Builder()
            .url("$baseUrl/global/health".toHttpUrl())
            .header("Authorization", basicHeader(credential))
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> parseHealth(response.body?.string().orEmpty())
                    401 -> OpenCodeConnectionState.Unauthorized
                    403 -> OpenCodeConnectionState.Forbidden
                    404 -> OpenCodeConnectionState.NotFound
                    in 500..599 -> OpenCodeConnectionState.ServerError
                    else -> OpenCodeConnectionState.Incompatible
                }
            }
        } catch (_: SocketTimeoutException) {
            OpenCodeConnectionState.Timeout
        } catch (_: javax.net.ssl.SSLException) {
            OpenCodeConnectionState.TlsFailure
        } catch (_: IOException) {
            OpenCodeConnectionState.Unreachable
        } catch (_: Exception) {
            OpenCodeConnectionState.Incompatible
        }
    }

    private fun parseHealth(body: String): OpenCodeConnectionState = try {
        val health = json.parseToJsonElement(body).jsonObject
        val version = health["version"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            ?: return OpenCodeConnectionState.Incompatible
        if (health["healthy"]?.jsonPrimitive?.booleanOrNull == true) {
            OpenCodeConnectionState.Connected(version)
        } else {
            OpenCodeConnectionState.Unhealthy
        }
    } catch (_: Exception) {
        OpenCodeConnectionState.Incompatible
    }

    private fun basicHeader(credential: OpenCodeCredential): String = when (credential) {
        is OpenCodeCredential.Basic -> okhttp3.Credentials.basic(credential.username, credential.password)
    }
}
