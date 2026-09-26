package dev.chungjungsoo.gptmobile.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeBrowseRepository
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeCacheDatabase
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeCredentialVault
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeProfileRepository
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeReadApi
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeReliableSyncCoordinator
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeSseApi
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeUrlPolicy
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OpenCodeBrowseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): OpenCodeCacheDatabase = OpenCodeCacheDatabase.open(context)

    @Provides @Singleton
    fun api(vault: OpenCodeCredentialVault, policy: OpenCodeUrlPolicy) = OpenCodeReadApi(vault, policy)

    @Provides @Singleton
    fun sse(vault: OpenCodeCredentialVault, policy: OpenCodeUrlPolicy) = OpenCodeSseApi(vault, policy)

    @Provides @Singleton
    fun browse(profiles: OpenCodeProfileRepository, api: OpenCodeReadApi, sse: OpenCodeSseApi, db: OpenCodeCacheDatabase) = OpenCodeBrowseRepository(profiles, api, sse, db.cacheDao())

    @Provides @Singleton
    fun reliableSync(browse: OpenCodeBrowseRepository) = OpenCodeReliableSyncCoordinator(browse)
}
