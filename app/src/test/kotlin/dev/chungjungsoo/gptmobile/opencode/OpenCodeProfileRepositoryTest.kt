package dev.chungjungsoo.gptmobile.opencode

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import dev.chungjungsoo.gptmobile.data.opencode.DataStoreOpenCodeProfileRepository
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeCredentialVault
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeUrlPolicy
import dev.chungjungsoo.gptmobile.data.opencode.VaultResult
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import java.io.IOException
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenCodeProfileRepositoryTest {
    private class MemoryStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        private val lock = Mutex()
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = lock.withLock {
            yield()
            transform(data.value).also { data.value = it }
        }
    }

    private class FakeVault : OpenCodeCredentialVault {
        val entries = mutableMapOf<String, OpenCodeCredential>()
        var failSave = false
        override fun save(serverId: String, reference: String, endpointBinding: String, credential: OpenCodeCredential) {
            if (failSave) throw IOException("Synthetic storage failure")
            entries[reference] = credential
        }
        override fun load(serverId: String, reference: String, endpointBinding: String): VaultResult<OpenCodeCredential> =
            entries[reference]?.let { VaultResult.Success(it) } ?: VaultResult.ReauthenticationRequired
        override fun delete(serverId: String, reference: String) {
            entries.remove(reference)
        }
        override fun deleteServer(serverId: String) = Unit
    }

    private val store = MemoryStore()
    private val vault = FakeVault()
    private val exportFile = File(System.getProperty("java.io.tmpdir"), "opencode-profile-test-${System.nanoTime()}.json")
    private val repository = DataStoreOpenCodeProfileRepository(
        store,
        vault,
        OpenCodeUrlPolicy(false),
        exportFile
    )
    private val credential = OpenCodeCredential.Basic("test-user", "synthetic-password")

    @Test
    fun concurrentCreatesPreserveEveryProfile() = runBlocking {
        (1..12).map { i -> async { repository.save(null, "Server $i", "https://server$i.test", credential) } }.awaitAll()
        assertEquals(12, repository.profiles().size)
        assertEquals(12, vault.entries.size)
    }

    @Test
    fun staleEditDoesNotRecreateDeletedProfile() = runBlocking {
        val profile = repository.save(null, "Server", "https://server.test", credential)
        repository.delete(profile.serverId)
        try {
            repository.save(profile.serverId, "Stale form", profile.baseUrl, credential)
            throw AssertionError("Expected stale profile rejection")
        } catch (_: IllegalArgumentException) {
            assertTrue(repository.profiles().isEmpty())
            assertTrue(vault.entries.isEmpty())
        }
    }

    @Test
    fun failedVaultWriteDoesNotChangeExistingReference() = runBlocking {
        val profile = repository.save(null, "Server", "https://server.test", credential)
        vault.failSave = true
        try {
            repository.save(profile.serverId, "Changed", profile.baseUrl, credential)
            throw AssertionError("Expected storage failure")
        } catch (_: IOException) {
            assertEquals(profile, repository.profiles().single())
            assertTrue(vault.entries.containsKey(profile.credentialRef))
        }
    }

    @Test
    fun metadataEditKeepsCredentialAndIncrementsRevision() = runBlocking {
        val before = repository.save(null, "Before", "https://server.test", credential)
        val after = repository.save(before.serverId, "After", before.baseUrl, null)
        assertEquals(before.credentialRef, after.credentialRef)
        assertEquals(before.profileRevision + 1, after.profileRevision)
        assertEquals(1, vault.entries.size)
    }

    @Test
    fun backupExportOmitsCredentialReferenceAndPassword() = runBlocking {
        val profile = repository.save(null, "Server", "https://server.test", credential)
        val export = exportFile.readText()

        assertTrue(export.contains(profile.serverId))
        assertTrue(!export.contains(profile.credentialRef))
        assertTrue(!export.contains("synthetic-password"))
    }

    @Test
    fun healthResultPersistsWithoutChangingCredentialReference() = runBlocking {
        val before = repository.save(null, "Server", "https://server.test", credential)
        repository.recordHealth(before.serverId, "1.18.30", 123L)
        val after = repository.profiles().single()

        assertEquals(before.credentialRef, after.credentialRef)
        assertEquals("1.18.30", after.lastKnownVersion)
        assertEquals(123L, after.lastHealthCheckAt)
    }
}
