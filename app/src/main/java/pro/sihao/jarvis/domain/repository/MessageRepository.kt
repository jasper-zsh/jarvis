package pro.sihao.jarvis.domain.repository

import kotlinx.coroutines.flow.Flow
import pro.sihao.jarvis.domain.model.Message

interface MessageRepository {
    fun getAllMessages(): Flow<List<Message>>
    suspend fun getRecentMessages(limit: Int = 50): Flow<List<Message>>
    suspend fun getMessagesPaginated(limit: Int, offset: Int): List<Message>
    suspend fun insertMessage(message: Message): Long
    suspend fun insertMessages(messages: List<Message>)
    suspend fun upsertMessage(message: Message): Long
    suspend fun deleteMessage(message: Message)
    suspend fun deleteLoadingMessages()
    suspend fun clearAllMessages()
    suspend fun clearConversation(conversationId: Long? = null)
    suspend fun getMessageCount(): Int
    
    // Real-time message handling methods
    suspend fun insertRealtimeMessage(message: Message): Long = insertMessage(message)
    suspend fun getRealtimeMessages(): Flow<List<Message>> = getAllMessages()
    suspend fun markRealtimeSessionComplete(sessionId: String) {
        // Default implementation - can be overridden in implementation
    }
}
