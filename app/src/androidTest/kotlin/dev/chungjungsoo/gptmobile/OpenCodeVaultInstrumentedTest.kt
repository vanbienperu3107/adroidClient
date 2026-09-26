package dev.chungjungsoo.gptmobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.chungjungsoo.gptmobile.data.opencode.AndroidKeyStoreCredentialVault
import dev.chungjungsoo.gptmobile.data.opencode.VaultResult
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenCodeVaultInstrumentedTest {
    @Test
    fun realKeysEnforceBindingAndServerIsolation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val vault = AndroidKeyStoreCredentialVault(context)
        val a = UUID.randomUUID().toString()
        val b = UUID.randomUUID().toString()
        val ra = "cred_${UUID.randomUUID()}"
        val rb = "cred_${UUID.randomUUID()}"
        val secret = OpenCodeCredential.Basic("synthetic", "instrumented-only")
        try {
            vault.save(a, ra, "https://a.test", secret)
            vault.save(b, rb, "https://b.test", secret)
            assertEquals(VaultResult.Success(secret), vault.load(a, ra, "https://a.test"))
            assertEquals(VaultResult.ReauthenticationRequired, vault.load(a, ra, "https://other.test"))
            val ciphertext = context.noBackupFilesDir.resolve("opencode-vault/$ra.bin").readBytes()
            assertTrue(!ciphertext.decodeToString().contains("instrumented-only"))
            vault.deleteServer(a)
            assertEquals(VaultResult.ReauthenticationRequired, vault.load(a, ra, "https://a.test"))
            assertEquals(VaultResult.Success(secret), vault.load(b, rb, "https://b.test"))
        } finally {
            vault.delete(a, ra)
            vault.delete(b, rb)
            vault.deleteServer(a)
            vault.deleteServer(b)
        }
    }
}
