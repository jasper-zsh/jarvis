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
}