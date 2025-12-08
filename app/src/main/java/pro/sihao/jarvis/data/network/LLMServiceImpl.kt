package pro.sihao.jarvis.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pro.sihao.jarvis.data.network.api.OpenAICompatibleApiService
import pro.sihao.jarvis.data.network.dto.OpenAIRequest
import pro.sihao.jarvis.data.network.dto.OpenAIResponse
import pro.sihao.jarvis.data.storage.SecureStorage
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.domain.service.LLMService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMServiceImpl @Inject constructor(
    private val secureStorage: SecureStorage
) : LLMService {

    // Get user-configured API settings
    private val apiConfig: APIConfig
        get() = APIConfig(
            baseUrl = secureStorage.getBaseUrl() ?: APIConfig.OPENAI.baseUrl,
            defaultModel = secureStorage.getModelName() ?: APIConfig.OPENAI.defaultModel,
            name = secureStorage.getApiProvider() ?: APIConfig.OPENAI.name
        )

    private val openAIApiService: OpenAICompatibleApiService by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(secureStorage.getBaseUrl() ?: apiConfig.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAICompatibleApiService::class.java)
    }

    private fun getApiConfig(providerName: String): APIConfig {
        return when (providerName) {
            "DEEPSEEK" -> APIConfig.DEEPSEEK
            "LOCAL_AI" -> APIConfig.LOCAL_AI
            "TOGETHER_AI" -> APIConfig.TOGETHER_AI
            "GROQ" -> APIConfig.GROQ
            else -> APIConfig.OPENAI
        }
    }

    override suspend fun sendMessage(
        message: String,
        conversationHistory: List<Message>,
        apiKey: String
    ): Flow<Result<String>> = flow {
        try {
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

      val maxTokens = secureStorage.getMaxTokens()
      val request = OpenAIRequest(
                model = secureStorage.getModelName() ?: apiConfig.defaultModel,
                messages = openAIMessages,
                temperature = secureStorage.getTemperature(),
                max_tokens = if (maxTokens > 0) maxTokens else null
            )

            val response = openAIApiService.createChatCompletion(
                request = request,
                authorization = "Bearer $apiKey"
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