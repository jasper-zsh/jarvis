package pro.sihao.jarvis.domain.repository

import kotlinx.coroutines.flow.Flow
import pro.sihao.jarvis.domain.model.Message

interface MessageRepository {
    fun getAllMessages(): Flow<List<Message>>
    suspend fun insertMessage(message: Message): Long
    suspend fun insertMessages(messages: List<Message>)
    suspend fun deleteMessage(message: Message)
    suspend fun deleteLoadingMessages()
    suspend fun clearAllMessages()
    suspend fun getMessageCount(): Int
}