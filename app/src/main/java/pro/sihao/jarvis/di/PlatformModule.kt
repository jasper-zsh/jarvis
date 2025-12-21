package pro.sihao.jarvis.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pro.sihao.jarvis.platform.android.connection.GlassesConnectionManager
import pro.sihao.jarvis.platform.network.NetworkMonitor
import pro.sihao.jarvis.core.data.storage.GlassesPreferences
import pro.sihao.jarvis.core.presentation.navigation.NavigationManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {

    @Provides
    @Singleton
    fun provideGlassesConnectionManager(
        @ApplicationContext context: Context,
        preferences: GlassesPreferences
    ): GlassesConnectionManager {
        return GlassesConnectionManager(context, preferences)
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
    fun provideNavigationManager(): NavigationManager {
        return NavigationManager()
    }
}