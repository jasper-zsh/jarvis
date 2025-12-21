package pro.sihao.jarvis.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pro.sihao.jarvis.platform.android.connection.GlassesConnectionManager
import pro.sihao.jarvis.platform.android.audio.AudioRoutingManager
import pro.sihao.jarvis.features.realtime.data.bridge.GlassesPipeCatBridge
import pro.sihao.jarvis.platform.network.webrtc.PipeCatConnectionManager
import pro.sihao.jarvis.core.domain.repository.MessageRepository
import pro.sihao.jarvis.core.domain.service.PipeCatService
import javax.inject.Singleton

/**
 * Hilt module for PipeCat dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object PipeCatModule {

    
    @Provides
    @Singleton
    fun providePipeCatConnectionManager(
        @ApplicationContext context: Context,
        pipeCatService: PipeCatService,
        messageRepository: MessageRepository
    ): PipeCatConnectionManager {
        return PipeCatConnectionManager(context, pipeCatService, messageRepository)
    }

    @Provides
    @Singleton
    fun provideGlassesPipeCatBridge(
        @ApplicationContext context: Context,
        glassesConnectionManager: GlassesConnectionManager,
        pipeCatConnectionManager: PipeCatConnectionManager,
        messageRepository: MessageRepository,
        audioRoutingManager: AudioRoutingManager
    ): GlassesPipeCatBridge {
        return GlassesPipeCatBridge(context, glassesConnectionManager, pipeCatConnectionManager, messageRepository, audioRoutingManager)
    }

    @Provides
    @Singleton
    fun provideAudioRoutingManager(
        @ApplicationContext context: Context
    ): AudioRoutingManager {
        return AudioRoutingManager(context)
    }
}