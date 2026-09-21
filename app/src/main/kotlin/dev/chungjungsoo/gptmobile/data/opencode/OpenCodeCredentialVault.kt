package dev.chungjungsoo.gptmobile.data.opencode

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeCredential
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface VaultResult<out T> {
    data class Success<T>(val value: T) : VaultResult<T>
    data object ReauthenticationRequired : VaultResult<Nothing>
    data object Unavailable : VaultResult<Nothing>
}

interface OpenCodeCredentialVault {
    fun save(serverId: String, reference: String, endpointBinding: String, credential: OpenCodeCredential)
    fun load(serverId: String, reference: String, endpointBinding: String): VaultResult<OpenCodeCredential>
    fun delete(serverId: String, reference: String)
    fun deleteServer(serverId: String)
}

@Singleton
class AndroidKeyStoreCredentialVault @Inject constructor(
    @ApplicationContext context: Context
) : OpenCodeCredentialVault {

    private val directory = File(context.noBackupFilesDir, "opencode-vault").also { it.mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    override fun save(serverId: String, reference: String, endpointBinding: String, credential: OpenCodeCredential) {
        val key = keyFor(serverId)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(aad(serverId, reference, endpointBinding))
        val plaintext = json.encodeToString(
            VaultCredential.serializer(),
            when (credential) {
                is OpenCodeCredential.Basic -> VaultCredential(credential.username, credential.password)
            }
        ).encodeToByteArray()
        val encrypted = cipher.doFinal(plaintext)
        val target = fileFor(reference)
        val staging = File(directory, "${target.name}.staging")
        staging.writeBytes(cipher.iv + encrypted)
        // A new credential always receives a new reference. Never replace an existing entry here.
        require(staging.renameTo(target)) { "Could not persist OpenCode credential" }
    }

    override fun load(serverId: String, reference: String, endpointBinding: String): VaultResult<OpenCodeCredential> {
        return try {
            val bytes = fileFor(reference).readBytes()
            if (bytes.size <= IV_LENGTH) return VaultResult.ReauthenticationRequired
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keyFor(serverId), GCMParameterSpec(TAG_LENGTH, bytes.copyOfRange(0, IV_LENGTH)))
            cipher.updateAAD(aad(serverId, reference, endpointBinding))
            val value = json.decodeFromString(VaultCredential.serializer(), cipher.doFinal(bytes.copyOfRange(IV_LENGTH, bytes.size)).decodeToString())
            VaultResult.Success(OpenCodeCredential.Basic(value.username, value.password))
        } catch (_: java.io.FileNotFoundException) {
            VaultResult.ReauthenticationRequired
        } catch (_: java.security.UnrecoverableKeyException) {
            VaultResult.ReauthenticationRequired
        } catch (_: javax.crypto.AEADBadTagException) {
            VaultResult.ReauthenticationRequired
        } catch (_: Exception) {
            VaultResult.Unavailable
        }
    }

    override fun delete(serverId: String, reference: String) {
        fileFor(reference).delete()
    }

    override fun deleteServer(serverId: String) {
        val alias = "opencode.$serverId"
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }.let { store ->
            if (store.containsAlias(alias)) store.deleteEntry(alias)
        }
    }

    private fun keyFor(serverId: String): SecretKey {
        val alias = "opencode.$serverId"
        val store = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).apply {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private fun fileFor(reference: String): File {
        require(REFERENCE.matches(reference)) { "Invalid credential reference" }
        return File(directory, "$reference.bin")
    }

    private fun aad(serverId: String, reference: String, endpointBinding: String) =
        "$serverId\u0000$reference\u0000$endpointBinding".encodeToByteArray()

    @Serializable
    private data class VaultCredential(val username: String, val password: String)

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_LENGTH = 128
        val REFERENCE = Regex("[a-zA-Z0-9_-]{1,128}")
    }
}
