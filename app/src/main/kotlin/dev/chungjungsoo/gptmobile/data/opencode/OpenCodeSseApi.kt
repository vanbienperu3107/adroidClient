package dev.chungjungsoo.gptmobile.data.opencode

import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

data class OpenCodeSseEvent(val id: String, val type: String, val sessionId: String?)

/** Legacy `/event` stream. Events only notify callers to reconcile REST state. */
class OpenCodeSseApi(
    private val vault: OpenCodeCredentialVault,
    private val policy: OpenCodeUrlPolicy,
    client: OkHttpClient = OkHttpClient(),
    private val maxFrameBytes: Int = 64_000
) {
    private val http = client.newBuilder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).readTimeout(0, TimeUnit.MILLISECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun stream(profile: OpenCodeServerProfile, directory: String, onEvent: (OpenCodeSseEvent) -> Unit): OpenCodeReadResult = withContext(Dispatchers.IO) {
        val canonical = policy.canonicalize(profile.baseUrl)
        require(canonical == profile.baseUrl)
        val credential = when (val result = vault.load(profile.serverId, profile.credentialRef, canonical)) {
            is VaultResult.Success -> result.value as OpenCodeCredential.Basic
            VaultResult.ReauthenticationRequired -> return@withContext OpenCodeReadResult.ReauthenticationRequired
            VaultResult.Unavailable -> return@withContext OpenCodeReadResult.VaultUnavailable
        }
        val url = "$canonical/".toHttpUrl().newBuilder().addPathSegment("event")
            .addQueryParameter("directory", directory).build()
        val request = Request.Builder().url(url).header("Accept", "text/event-stream")
            .header("Authorization", Credentials.basic(credential.username, credential.password)).build()
        execute(request, onEvent)
    }

    private suspend fun execute(request: Request, onEvent: (OpenCodeSseEvent) -> Unit): OpenCodeReadResult = suspendCancellableCoroutine { continuation ->
        val call = http.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resume(classify(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.use {
                    if (it.code != 200 || it.body?.contentType()?.subtype != "event-stream") {
                        OpenCodeReadResult.HttpFailure(it.code)
                    } else {
                        readFrames(it, onEvent)
                    }
                }
                if (continuation.isActive) continuation.resume(result)
            }
        })
    }

    private fun readFrames(response: Response, onEvent: (OpenCodeSseEvent) -> Unit): OpenCodeReadResult {
        var frameBytes = 0
        response.body?.charStream()?.buffered()?.useLines { lines ->
            lines.forEach { line ->
                frameBytes += line.length
                if (frameBytes > maxFrameBytes) return OpenCodeReadResult.TooLarge
                if (line.isBlank()) {
                    frameBytes = 0
                } else if (line.startsWith("data: ")) {
                    parse(line.removePrefix("data: "))?.let(onEvent)
                }
            }
        }
        return OpenCodeReadResult.NetworkFailure
    }

    private fun parse(data: String): OpenCodeSseEvent? = try {
        val root = json.parseToJsonElement(data).jsonObject
        val id = root["id"]?.jsonPrimitive?.content ?: return null
        val type = root["type"]?.jsonPrimitive?.content ?: return null
        val properties = root["properties"]?.jsonObject
        val sessionId = properties?.get("sessionID")?.jsonPrimitive?.content
            ?: properties?.get("info")?.jsonObject?.get("id")?.jsonPrimitive?.content
            ?: properties?.get("part")?.jsonObject?.get("sessionID")?.jsonPrimitive?.content
        OpenCodeSseEvent(id, type, sessionId)
    } catch (_: Exception) {
        null
    }

    private fun classify(error: IOException): OpenCodeReadResult = when (error) {
        is SocketTimeoutException -> OpenCodeReadResult.Timeout
        is SSLException -> OpenCodeReadResult.TlsFailure
        else -> OpenCodeReadResult.NetworkFailure
    }
}
