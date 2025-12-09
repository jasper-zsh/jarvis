package pro.sihao.jarvis.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pro.sihao.jarvis.media.PhotoCaptureManager
import pro.sihao.jarvis.media.VoicePlayer
import pro.sihao.jarvis.media.VoiceRecorder
import pro.sihao.jarvis.permission.PermissionManager
import pro.sihao.jarvis.data.storage.MediaStorageManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideMediaStorageManager(@ApplicationContext context: Context): MediaStorageManager {
        return MediaStorageManager(context)
    }

    @Provides
    @Singleton
    fun provideVoiceRecorder(@ApplicationContext context: Context): VoiceRecorder {
        return VoiceRecorder(context)
    }

    @Provides
    @Singleton
    fun provideVoicePlayer(@ApplicationContext context: Context): VoicePlayer {
        return VoicePlayer(context)
    }

    @Provides
    @Singleton
    fun providePhotoCaptureManager(@ApplicationContext context: Context): PhotoCaptureManager {
        return PhotoCaptureManager(context)
    }

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }
}