package pro.sihao.jarvis.data.network

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.source
import pro.sihao.jarvis.data.database.entity.LLMProviderEntity
import pro.sihao.jarvis.data.network.dto.ContentPart
import pro.sihao.jarvis.data.network.dto.InputAudioPayload
import pro.sihao.jarvis.data.network.dto.OpenAIRequest
import pro.sihao.jarvis.data.repository.ModelConfigRepository
import pro.sihao.jarvis.data.repository.ModelConfiguration
import pro.sihao.jarvis.data.repository.ProviderRepository
import pro.sihao.jarvis.domain.model.ContentType
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.domain.service.LLMService
import pro.sihao.jarvis.domain.service.LLMStreamEvent
import java.io.File
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMServiceImpl @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val modelConfigRepository: ModelConfigRepository
) : LLMService {
    private val cancelFlag = AtomicBoolean(false)

    override fun cancelActiveRequest() {
        cancelFlag.set(true)
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

    private suspend fun getApiKeyForActiveProvider(providerId: Long): String? {
        return providerRepository.getApiKeyForProvider(providerId)
    }

    private fun resolveBaseUrl(provider: LLMProviderEntity?): String {
        return provider?.baseUrl ?: APIConfig.OPENAI.baseUrl
    }

    private fun streamChatCompletion(
        baseUrl: String,
        authorization: String,
        request: OpenAIRequest
    ): Flow<LLMStreamEvent> {
        return flow {
            cancelFlag.set(false)
            val gson = Gson()
            val client = buildHttpClient()
            val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val fullUrl = "${url}chat/completions"

            val streamingRequest = request.copy(stream = true)
            val jsonBody = gson.toJson(streamingRequest)
            val httpRequest = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", authorization)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val call = client.newCall(httpRequest)
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        emit(LLMStreamEvent.Error(Exception("API Error: ${response.code} ${response.message}")))
                        return@use
                    }
                    val body = response.body
                    if (body == null) {
                        emit(LLMStreamEvent.Error(Exception("Empty response body")))
                        return@use
                    }

                    val source = body.source()
                    val sb = StringBuilder()
                    var readLines = 0

                    while (true) {
                        if (!currentCoroutineContext().isActive || cancelFlag.get()) {
                            call.cancel()
                            emit(LLMStreamEvent.Canceled)
                            return@use
                        }
                        val line = source.readUtf8Line() ?: break
                        readLines++
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        if (trimmed.startsWith("data:")) {
                            val payload = trimmed.removePrefix("data:").trim()
                            if (payload == "[DONE]") break
                            val deltaText = extractTextFromPayload(payload, gson)
                            if (!deltaText.isNullOrBlank()) {
                                sb.append(deltaText)
                                emit(LLMStreamEvent.Partial(sb.toString()))
                            }
                        }
                    }
                    val resultText = sb.toString()
                    if (cancelFlag.get()) {
                        emit(LLMStreamEvent.Canceled)
                    } else if (resultText.isBlank()) {
                        emit(LLMStreamEvent.Error(Exception("Empty streamed content (lines=$readLines)")))
                    } else {
                        emit(LLMStreamEvent.Complete(resultText))
                    }
                }
            } catch (e: Exception) {
                emit(LLMStreamEvent.Error(Exception(e.message ?: e.toString())))
            } finally {
                cancelFlag.set(false)
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun buildBaseMessages(conversationHistory: List<Message>): MutableList<pro.sihao.jarvis.data.network.dto.OpenAIMessage> {
        val messages = mutableListOf<pro.sihao.jarvis.data.network.dto.OpenAIMessage>()

        messages.add(
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

        conversationHistory.takeLast(10).forEach { msg ->
            messages.add(
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

        return messages
    }

    private fun buildMediaAwareHistory(conversationHistory: List<Message>): MutableList<pro.sihao.jarvis.data.network.dto.OpenAIMessage> {
        val messages = mutableListOf<pro.sihao.jarvis.data.network.dto.OpenAIMessage>()

        messages.add(
            pro.sihao.jarvis.data.network.dto.OpenAIMessage(
                role = "system",
                content = listOf(
                    ContentPart(
                        type = "text",
                        text = "You are Jarvis, a helpful AI assistant. Be concise and friendly. You can analyze images, transcribe voice messages, and understand various media formats."
                    )
                )
            )
        )

        conversationHistory.takeLast(10).forEach { msg ->
            val parts = when (msg.contentType) {
                ContentType.TEXT -> listOf(
                    ContentPart(type = "text", text = msg.content)
                )
                ContentType.VOICE -> {
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
                ContentType.PHOTO -> listOf(
                    ContentPart(
                        type = "text",
                        text = if (msg.content.isNotBlank()) msg.content else "[Photo - user sent an image]"
                    )
                )
            }
            messages.add(
                pro.sihao.jarvis.data.network.dto.OpenAIMessage(
                    role = if (msg.isFromUser) "user" else "assistant",
                    content = parts
                )
            )
        }

        return messages
    }

    override suspend fun sendMessage(
        message: String,
        conversationHistory: List<Message>,
        apiKey: String?
    ): Flow<LLMStreamEvent> = flow {
        val provider = providerRepository.getActiveProvider()

        if (provider == null) {
            emit(LLMStreamEvent.Error(Exception("No provider configured")))
            return@flow
        }

        val effectiveApiKey = apiKey ?: getApiKeyForActiveProvider(provider.id)
        if (effectiveApiKey.isNullOrEmpty()) {
            emit(LLMStreamEvent.Error(Exception("No API key configured for provider: ${provider.displayName}")))
            return@flow
        }

        val resolvedModel = resolveModelForProvider(provider.id)
        val modelName = resolvedModel?.name
        if (modelName.isNullOrEmpty()) {
            emit(LLMStreamEvent.Error(Exception("No model configured for provider: ${provider.displayName}")))
            return@flow
        }

        val openAIMessages = buildBaseMessages(conversationHistory)
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

        val request = OpenAIRequest(
            model = modelName,
            messages = openAIMessages,
            temperature = resolvedModel.config?.temperature ?: 0.7f,
            max_tokens = resolvedModel.config?.maxTokens
        )

        emitAll(streamChatCompletion(resolveBaseUrl(provider), "Bearer $effectiveApiKey", request))
    }

    override suspend fun sendMessage(
        message: String,
        conversationHistory: List<Message>,
        mediaMessage: Message?,
        apiKey: String?
    ): Flow<LLMStreamEvent> = flow {
        val provider = providerRepository.getActiveProvider()

        if (provider == null) {
            emit(LLMStreamEvent.Error(Exception("No provider configured")))
            return@flow
        }

        val effectiveApiKey = apiKey ?: getApiKeyForActiveProvider(provider.id)
        if (effectiveApiKey.isNullOrEmpty()) {
            emit(LLMStreamEvent.Error(Exception("No API key configured for provider: ${provider.displayName}")))
            return@flow
        }

        val resolvedModel = resolveModelForProvider(provider.id)
        val modelName = resolvedModel?.name
        if (modelName.isNullOrEmpty()) {
            emit(LLMStreamEvent.Error(Exception("No model configured for provider: ${provider.displayName}")))
            return@flow
        }

        val openAIMessages = buildMediaAwareHistory(conversationHistory)

        val userContentParts = mutableListOf<ContentPart>()
        when {
            mediaMessage != null && mediaMessage.contentType == ContentType.VOICE -> {
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
            mediaMessage != null && mediaMessage.contentType == ContentType.PHOTO -> {
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

        val request = OpenAIRequest(
            model = modelName,
            messages = openAIMessages,
            temperature = resolvedModel.config?.temperature ?: 0.7f,
            max_tokens = resolvedModel.config?.maxTokens
        )

        emitAll(streamChatCompletion(resolveBaseUrl(provider), "Bearer $effectiveApiKey", request))
    }
}
