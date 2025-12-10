package pro.sihao.jarvis.domain.service

import kotlinx.coroutines.flow.Flow
import pro.sihao.jarvis.domain.model.Message

sealed class LLMStreamEvent {
    data class Partial(val content: String) : LLMStreamEvent()
    data class Complete(val content: String) : LLMStreamEvent()
    data class Error(val throwable: Throwable) : LLMStreamEvent()
    object Canceled : LLMStreamEvent()
}

interface LLMService {
    suspend fun sendMessage(
        message: String,
        conversationHistory: List<Message>,
        apiKey: String? = null
    ): Flow<LLMStreamEvent>

    suspend fun sendMessage(
        message: String,
        conversationHistory: List<Message>,
        mediaMessage: Message? = null,
        apiKey: String? = null
    ): Flow<LLMStreamEvent>

    fun cancelActiveRequest()
}
