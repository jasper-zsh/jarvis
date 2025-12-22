package pro.sihao.jarvis.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for PipeCat dependencies
 *
 * NOTE: This module is kept for future extensibility but PipeCatConnectionManager
 * registration has been removed to reduce code duplication.
 * The PipeCatService is directly bound in its implementation class with @Singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object PipeCatModule {
    // Empty module - dependencies are now provided directly in their implementations
}