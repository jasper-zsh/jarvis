package pro.sihao.jarvis.data.mapper

import pro.sihao.jarvis.data.database.entity.MessageEntity
import pro.sihao.jarvis.domain.model.Message
import java.util.Date

object MessageMapper {
    fun toDomain(entity: MessageEntity): Message {
        return Message(
            id = entity.id,
            content = entity.content,
            timestamp = Date(entity.timestamp),
            isFromUser = entity.isFromUser,
            isLoading = entity.isLoading,
            errorMessage = entity.errorMessage
        )
    }

    fun toEntity(domain: Message): MessageEntity {
        return MessageEntity(
            id = domain.id,
            content = domain.content,
            timestamp = domain.timestamp.time,
            isFromUser = domain.isFromUser,
            isLoading = domain.isLoading,
            errorMessage = domain.errorMessage
        )
    }

    fun toDomainList(entities: List<MessageEntity>): List<Message> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(domains: List<Message>): List<MessageEntity> {
        return domains.map { toEntity(it) }
    }
}