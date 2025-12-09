package pro.sihao.jarvis.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pro.sihao.jarvis.data.network.api.OpenAICompatibleApiService
import pro.sihao.jarvis.data.network.dto.OpenAIRequest
import pro.sihao.jarvis.data.network.dto.OpenAIResponse
import pro.sihao.jarvis.data.repository.ProviderRepository
import pro.sihao.jarvis.data.repository.ModelConfigRepository
import pro.sihao.jarvis.data.repository.ModelConfiguration
import pro.sihao.jarvis.data.database.entity.LLMProviderEntity
import pro.sihao.jarvis.data.database.entity.ModelConfigEntity
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.domain.service.LLMService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMServiceImpl @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val modelConfigRepository: ModelConfigRepository
) : LLMService {

    // Get active API service based on database configuration
    private suspend fun getActiveApiService(): OpenAICompatibleApiService {
        val activeProvider = providerRepository.getActiveProvider()

        val baseUrl = activeProvider?.baseUrl ?: APIConfig.OPENAI.baseUrl

        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAICompatibleApiService::class.java)
    }

    // Get active model configuration
    private suspend fun getActiveModelConfig(): ModelConfiguration? {
        return modelConfigRepository.getActiveModelConfig()
    }

    // Get API key for active provider
    private suspend fun getApiKeyForActiveProvider(providerId: Long): String? {
        return providerRepository.getApiKeyForProvider(providerId)
    }

    override suspend fun sendMessage(
        message: String,
        conversationHistory: List<Message>,
        apiKey: String?
    ): Flow<Result<String>> = flow {
        try {
            // Get active provider
            val provider = providerRepository.getActiveProvider()

            if (provider == null) {
                emit(Result.failure(Exception("No provider configured")))
                return@flow
            }

            // Get API key
            val effectiveApiKey = apiKey ?: getApiKeyForActiveProvider(provider.id)
            if (effectiveApiKey.isNullOrEmpty()) {
                emit(Result.failure(Exception("No API key configured for provider: ${provider.displayName}")))
                return@flow
            }

            // Get model configuration
            val modelConfig = getActiveModelConfig()
            val modelName = modelConfig?.modelName ?: provider.defaultModel
            if (modelName.isNullOrEmpty()) {
                emit(Result.failure(Exception("No model configured for provider: ${provider.displayName}")))
                return@flow
            }

            // Get API service for this provider
            val apiService = getActiveApiService()

            val openAIMessages = mutableListOf<pro.sihao.jarvis.data.network.dto.OpenAIMessage>()

            // Add system message
            openAIMessages.add(
                pro.sihao.jarvis.data.network.dto.OpenAIMessage(
                    role = "system",
                    content = "You are Jarvis, a helpful AI assistant. Be concise and friendly."
                )
            )

            // Add conversation history (last 10 messages for context)
            conversationHistory.takeLast(10).forEach { msg ->
                openAIMessages.add(
                    pro.sihao.jarvis.data.network.dto.OpenAIMessage(
                        role = if (msg.isFromUser) "user" else "assistant",
                        content = msg.content
                    )
                )
            }

            // Add current message
            openAIMessages.add(
                pro.sihao.jarvis.data.network.dto.OpenAIMessage(
                    role = "user",
                    content = message
                )
            )

            // Create request with model configuration
            val request = OpenAIRequest(
                model = modelName,
                messages = openAIMessages,
                temperature = modelConfig?.temperature ?: 0.7f,
                max_tokens = modelConfig?.maxTokens
            )

            val response = apiService.createChatCompletion(
                "Bearer $effectiveApiKey",
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.choices.isNotEmpty()) {
                    val aiMessage = body.choices.first().message.content
                    emit(Result.success(aiMessage))
                } else {
                    emit(Result.failure(Exception("Empty response from AI")))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                emit(Result.failure(Exception("API Error: $errorMessage")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}