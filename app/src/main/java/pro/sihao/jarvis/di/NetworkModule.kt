package pro.sihao.jarvis.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pro.sihao.jarvis.features.realtime.data.service.PipeCatServiceImpl
import pro.sihao.jarvis.core.domain.service.PipeCatService

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    abstract fun bindPipeCatService(
        pipeCatServiceImpl: PipeCatServiceImpl
    ): PipeCatService
}