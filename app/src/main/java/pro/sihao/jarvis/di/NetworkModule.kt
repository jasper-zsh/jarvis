package pro.sihao.jarvis.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pro.sihao.jarvis.data.service.PipeCatServiceImpl
import pro.sihao.jarvis.domain.service.PipeCatService

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    abstract fun bindPipeCatService(
        pipeCatServiceImpl: PipeCatServiceImpl
    ): PipeCatService
}