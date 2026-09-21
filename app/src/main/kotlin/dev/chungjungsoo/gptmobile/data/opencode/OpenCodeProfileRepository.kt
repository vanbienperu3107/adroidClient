package dev.chungjungsoo.gptmobile.data.opencode

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.chungjungsoo.gptmobile.di.OpenCodeProfileStore
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeAuthMode
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface OpenCodeProfileRepository {
    suspend fun profiles(): List<OpenCodeServerProfile>
    suspend fun save(
        existingId: String?,
        displayName: String,
        baseUrl: String,
        credential: OpenCodeCredential.Basic?
    ): OpenCodeServerProfile
    suspend fun delete(serverId: String)
    suspend fun recordHealth(serverId: String, version: String?, checkedAt: Long)
}

@Singleton
class DataStoreOpenCodeProfileRepository internal constructor(
    @OpenCodeProfileStore
    private val dataStore: DataStore<Preferences>,
    private val vault: OpenCodeCredentialVault,
    private val urlPolicy: OpenCodeUrlPolicy,
    private val exportFile: java.io.File
) : OpenCodeProfileRepository {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @OpenCodeProfileStore dataStore: DataStore<Preferences>,
        vault: OpenCodeCredentialVault,
        urlPolicy: OpenCodeUrlPolicy
    ) : this(dataStore, vault, urlPolicy, context.filesDir.resolve("opencode-profile-export.json"))

    private val json = Json { ignoreUnknownKeys = true }

    // All profiles share one document, so mutations across servers must serialize too.
    private val mutationMutex = Mutex()

    override suspend fun profiles(): List<OpenCodeServerProfile> = mutationMutex.withLock { read().map { it.toDomain() } }

    override suspend fun save(
        existingId: String?,
        displayName: String,
        baseUrl: String,
        credential: OpenCodeCredential.Basic?
    ): OpenCodeServerProfile = mutationMutex.withLock {
        require(displayName.isNotBlank()) { "Server name is required" }
        val canonicalUrl = urlPolicy.canonicalize(baseUrl)
        val all = read().toMutableList()
        val index = existingId?.let { id -> all.indexOfFirst { it.serverId == id } } ?: -1
        require(existingId == null || index >= 0) { "Server profile no longer exists" }
        val previous = all.getOrNull(index)
        val serverId = previous?.serverId ?: UUID.randomUUID().toString()
        val bindingChanged = previous?.baseUrl != canonicalUrl || previous?.authMode != OpenCodeAuthMode.BASIC.name
        require(!bindingChanged || credential != null) { "Changing the server endpoint requires a credential" }
        val reference = if (credential == null) {
            require(!previous?.credentialRef.isNullOrBlank()) { "Credential is required" }
            previous.credentialRef
        } else {
            "cred_${UUID.randomUUID().toString().replace("-", "")}"
        }
        if (credential != null) vault.save(serverId, reference, canonicalUrl, credential)
        val updated = StoredProfile(
            serverId = serverId,
            displayName = displayName.trim(),
            baseUrl = canonicalUrl,
            authMode = OpenCodeAuthMode.BASIC.name,
            credentialRef = reference,
            profileRevision = (previous?.profileRevision ?: 0) + 1,
            lastKnownVersion = previous?.lastKnownVersion,
            lastHealthCheckAt = null
        )
        if (index == -1) all += updated else all[index] = updated
        try {
            write(all)
        } catch (error: Exception) {
            if (credential != null) vault.delete(serverId, reference)
            throw error
        }
        if (credential != null && previous != null && previous.credentialRef != reference) {
            vault.delete(serverId, previous.credentialRef)
        }
        runCatching { writeExport(all) }
        updated.toDomain()
    }

    override suspend fun delete(serverId: String): Unit = mutationMutex.withLock {
        val all = read().toMutableList()
        val profile = all.firstOrNull { it.serverId == serverId } ?: return@withLock
        all.remove(profile)
        write(all)
        vault.delete(serverId, profile.credentialRef)
        vault.deleteServer(serverId)
        runCatching { writeExport(all) }
    }

    override suspend fun recordHealth(serverId: String, version: String?, checkedAt: Long): Unit = mutationMutex.withLock {
        val all = read().toMutableList()
        val index = all.indexOfFirst { it.serverId == serverId }
        if (index < 0) return@withLock
        val existing = all[index]
        val updated = existing.copy(lastKnownVersion = version, lastHealthCheckAt = checkedAt)
        all[index] = updated
        write(all)
        runCatching { writeExport(all) }
    }

    private suspend fun read(): List<StoredProfile> {
        dataStore.data.first()[PROFILES_KEY]?.let { return json.decodeFromString(ListSerializer, it) }
        if (!exportFile.exists()) return emptyList()
        val imported = try {
            json.decodeFromString(ExportListSerializer, exportFile.readText()).mapNotNull { exported ->
                if (exported.authMode != OpenCodeAuthMode.BASIC.name) return@mapNotNull null
                val canonicalUrl = try {
                    urlPolicy.canonicalize(exported.baseUrl)
                } catch (_: IllegalArgumentException) {
                    return@mapNotNull null
                }
                StoredProfile(exported.serverId, exported.displayName, canonicalUrl, exported.authMode, "", exported.profileRevision)
            }
        } catch (_: Exception) {
            emptyList()
        }
        if (imported.isNotEmpty()) write(imported)
        return imported
    }

    private suspend fun write(profiles: List<StoredProfile>) {
        dataStore.edit { it[PROFILES_KEY] = json.encodeToString(ListSerializer, profiles) }
    }

    private fun writeExport(profiles: List<StoredProfile>) {
        val export = profiles.map { ExportProfile(it.serverId, it.displayName, it.baseUrl, it.authMode, it.profileRevision) }
        val staging = exportFile.resolveSibling("${exportFile.name}.staging")
        staging.writeText(json.encodeToString(ExportListSerializer, export))
        try {
            Files.move(staging.toPath(), exportFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(staging.toPath(), exportFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    @Serializable
    private data class StoredProfile(
        val serverId: String,
        val displayName: String,
        val baseUrl: String,
        val authMode: String,
        val credentialRef: String,
        val profileRevision: Long,
        val lastKnownVersion: String? = null,
        val lastHealthCheckAt: Long? = null
    ) {
        fun toDomain() = OpenCodeServerProfile(
            serverId,
            displayName,
            baseUrl,
            OpenCodeAuthMode.valueOf(authMode),
            credentialRef,
            profileRevision,
            lastKnownVersion,
            lastHealthCheckAt
        )
    }

    @Serializable
    private data class ExportProfile(
        val serverId: String,
        val displayName: String,
        val baseUrl: String,
        val authMode: String,
        val profileRevision: Long
    )

    private companion object {
        val PROFILES_KEY = stringPreferencesKey("opencode_profiles_v1")
        val ListSerializer = kotlinx.serialization.builtins.ListSerializer(StoredProfile.serializer())
        val ExportListSerializer = kotlinx.serialization.builtins.ListSerializer(ExportProfile.serializer())
    }
}
