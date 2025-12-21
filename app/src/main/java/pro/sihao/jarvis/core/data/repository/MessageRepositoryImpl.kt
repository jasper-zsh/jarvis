package pro.sihao.jarvis.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pro.sihao.jarvis.core.data.database.JarvisDatabase
import pro.sihao.jarvis.core.data.database.entity.MessageEntity
import pro.sihao.jarvis.features.mapper.MessageMapper
import pro.sihao.jarvis.core.domain.model.Message
import pro.sihao.jarvis.core.domain.repository.MessageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val database: JarvisDatabase,
    private val pipeCatService: pro.sihao.jarvis.core.domain.service.PipeCatService? = null
) : MessageRepository {

    override fun getAllMessages(): Flow<List<Message>> {
        return database.messageDao().getAllMessages().map { entities ->
            MessageMapper.toDomainList(entities)
        }
    }

    override suspend fun getRecentMessages(limit: Int): Flow<List<Message>> {
        return database.messageDao().getRecentMessages(limit).map { entities ->
            MessageMapper.toDomainList(entities)
        }
    }

    override suspend fun getMessagesPaginated(limit: Int, offset: Int): List<Message> {
        val entities = database.messageDao().getMessagesPaginated(limit, offset)
        return MessageMapper.toDomainList(entities)
    }

    override suspend fun insertMessage(message: Message): Long {
        val entity = MessageMapper.toEntity(message)
        val messageId = database.messageDao().insertMessage(entity)
        
        // Notify PipeCat service of new message for real-time processing
        pipeCatService?.let { service ->
            try {
                // This could be used to sync messages to real-time session
                // Implementation depends on specific PipeCat integration requirements
            } catch (e: Exception) {
                // Log error but don't fail main message insertion
            }
        }
        
        return messageId
    }

    override suspend fun upsertMessage(message: Message): Long {
        val entity = MessageMapper.toEntity(message)
        return database.messageDao().insertMessage(entity)
    }

    override suspend fun insertMessages(messages: List<Message>) {
        val entities = MessageMapper.toEntityList(messages)
        database.messageDao().insertMessages(entities)
    }

    override suspend fun deleteMessage(message: Message) {
        // For now, we can't directly delete by entity match, so this method is limited
        // In a real implementation, you'd want to delete by some unique identifier
        // or by content + timestamp combination
    }

    override suspend fun deleteLoadingMessages() {
        database.messageDao().deleteLoadingMessages()
    }

    override suspend fun clearAllMessages() {
        database.messageDao().clearAllMessages()
    }

    override suspend fun clearConversation(conversationId: Long?) {
        // Currently single-conversation; reuse clearAllMessages
        database.messageDao().clearConversation()
    }

    override suspend fun getMessageCount(): Int {
        return database.messageDao().getMessageCount()
    }
    
    /**
     * Get recent messages for real-time processing
     */
    suspend fun getRecentMessagesForRealtime(limit: Int = 10): List<Message> {
        val entities = database.messageDao().getRecentMessages(limit).first()
        return MessageMapper.toDomainList(entities)
    }
}
