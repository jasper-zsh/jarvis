package pro.sihao.jarvis.domain.model

import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageTest {

    @Test
    fun `create message with all properties should work correctly`() {
        // Given
        val id = 1L
        val content = "Hello world"
        val timestamp = Date()
        val isFromUser = true
        val isLoading = false
        val errorMessage = null

        // When
        val message = Message(
            id = id,
            content = content,
            timestamp = timestamp,
            isFromUser = isFromUser,
            isLoading = isLoading,
            errorMessage = errorMessage
        )

        // Then
        assertNotNull(message)
        assertEquals(id, message.id)
        assertEquals(content, message.content)
        assertEquals(timestamp, message.timestamp)
        assertEquals(isFromUser, message.isFromUser)
        assertEquals(isLoading, message.isLoading)
        assertEquals(errorMessage, message.errorMessage)
    }

    @Test
    fun `create message without id should work correctly`() {
        // Given
        val content = "Test message"
        val timestamp = Date()
        val isFromUser = false

        // When
        val message = Message(
            id = null,
            content = content,
            timestamp = timestamp,
            isFromUser = isFromUser,
            isLoading = false,
            errorMessage = null
        )

        // Then
        assertNotNull(message)
        assertNull(message.id)
        assertEquals(content, message.content)
        assertEquals(timestamp, message.timestamp)
        assertEquals(isFromUser, message.isFromUser)
        assertFalse(message.isLoading)
        assertNull(message.errorMessage)
    }

    @Test
    fun `create loading message should work correctly`() {
        // Given
        val content = ""
        val timestamp = Date()

        // When
        val message = Message(
            id = null,
            content = content,
            timestamp = timestamp,
            isFromUser = false,
            isLoading = true,
            errorMessage = null
        )

        // Then
        assertNotNull(message)
        assertTrue(message.isLoading)
        assertFalse(message.isFromUser)
        assertEquals(content, message.content)
        assertEquals(timestamp, message.timestamp)
    }

    @Test
    fun `create error message should work correctly`() {
        // Given
        val content = ""
        val timestamp = Date()
        val errorMessage = "Network error occurred"

        // When
        val message = Message(
            id = null,
            content = content,
            timestamp = timestamp,
            isFromUser = false,
            isLoading = false,
            errorMessage = errorMessage
        )

        // Then
        assertNotNull(message)
        assertEquals(errorMessage, message.errorMessage)
        assertFalse(message.isLoading)
        assertFalse(message.isFromUser)
        assertEquals(content, message.content)
        assertEquals(timestamp, message.timestamp)
    }

    @Test
    fun `message equality should work correctly`() {
        // Given
        val timestamp = Date()
        val message1 = Message(
            id = 1L,
            content = "Test",
            timestamp = timestamp,
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )
        val message2 = Message(
            id = 1L,
            content = "Test",
            timestamp = timestamp,
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )
        val message3 = Message(
            id = 2L,
            content = "Test",
            timestamp = timestamp,
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )

        // Then
        assertEquals(message1, message2)
        assertTrue(message1 != message3)
    }

    @Test
    fun `create voice message should work correctly`() {
        // Given
        val content = "Voice message (2:30)"
        val timestamp = Date()
        val mediaUrl = "/storage/emulated/0/voice/voice_123.aac"
        val duration = 150000L // 2.5 minutes in milliseconds

        // When
        val message = Message(
            content = content,
            timestamp = timestamp,
            isFromUser = true,
            contentType = ContentType.VOICE,
            mediaUrl = mediaUrl,
            duration = duration,
            mediaSize = 245760L
        )

        // Then
        assertNotNull(message)
        assertEquals(content, message.content)
        assertEquals(ContentType.VOICE, message.contentType)
        assertEquals(mediaUrl, message.mediaUrl)
        assertEquals(duration, message.duration)
        assertEquals(245760L, message.mediaSize)
        assertNull(message.thumbnailUrl)
    }

    @Test
    fun `create photo message should work correctly`() {
        // Given
        val content = "Photo description"
        val timestamp = Date()
        val mediaUrl = "/storage/emulated/0/photos/photo_456.jpg"
        val thumbnailUrl = "/storage/emulated/0/thumbnails/photo_456_thumb.jpg"

        // When
        val message = Message(
            content = content,
            timestamp = timestamp,
            isFromUser = true,
            contentType = ContentType.PHOTO,
            mediaUrl = mediaUrl,
            thumbnailUrl = thumbnailUrl,
            mediaSize = 1024000L
        )

        // Then
        assertNotNull(message)
        assertEquals(content, message.content)
        assertEquals(ContentType.PHOTO, message.contentType)
        assertEquals(mediaUrl, message.mediaUrl)
        assertEquals(thumbnailUrl, message.thumbnailUrl)
        assertEquals(1024000L, message.mediaSize)
        assertNull(message.duration)
    }

    @Test
    fun `create text message should default to TEXT content type`() {
        // Given
        val content = "Regular text message"
        val timestamp = Date()

        // When
        val message = Message(
            content = content,
            timestamp = timestamp,
            isFromUser = true
        )

        // Then
        assertNotNull(message)
        assertEquals(content, message.content)
        assertEquals(ContentType.TEXT, message.contentType)
        assertNull(message.mediaUrl)
        assertNull(message.duration)
        assertNull(message.thumbnailUrl)
        assertNull(message.mediaSize)
    }
}