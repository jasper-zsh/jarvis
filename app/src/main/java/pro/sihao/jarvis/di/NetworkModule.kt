package pro.sihao.jarvis.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pro.sihao.jarvis.data.network.LLMServiceImpl
import pro.sihao.jarvis.domain.service.LLMService

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    abstract fun bindLLMService(
        llmServiceImpl: LLMServiceImpl
    ): LLMService
}