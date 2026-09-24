package dev.chungjungsoo.gptmobile.data.opencode

import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

sealed interface OpenCodeReadResult {
    data class Success(val json: String) : OpenCodeReadResult
    data class HttpFailure(val status: Int) : OpenCodeReadResult
    data object ReauthenticationRequired : OpenCodeReadResult
    data object VaultUnavailable : OpenCodeReadResult
    data object InvalidResponse : OpenCodeReadResult
    data object TooLarge : OpenCodeReadResult
    data object NetworkFailure : OpenCodeReadResult
    data object Timeout : OpenCodeReadResult
    data object TlsFailure : OpenCodeReadResult
    data object StaleScope : OpenCodeReadResult
}

/** Read-only transport. Mutation and persistence require a scope/revision coordinator. */
class OpenCodeReadApi(
    private val vault: OpenCodeCredentialVault,
    private val policy: OpenCodeUrlPolicy,
    client: OkHttpClient = OkHttpClient(),
    private val maxBytes: Int = 2_000_000
) {
    private val http = client.newBuilder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).callTimeout(30, TimeUnit.SECONDS).build()

    init {
        require(maxBytes > 0)
    }

    suspend fun get(profile: OpenCodeServerProfile, segments: List<String>, directory: String?, limit: Int? = null, before: String? = null): OpenCodeReadResult = request(profile, segments, directory, "GET", null, limit, before)

    /** Call only after verifying session ownership. Mutations are never retried. */
    suspend fun mutate(profile: OpenCodeServerProfile, segments: List<String>, directory: String, method: String, json: String? = null): OpenCodeReadResult {
        require(method == "PATCH" || method == "DELETE")
        return request(profile, segments, directory, method, json, null, null)
    }

    private suspend fun request(profile: OpenCodeServerProfile, segments: List<String>, directory: String?, method: String, payload: String?, limit: Int?, before: String?): OpenCodeReadResult = withContext(Dispatchers.IO) {
        require(segments.isNotEmpty() && segments.all { it.isNotBlank() && it != "." && it != ".." && '/' !in it && '\\' !in it })
        require(limit == null || limit > 0)
        val canonical = policy.canonicalize(profile.baseUrl)
        require(canonical == profile.baseUrl)
        if (profile.credentialRef.isBlank()) return@withContext OpenCodeReadResult.ReauthenticationRequired
        val credential = when (val result = vault.load(profile.serverId, profile.credentialRef, canonical)) {
            is VaultResult.Success -> result.value as OpenCodeCredential.Basic
            VaultResult.ReauthenticationRequired -> return@withContext OpenCodeReadResult.ReauthenticationRequired
            VaultResult.Unavailable -> return@withContext OpenCodeReadResult.VaultUnavailable
        }
        val url = "$canonical/".toHttpUrl().newBuilder().apply {
            segments.forEach { addPathSegment(it) }
            directory?.let { addQueryParameter("directory", it) }
            limit?.let { addQueryParameter("limit", it.toString()) }
            before?.let { addQueryParameter("before", it) }
        }.build()
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .header("Authorization", Credentials.basic(credential.username, credential.password))
            .method(method, payload?.toRequestBody("application/json".toMediaType())).build()
        execute(request)
    }

    private suspend fun execute(request: Request): OpenCodeReadResult = suspendCancellableCoroutine { continuation ->
        val call = http.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(classify(e))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    continuation.resume(response.use { readResponse(it) })
                } catch (error: IOException) {
                    continuation.resume(classify(error))
                } catch (error: Exception) {
                    continuation.resumeWithException(error)
                }
            }
        })
    }

    private fun classify(error: IOException): OpenCodeReadResult = when (error) {
        is SocketTimeoutException -> OpenCodeReadResult.Timeout
        is SSLException -> OpenCodeReadResult.TlsFailure
        else -> OpenCodeReadResult.NetworkFailure
    }

    private fun readResponse(response: Response): OpenCodeReadResult {
        if (response.code != 200) return OpenCodeReadResult.HttpFailure(response.code)
        val body = response.body ?: return OpenCodeReadResult.InvalidResponse
        if (body.contentType()?.subtype != "json") return OpenCodeReadResult.InvalidResponse
        if (body.contentLength() > maxBytes) return OpenCodeReadResult.TooLarge
        val bytes = ByteArrayOutputStream()
        body.byteStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (bytes.size() + count > maxBytes) return OpenCodeReadResult.TooLarge
                bytes.write(buffer, 0, count)
            }
        }
        return OpenCodeReadResult.Success(bytes.toString(Charsets.UTF_8.name()))
    }
}
