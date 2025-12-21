package pro.sihao.jarvis.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pro.sihao.jarvis.platform.network.webrtc.PipeCatConnectionManager
import pro.sihao.jarvis.core.domain.repository.MessageRepository
import pro.sihao.jarvis.core.domain.service.PipeCatService
import pro.sihao.jarvis.features.realtime.data.config.ConfigurationManager
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
        messageRepository: MessageRepository,
        configurationManager: ConfigurationManager,
    ): PipeCatConnectionManager {
        return PipeCatConnectionManager(context, pipeCatService, messageRepository, configurationManager)
    }
}