package pro.sihao.jarvis.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pro.sihao.jarvis.data.database.JarvisDatabase
import pro.sihao.jarvis.data.database.entity.MessageEntity
import pro.sihao.jarvis.data.mapper.MessageMapper
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.domain.repository.MessageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val database: JarvisDatabase
) : MessageRepository {

    override fun getAllMessages(): Flow<List<Message>> {
        return database.messageDao().getAllMessages().map { entities ->
            MessageMapper.toDomainList(entities)
        }
    }

    override suspend fun insertMessage(message: Message): Long {
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

    override suspend fun getMessageCount(): Int {
        return database.messageDao().getMessageCount()
    }
}