package pro.sihao.jarvis.data.mapper

import org.junit.Test
import pro.sihao.jarvis.core.data.database.entity.MessageEntity
import pro.sihao.jarvis.core.domain.model.ContentType
import pro.sihao.jarvis.core.domain.model.Message
import pro.sihao.jarvis.features.realtime.data.mapper.MessageMapper
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MessageMapperTest {

    @Test
    fun `toDomain should convert entity to message correctly`() {
        // Given
        val timestamp = System.currentTimeMillis()
        val entity = MessageEntity(
            id = 1L,
            content = "Hello world",
            timestamp = timestamp,
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )

        // When
        val message = MessageMapper.toDomain(entity)

        // Then
        assertNotNull(message)
        assertEquals(entity.id, message.id)
        assertEquals(entity.content, message.content)
        assertEquals(Date(entity.timestamp), message.timestamp)
        assertEquals(entity.isFromUser, message.isFromUser)
        assertEquals(entity.isLoading, message.isLoading)
        assertEquals(entity.errorMessage, message.errorMessage)
    }

    @Test
    fun `toEntity should convert message to entity correctly`() {
        // Given
        val timestamp = Date()
        val message = Message(
            id = 1L,
            content = "Hello world",
            timestamp = timestamp,
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )

        // When
        val entity = MessageMapper.toEntity(message)

        // Then
        assertNotNull(entity)
        assertEquals(message.id, entity.id)
        assertEquals(message.content, entity.content)
        assertEquals(message.timestamp.time, entity.timestamp)
        assertEquals(message.isFromUser, entity.isFromUser)
        assertEquals(message.isLoading, entity.isLoading)
        assertEquals(message.errorMessage, entity.errorMessage)
    }

    @Test
    fun `toDomainList should convert list of entities to list of messages`() {
        // Given
        val entities = listOf(
            MessageEntity(
                id = 1L,
                content = "Message 1",
                timestamp = System.currentTimeMillis(),
                isFromUser = true,
                isLoading = false,
                errorMessage = null
            ),
            MessageEntity(
                id = 2L,
                content = "Message 2",
                timestamp = System.currentTimeMillis(),
                isFromUser = false,
                isLoading = false,
                errorMessage = null
            )
        )

        // When
        val messages = MessageMapper.toDomainList(entities)

        // Then
        assertEquals(2, messages.size)
        assertEquals("Message 1", messages[0].content)
        assertEquals(true, messages[0].isFromUser)
        assertEquals("Message 2", messages[1].content)
        assertEquals(false, messages[1].isFromUser)
    }

    @Test
    fun `toEntityList should convert list of messages to list of entities`() {
        // Given
        val messages = listOf(
            Message(
                id = 1L,
                content = "Message 1",
                timestamp = Date(),
                isFromUser = true,
                isLoading = false,
                errorMessage = null
            ),
            Message(
                id = 2L,
                content = "Message 2",
                timestamp = Date(),
                isFromUser = false,
                isLoading = false,
                errorMessage = null
            )
        )

        // When
        val entities = MessageMapper.toEntityList(messages)

        // Then
        assertEquals(2, entities.size)
        assertEquals("Message 1", entities[0].content)
        assertEquals(true, entities[0].isFromUser)
        assertEquals("Message 2", entities[1].content)
        assertEquals(false, entities[1].isFromUser)
    }

    @Test
    fun `toDomain should handle entity with error message`() {
        // Given
        val entity = MessageEntity(
            id = 1L,
            content = "",
            timestamp = System.currentTimeMillis(),
            isFromUser = false,
            isLoading = false,
            errorMessage = "API Error"
        )

        // When
        val message = MessageMapper.toDomain(entity)

        // Then
        assertNotNull(message)
        assertEquals(entity.errorMessage, message.errorMessage)
    }

    @Test
    fun `toDomain should handle entity with loading state`() {
        // Given
        val entity = MessageEntity(
            id = 1L,
            content = "",
            timestamp = System.currentTimeMillis(),
            isFromUser = false,
            isLoading = true,
            errorMessage = null
        )

        // When
        val message = MessageMapper.toDomain(entity)

        // Then
        assertNotNull(message)
        assertEquals(entity.isLoading, message.isLoading)
    }

    @Test
    fun `toDomain should convert voice message entity correctly`() {
        // Given
        val entity = MessageEntity(
            id = 1L,
            content = "Voice message (1:30)",
            timestamp = System.currentTimeMillis(),
            isFromUser = true,
            contentType = "VOICE",
            mediaUrl = "/storage/voice/recording.aac",
            duration = 90000L,
            mediaSize = 147456L
        )

        // When
        val message = MessageMapper.toDomain(entity)

        // Then
        assertNotNull(message)
        assertEquals(ContentType.VOICE, message.contentType)
        assertEquals(entity.mediaUrl, message.mediaUrl)
        assertEquals(entity.duration, message.duration)
        assertEquals(entity.mediaSize, message.mediaSize)
        assertNull(message.thumbnailUrl)
    }

    @Test
    fun `toDomain should convert photo message entity correctly`() {
        // Given
        val entity = MessageEntity(
            id = 1L,
            content = "Photo from camera",
            timestamp = System.currentTimeMillis(),
            isFromUser = true,
            contentType = "PHOTO",
            mediaUrl = "/storage/photos/image.jpg",
            thumbnailUrl = "/storage/thumbnails/image_thumb.jpg",
            mediaSize = 2048000L
        )

        // When
        val message = MessageMapper.toDomain(entity)

        // Then
        assertNotNull(message)
        assertEquals(ContentType.PHOTO, message.contentType)
        assertEquals(entity.mediaUrl, message.mediaUrl)
        assertEquals(entity.thumbnailUrl, message.thumbnailUrl)
        assertEquals(entity.mediaSize, message.mediaSize)
        assertNull(message.duration)
    }

    @Test
    fun `toDomain should handle legacy entity without content type`() {
        // Given
        val entity = MessageEntity(
            id = 1L,
            content = "Legacy text message",
            timestamp = System.currentTimeMillis(),
            isFromUser = true,
            contentType = "INVALID_TYPE" // Invalid type should fallback to TEXT
        )

        // When
        val message = MessageMapper.toDomain(entity)

        // Then
        assertNotNull(message)
        assertEquals(ContentType.TEXT, message.contentType) // Should fallback to TEXT
        assertNull(message.mediaUrl)
        assertNull(message.duration)
        assertNull(message.thumbnailUrl)
        assertNull(message.mediaSize)
    }

    @Test
    fun `toEntity should convert voice message correctly`() {
        // Given
        val message = Message(
            id = 1L,
            content = "Voice recording",
            timestamp = Date(),
            isFromUser = true,
            contentType = ContentType.VOICE,
            mediaUrl = "/storage/voice/test.aac",
            duration = 120000L,
            mediaSize = 196608L
        )

        // When
        val entity = MessageMapper.toEntity(message)

        // Then
        assertNotNull(entity)
        assertEquals("VOICE", entity.contentType)
        assertEquals(message.mediaUrl, entity.mediaUrl)
        assertEquals(message.duration, entity.duration)
        assertEquals(message.mediaSize, entity.mediaSize)
        assertNull(entity.thumbnailUrl)
    }

    @Test
    fun `toEntity should convert photo message correctly`() {
        // Given
        val message = Message(
            id = 1L,
            content = "Photo",
            timestamp = Date(),
            isFromUser = true,
            contentType = ContentType.PHOTO,
            mediaUrl = "/storage/photos/test.jpg",
            thumbnailUrl = "/storage/thumbnails/test_thumb.jpg",
            mediaSize = 1024000L
        )

        // When
        val entity = MessageMapper.toEntity(message)

        // Then
        assertNotNull(entity)
        assertEquals("PHOTO", entity.contentType)
        assertEquals(message.mediaUrl, entity.mediaUrl)
        assertEquals(message.thumbnailUrl, entity.thumbnailUrl)
        assertEquals(message.mediaSize, entity.mediaSize)
        assertNull(entity.duration)
    }
}