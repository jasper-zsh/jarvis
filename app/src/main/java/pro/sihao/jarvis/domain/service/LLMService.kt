package pro.sihao.jarvis.domain.service

import kotlinx.coroutines.flow.Flow
import pro.sihao.jarvis.domain.model.Message

interface LLMService {
    suspend fun sendMessage(
        message: String,
        conversationHistory: List<Message>,
        apiKey: String
    ): Flow<Result<String>>
}