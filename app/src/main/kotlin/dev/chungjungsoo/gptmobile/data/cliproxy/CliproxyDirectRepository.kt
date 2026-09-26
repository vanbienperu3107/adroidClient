package dev.chungjungsoo.gptmobile.data.cliproxy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeCredentialVault
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeUrlPolicy
import dev.chungjungsoo.gptmobile.data.opencode.VaultResult
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class CliproxyDirectConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val selectedModel: String? = null
)

sealed interface CliproxyResult {
    data class Models(val values: List<String>) : CliproxyResult
    data object Unauthorized : CliproxyResult
    data object InvalidConfiguration : CliproxyResult
    data object NetworkFailure : CliproxyResult
}

/** Direct Cliproxy configuration keeps the API key in Android Keystore, never preferences. */
@Singleton
class CliproxyDirectRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val vault: OpenCodeCredentialVault,
    private val urlPolicy: OpenCodeUrlPolicy,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = client.newBuilder().followRedirects(false).followSslRedirects(false).build()

    suspend fun config(): CliproxyDirectConfig {
        val values = dataStore.data.first()
        return CliproxyDirectConfig(values[ENABLED] ?: false, values[URL].orEmpty(), values[MODEL])
    }

    suspend fun save(baseUrl: String, apiKey: String?, enabled: Boolean, model: String?): CliproxyDirectConfig {
        val canonical = urlPolicy.canonicalize(baseUrl)
        if (!apiKey.isNullOrBlank()) {
            vault.save(SERVER_ID, REFERENCE, canonical, OpenCodeCredential.Basic("cliproxy", apiKey))
        } else if (enabled && credential(canonical) == null) {
            throw IllegalArgumentException("Cliproxy API key is required")
        }
        dataStore.edit {
            it[ENABLED] = enabled
            it[URL] = canonical
            model?.let { selected -> it[MODEL] = selected } ?: it.remove(MODEL)
        }
        return config()
    }

    suspend fun models(): CliproxyResult {
        val config = config()
        val key = credential(config.baseUrl) ?: return CliproxyResult.InvalidConfiguration
        val url = try {
            "${config.baseUrl}/".toHttpUrl().newBuilder().addPathSegment("models").build()
        } catch (_: Exception) {
            return CliproxyResult.InvalidConfiguration
        }
        val request = Request.Builder().url(url).header("Authorization", "Bearer $key").build()
        return try {
            http.newCall(request).execute().use { response ->
                when {
                    response.code in setOf(401, 403) -> CliproxyResult.Unauthorized
                    response.code != 200 -> CliproxyResult.NetworkFailure
                    else -> {
                        val rows = json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject["data"]?.jsonArray.orEmpty()
                        val models = rows.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }.distinct().sorted()
                        CliproxyResult.Models(models)
                    }
                }
            }
        } catch (_: IOException) {
            CliproxyResult.NetworkFailure
        }
    }

    suspend fun apiKey(): String? = credential(config().baseUrl)

    private fun credential(binding: String): String? = when (val result = vault.load(SERVER_ID, REFERENCE, binding)) {
        is VaultResult.Success -> (result.value as? OpenCodeCredential.Basic)?.password
        else -> null
    }

    private companion object {
        const val SERVER_ID = "direct-cliproxy"
        const val REFERENCE = "cliproxy-direct-key"
        val ENABLED = booleanPreferencesKey("cliproxy_direct_enabled")
        val URL = stringPreferencesKey("cliproxy_direct_url")
        val MODEL = stringPreferencesKey("cliproxy_direct_model")
    }
}
