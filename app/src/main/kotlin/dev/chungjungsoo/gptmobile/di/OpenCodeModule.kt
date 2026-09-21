package dev.chungjungsoo.gptmobile.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.chungjungsoo.gptmobile.data.opencode.AndroidKeyStoreCredentialVault
import dev.chungjungsoo.gptmobile.data.opencode.DataStoreOpenCodeProfileRepository
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeCredentialVault
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeProfileRepository
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeUrlPolicy
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OpenCodeModule {
    @Provides
    @Singleton
    @OpenCodeProfileStore
    fun provideOpenCodeProfileDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.noBackupFilesDir.resolve("opencode-profiles.preferences_pb") }
        )

    @Provides
    @Singleton
    fun provideOpenCodeUrlPolicy(): OpenCodeUrlPolicy = OpenCodeUrlPolicy(allowHttpLoopback = false)

    @Provides
    @Singleton
    fun provideOpenCodeCredentialVault(vault: AndroidKeyStoreCredentialVault): OpenCodeCredentialVault = vault

    @Provides
    @Singleton
    fun provideOpenCodeProfileRepository(repository: DataStoreOpenCodeProfileRepository): OpenCodeProfileRepository = repository
}
