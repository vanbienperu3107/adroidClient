package dev.chungjungsoo.gptmobile.data.opencode

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import kotlinx.coroutines.CancellationException
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
    suspend fun recordHealth(serverId: String, version: String?, checkedAt: Long, expectedRevision: Long)
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
        if (credential != null) {
            // Persist cleanup intent BEFORE the external vault write. A killed process
            // must never leave an untracked credential behind.
            queueCleanup(Cleanup(serverId, reference, previous == null))
            vault.save(serverId, reference, canonicalUrl, credential)
        }
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
        dataStore.edit { preferences ->
            val cleanup = cleanupEntries(preferences).filterNot { it.reference == reference }.toMutableList()
            if (credential != null && previous != null && previous.credentialRef.isNotBlank()) {
                cleanup += Cleanup(serverId, previous.credentialRef, false)
            }
            preferences[PROFILES_KEY] = json.encodeToString(ListSerializer, all)
            preferences[CLEANUP_KEY] = json.encodeToString(CleanupSerializer, cleanup)
            preferences[EXPORT_PENDING] = true
        }
        recover(all)
        updated.toDomain()
    }

    override suspend fun delete(serverId: String): Unit = mutationMutex.withLock {
        val all = read().toMutableList()
        val profile = all.firstOrNull { it.serverId == serverId } ?: return@withLock
        all.remove(profile)
        // Removal and the tombstone are one DataStore transaction. Subsequent
        // readers cannot use this profile even if vault cleanup fails.
        dataStore.edit { preferences ->
            val cleanup = cleanupEntries(preferences) + Cleanup(serverId, profile.credentialRef, true)
            preferences[PROFILES_KEY] = json.encodeToString(ListSerializer, all)
            preferences[CLEANUP_KEY] = json.encodeToString(CleanupSerializer, cleanup)
            preferences[EXPORT_PENDING] = true
        }
        recover(all)
    }

    override suspend fun recordHealth(serverId: String, version: String?, checkedAt: Long, expectedRevision: Long): Unit = mutationMutex.withLock {
        val all = read().toMutableList()
        val index = all.indexOfFirst { it.serverId == serverId }
        if (index < 0) return@withLock
        val existing = all[index]
        if (existing.profileRevision != expectedRevision) return@withLock
        val updated = existing.copy(lastKnownVersion = version, lastHealthCheckAt = checkedAt)
        all[index] = updated
        write(all)
        recover(all)
    }

    private suspend fun read(): List<StoredProfile> {
        dataStore.data.first()[PROFILES_KEY]?.let {
            val profiles = json.decodeFromString(ListSerializer, it)
            recover(profiles)
            return profiles
        }
        // Cleanup must also run when the process died during the first create.
        recover(emptyList())
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
        dataStore.edit {
            it[PROFILES_KEY] = json.encodeToString(ListSerializer, profiles)
            it[EXPORT_PENDING] = true
        }
    }

    private fun cleanupEntries(preferences: Preferences): List<Cleanup> = preferences[CLEANUP_KEY]?.let { json.decodeFromString(CleanupSerializer, it) } ?: emptyList()

    private suspend fun queueCleanup(entry: Cleanup) {
        dataStore.edit { it[CLEANUP_KEY] = json.encodeToString(CleanupSerializer, cleanupEntries(it) + entry) }
    }

    private suspend fun recover(profiles: List<StoredProfile>) {
        for (entry in cleanupEntries(dataStore.data.first())) {
            if (profiles.any { it.credentialRef == entry.reference && entry.reference.isNotBlank() }) continue
            try {
                if (entry.reference.isNotBlank()) vault.delete(entry.serverId, entry.reference)
                if (entry.deleteKey && profiles.none { it.serverId == entry.serverId }) vault.deleteServer(entry.serverId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Leave the durable tombstone intact for next recovery.
                continue
            }
            dataStore.edit { it[CLEANUP_KEY] = json.encodeToString(CleanupSerializer, cleanupEntries(it) - entry) }
        }
        if (dataStore.data.first()[EXPORT_PENDING] == true) {
            try {
                writeExport(profiles)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return // Retry on the next read/mutation, including after restart.
            }
            dataStore.edit { it[EXPORT_PENDING] = false }
        }
    }

    @Serializable
    private data class Cleanup(val serverId: String, val reference: String, val deleteKey: Boolean)

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
        val CLEANUP_KEY = stringPreferencesKey("opencode_cleanup_v1")
        val EXPORT_PENDING = booleanPreferencesKey("opencode_export_pending")
        val CleanupSerializer = kotlinx.serialization.builtins.ListSerializer(Cleanup.serializer())
        val ListSerializer = kotlinx.serialization.builtins.ListSerializer(StoredProfile.serializer())
        val ExportListSerializer = kotlinx.serialization.builtins.ListSerializer(ExportProfile.serializer())
    }
}
