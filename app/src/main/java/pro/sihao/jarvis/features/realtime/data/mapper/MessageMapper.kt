package pro.sihao.jarvis.features.mapper

import pro.sihao.jarvis.core.data.database.entity.MessageEntity
import pro.sihao.jarvis.core.domain.model.ContentType
import pro.sihao.jarvis.core.domain.model.Message
import java.util.Date

object MessageMapper {
    fun toDomain(entity: MessageEntity): Message {
        return Message(
            id = entity.id,
            content = entity.content,
            timestamp = Date(entity.timestamp),
            isFromUser = entity.isFromUser,
            isLoading = entity.isLoading,
            errorMessage = entity.errorMessage,
            contentType = try {
                ContentType.valueOf(entity.contentType)
            } catch (e: IllegalArgumentException) {
                ContentType.TEXT // Fallback for old records
            },
            mediaUrl = entity.mediaUrl,
            duration = entity.duration,
            thumbnailUrl = entity.thumbnailUrl,
            mediaSize = entity.mediaSize
        )
    }

    fun toEntity(domain: Message): MessageEntity {
        return MessageEntity(
            id = domain.id,
            content = domain.content,
            timestamp = domain.timestamp.time,
            isFromUser = domain.isFromUser,
            isLoading = domain.isLoading,
            errorMessage = domain.errorMessage,
            contentType = domain.contentType.name,
            mediaUrl = domain.mediaUrl,
            duration = domain.duration,
            thumbnailUrl = domain.thumbnailUrl,
            mediaSize = domain.mediaSize
        )
    }

    fun toDomainList(entities: List<MessageEntity>): List<Message> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(domains: List<Message>): List<MessageEntity> {
        return domains.map { toEntity(it) }
    }
}