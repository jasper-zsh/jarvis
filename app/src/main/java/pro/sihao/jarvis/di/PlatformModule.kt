package pro.sihao.jarvis.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pro.sihao.jarvis.platform.android.connection.GlassesConnectionManager
import pro.sihao.jarvis.platform.network.NetworkMonitor
import pro.sihao.jarvis.features.realtime.data.config.ConfigurationManager
import pro.sihao.jarvis.platform.security.encryption.ApikeyEncryption
import pro.sihao.jarvis.core.data.storage.GlassesPreferences
import pro.sihao.jarvis.core.data.storage.MediaStorageManager
import pro.sihao.jarvis.core.domain.repository.MessageRepository
import pro.sihao.jarvis.core.domain.service.PipeCatService
import pro.sihao.jarvis.core.presentation.navigation.NavigationManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {

    @Provides
    @Singleton
    fun provideGlassesConnectionManager(
        @ApplicationContext context: Context,
        preferences: GlassesPreferences,
        mediaStorageManager: MediaStorageManager,
        messageRepository: MessageRepository,
        pipeCatService: PipeCatService
    ): GlassesConnectionManager {
        return GlassesConnectionManager(context, preferences, mediaStorageManager, messageRepository, pipeCatService)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }

    @Provides
    @Singleton
    fun provideConfigurationManager(
        @ApplicationContext context: Context,
        apikeyEncryption: ApikeyEncryption
    ): ConfigurationManager {
        return ConfigurationManager(context, apikeyEncryption)
    }

    @Provides
    @Singleton
    fun provideApikeyEncryption(
        @ApplicationContext context: Context
    ): ApikeyEncryption {
        return ApikeyEncryption(context)
    }

    @Provides
    @Singleton
    fun provideNavigationManager(): NavigationManager {
        return NavigationManager()
    }
}