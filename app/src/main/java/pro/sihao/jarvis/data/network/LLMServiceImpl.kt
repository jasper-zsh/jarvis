package pro.sihao.jarvis.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pro.sihao.jarvis.data.network.api.OpenAICompatibleApiService
import pro.sihao.jarvis.data.network.dto.OpenAIRequest
import pro.sihao.jarvis.data.network.dto.OpenAIResponse
import pro.sihao.jarvis.data.network.dto.ContentPart
import pro.sihao.jarvis.data.network.dto.InputAudioPayload
import pro.sihao.jarvis.data.network.dto.StreamChunk
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
import android.util.Base64
import java.io.File
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.JsonObject
import okio.buffer
import okio.source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class LLMServiceImpl @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val modelConfigRepository: ModelConfigRepository
) : LLMService {
    private val cancelFlag = AtomicBoolean(false)
    @Volatile private var partialListener: ((String) -> Unit)? = null

    override fun setPartialListener(listener: ((String) -> Unit)?) {
        partialListener = listener
    }

    // Get active API service based on database configuration
    private suspend fun getActiveApiService(): OpenAICompatibleApiService {
        val activeProvider = providerRepository.getActiveProvider()

        val baseUrl = activeProvider?.baseUrl ?: APIConfig.OPENAI.baseUrl

        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = buildHttpClient(logging)

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAICompatibleApiService::class.java)
    }

    private fun buildHttpClient(logging: HttpLoggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private data class ResolvedModel(
        val name: String,
        val config: ModelConfiguration?
    )

    // Resolve model configuration for the active provider with fallbacks
    private suspend fun resolveModelForProvider(providerId: Long): ResolvedModel? {
        val activeModel = modelConfigRepository.getActiveModelConfig()
        if (activeModel != null && activeModel.providerId == providerId && activeModel.isActive) {
            return ResolvedModel(activeModel.modelName, activeModel)
        }

        val defaultModel = modelConfigRepository.getDefaultModelForProvider(providerId)
        if (defaultModel?.isActive == true) {
            modelConfigRepository.setActiveModelConfig(defaultModel.id)
            return ResolvedModel(defaultModel.modelName, defaultModel)
        }

        val firstActiveModel = modelConfigRepository.getFirstActiveModelForProvider(providerId)
        if (firstActiveModel != null) {
            modelConfigRepository.setActiveModelConfig(firstActiveModel.id)
            return ResolvedModel(firstActiveModel.modelName, firstActiveModel)
        }

        val providerDefault = providerRepository.getProviderById(providerId)?.defaultModel
        return providerDefault?.let { ResolvedModel(it, null) }
    }

    private fun encodeFileToBase64(path: String): String? {
        return try {
            val fileBytes = File(path).readBytes()
            Base64.encodeToString(fileBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTextFromPayload(payload: String, gson: Gson): String? {
        // Try delta-based chunk first (string content or array content)
        runCatching {
            val obj = gson.fromJson(payload, JsonObject::class.java)
            val choices = obj.getAsJsonArray("choices") ?: return@runCatching null
            val first = choices.firstOrNull()?.asJsonObject ?: return@runCatching null
            val delta = first.getAsJsonObject("delta") ?: return@runCatching null
            val contentElement = delta.get("content") ?: return@runCatching null
            if (contentElement.isJsonPrimitive && contentElement.asJsonPrimitive.isString) {
                val text = contentElement.asString
                if (text.isNotBlank()) return text
            } else if (contentElement.isJsonArray) {
                val arr = contentElement.asJsonArray
                val combined = arr.mapNotNull { el ->
                    el.asJsonObject?.get("text")?.asString
                }.joinToString("")
                if (combined.isNotBlank()) return combined
            }
        }

        // Fallback: try message.content structure
        return runCatching {
            val obj = gson.fromJson(payload, JsonObject::class.java)
            val choices = obj.getAsJsonArray("choices") ?: return@runCatching null
            val first = choices.firstOrNull()?.asJsonObject ?: return@runCatching null
            val message = first.getAsJsonObject("message") ?: return@runCatching null
            val contentArr = message.getAsJsonArray("content") ?: return@runCatching null
            contentArr.mapNotNull { element ->
                element.asJsonObject?.get("text")?.asString
            }.joinToString("")
        }.getOrNull()
    }

    override fun cancelActiveRequest() {
        cancelFlag.set(true)
    }

    private suspend fun sendChatRequest(
        provider: pro.sihao.jarvis.data.database.entity.LLMProviderEntity,
        apiKey: String,
        request: OpenAIRequest
    ): Result<String> {
        cancelFlag.set(false)
        return withContext(Dispatchers.IO) {
            val result = streamChatCompletion(provider.baseUrl, "Bearer $apiKey", request)
            cancelFlag.set(false)
            partialListener = null
            result
        }
    }

    // Get API key for active provider
    private suspend fun getApiKeyForActiveProvider(providerId: Long): String? {
        return providerRepository.getApiKeyForProvider(providerId)
    }

    private suspend fun streamChatCompletion(
        baseUrl: String,
        authorization: String,
        request: OpenAIRequest
    ): Result<String> {
        return try {
            val gson = Gson()
            val client = buildHttpClient()
            val url = if (baseUrl.endsWith("/")) "$baseUrl" else "$baseUrl/"
            val fullUrl = "${url}chat/completions"

            val streamingRequest = request.copy(stream = true)
            val jsonBody = gson.toJson(streamingRequest)
            val httpRequest = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", authorization)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            withContext(Dispatchers.IO) {
                val call = client.newCall(httpRequest)
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("API Error: ${response.code} ${response.message}"))
                    }
                    val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
                    val source = body.source()
                    val sb = StringBuilder()
                    var readLines = 0
            while (true) {
                if (!isActive || cancelFlag.get()) {
                    call.cancel()
                    return@withContext Result.failure(Exception("Canceled"))
                }
                        val line = source.readUtf8Line() ?: break
                        readLines++
                        println("LLM stream line: <$line>")
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        if (trimmed.startsWith("data:")) {
                            val payload = trimmed.removePrefix("data:").trim()
                            if (payload == "[DONE]") break
                            println("LLM stream chunk payload: $payload")
                            val deltaText = extractTextFromPayload(payload, gson)
                            if (!deltaText.isNullOrBlank()) {
                                sb.append(deltaText)
                                partialListener?.invoke(sb.toString())
                            }
                        }
                    }
                    val resultText = sb.toString()
                    if (resultText.isBlank()) {
                        Result.failure(Exception("Empty streamed content (lines=$readLines)"))
                    } else {
                        Result.success(resultText)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: e.toString()))
        }
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
            val resolvedModel = resolveModelForProvider(provider.id)
            val modelName = resolvedModel?.name
            if (modelName.isNullOrEmpty()) {
                emit(Result.failure(Exception("No model configured for provider: ${provider.displayName}")))
                return@flow
            }

            val openAIMessages = mutableListOf<pro.sihao.jarvis.data.network.dto.OpenAIMessage>()

            // Add system message
            openAIMessages.add(
                pro.sihao.jarvis.data.network.dto.OpenAIMessage(
                    role = "system",
                    content = listOf(
                        ContentPart(
                            type = "text",
                            text = "You are Jarvis, a helpful AI assistant. Be concise and friendly."
                        )
                    )
                )
            )

            // Add conversation history (last 10 messages for context)
            conversationHistory.takeLast(10).forEach { msg ->
                openAIMessages.add(
                    pro.sihao.jarvis.data.network.dto.OpenAIMessage(
                        role = if (msg.isFromUser) "user" else "assistant",
                        content = listOf(
                            ContentPart(
                                type = "text",
                                text = msg.content
                            )
                        )
                    )
                )
            }

            // Add current message
            openAIMessages.add(
                pro.sihao.jarvis.data.network.dto.OpenAIMessage(
                    role = "user",
                    content = listOf(
                        ContentPart(
                            type = "text",
                            text = message
                        )
                    )
                )
            )

            // Create request with model configuration
            val request = OpenAIRequest(
                model = modelName,
                messages = openAIMessages,
                temperature = resolvedModel.config?.temperature ?: 0.7f,
                max_tokens = resolvedModel.config?.maxTokens
            )

            val streamResult = sendChatRequest(provider, effectiveApiKey, request)
            emit(streamResult)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun sendMessage(
        message: String,
        conversationHistory: List<Message>,
        mediaMessage: Message?,
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
            val resolvedModel = resolveModelForProvider(provider.id)
            val modelName = resolvedModel?.name
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
                    content = listOf(
                        ContentPart(
                            type = "text",
                            text = "You are Jarvis, a helpful AI assistant. Be concise and friendly. " +
                                    "You can analyze images, transcribe voice messages, and understand various media formats."
                        )
                    )
                )
            )

            // Add conversation history (last 10 messages for context)
            conversationHistory.takeLast(10).forEach { msg ->
                val parts = when (msg.contentType) {
                    pro.sihao.jarvis.domain.model.ContentType.TEXT -> listOf(
                        ContentPart(type = "text", text = msg.content)
                    )
                    pro.sihao.jarvis.domain.model.ContentType.VOICE -> {
                        val encoded = msg.mediaUrl?.let { encodeFileToBase64(it) }
                        buildList {
                            encoded?.let {
                                add(
                                    ContentPart(
                                        type = "input_audio",
                                        input_audio = InputAudioPayload(
                                            data = "data:;base64,$it",
                                            format = "aac"
                                        )
                                    )
                                )
                            }
                            add(ContentPart(type = "text", text = if (msg.content.isNotBlank()) msg.content else "[Voice message]"))
                        }
                    }
                    pro.sihao.jarvis.domain.model.ContentType.PHOTO -> listOf(
                        ContentPart(
                            type = "text",
                            text = if (msg.content.isNotBlank()) msg.content else "[Photo - user sent an image]"
                        )
                    )
                }
                openAIMessages.add(
                    pro.sihao.jarvis.data.network.dto.OpenAIMessage(
                        role = if (msg.isFromUser) "user" else "assistant",
                        content = parts
                    )
                )
            }

            val userContentParts = mutableListOf<ContentPart>()
            when {
                mediaMessage != null && mediaMessage.contentType == pro.sihao.jarvis.domain.model.ContentType.VOICE -> {
                    mediaMessage.mediaUrl?.let { path ->
                        encodeFileToBase64(path)?.let { encoded ->
                            userContentParts.add(
                                ContentPart(
                                    type = "input_audio",
                                    input_audio = InputAudioPayload(
                                        data = "data:;base64,$encoded",
                                        format = "aac"
                                    )
                                )
                            )
                        }
                    }
                    if (message.isNotBlank()) {
                        userContentParts.add(ContentPart(type = "text", text = message))
                    }
                }
                mediaMessage != null && mediaMessage.contentType == pro.sihao.jarvis.domain.model.ContentType.PHOTO -> {
                    userContentParts.add(
                        ContentPart(
                            type = "text",
                            text = if (message.isNotBlank()) message else "[Photo]"
                        )
                    )
                }
                else -> {
                    userContentParts.add(ContentPart(type = "text", text = message))
                }
            }

            openAIMessages.add(
                pro.sihao.jarvis.data.network.dto.OpenAIMessage(
                    role = "user",
                    content = userContentParts
                )
            )

            // Create request with model configuration
            val request = OpenAIRequest(
                model = modelName,
                messages = openAIMessages,
                temperature = resolvedModel.config?.temperature ?: 0.7f,
                max_tokens = resolvedModel.config?.maxTokens
            )

            val streamResult = sendChatRequest(provider, effectiveApiKey, request)
            emit(streamResult)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
