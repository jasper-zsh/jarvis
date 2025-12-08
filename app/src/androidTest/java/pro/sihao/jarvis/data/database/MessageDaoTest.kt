package pro.sihao.jarvis.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pro.sihao.jarvis.data.database.dao.MessageDao
import pro.sihao.jarvis.data.database.entity.MessageEntity
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MessageDaoTest {

    private lateinit var database: JarvisDatabase
    private lateinit var messageDao: MessageDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            JarvisDatabase::class.java
        ).allowMainThreadQueries().build()
        messageDao = database.messageDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetMessage() = runTest {
        // Given
        val message = MessageEntity(
            id = null,
            content = "Test message",
            timestamp = System.currentTimeMillis(),
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )

        // When
        val insertedId = messageDao.insertMessage(message)
        val messages = messageDao.getAllMessages().first()

        // Then
        assertNotNull(insertedId)
        assertEquals(1, messages.size)
        assertEquals("Test message", messages[0].content)
        assertEquals(true, messages[0].isFromUser)
        assertEquals(insertedId, messages[0].id)
    }

    @Test
    fun getAllMessages_shouldReturnMessagesInTimestampOrder() = runTest {
        // Given
        val baseTimestamp = System.currentTimeMillis()
        val message1 = MessageEntity(
            id = null,
            content = "First message",
            timestamp = baseTimestamp,
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )
        val message2 = MessageEntity(
            id = null,
            content = "Second message",
            timestamp = baseTimestamp + 1000,
            isFromUser = false,
            isLoading = false,
            errorMessage = null
        )

        // When
        messageDao.insertMessage(message1)
        messageDao.insertMessage(message2)
        val messages = messageDao.getAllMessages().first()

        // Then
        assertEquals(2, messages.size)
        assertEquals("First message", messages[0].content)
        assertEquals("Second message", messages[1].content)
    }

    @Test
    fun getRecentMessages_shouldReturnLimitedResults() = runTest {
        // Given
        val messages = (1..10).map { i ->
            MessageEntity(
                id = null,
                content = "Message $i",
                timestamp = System.currentTimeMillis() + i,
                isFromUser = i % 2 == 1,
                isLoading = false,
                errorMessage = null
            )
        }

        // When
        messageDao.insertMessages(messages)
        val recentMessages = messageDao.getRecentMessages(limit = 5).first()

        // Then
        assertEquals(5, recentMessages.size)
        // Should return messages in DESC order (most recent first)
        assertEquals("Message 10", recentMessages[0].content)
        assertEquals("Message 6", recentMessages[4].content)
    }

    @Test
    fun insertMessages_shouldInsertMultipleMessages() = runTest {
        // Given
        val messages = listOf(
            MessageEntity(
                id = null,
                content = "Message 1",
                timestamp = System.currentTimeMillis(),
                isFromUser = true,
                isLoading = false,
                errorMessage = null
            ),
            MessageEntity(
                id = null,
                content = "Message 2",
                timestamp = System.currentTimeMillis() + 1,
                isFromUser = false,
                isLoading = false,
                errorMessage = null
            )
        )

        // When
        messageDao.insertMessages(messages)
        val allMessages = messageDao.getAllMessages().first()

        // Then
        assertEquals(2, allMessages.size)
        assertEquals("Message 1", allMessages[0].content)
        assertEquals("Message 2", allMessages[1].content)
    }

    @Test
    fun deleteMessage_shouldRemoveSpecificMessage() = runTest {
        // Given
        val message = MessageEntity(
            id = null,
            content = "Message to delete",
            timestamp = System.currentTimeMillis(),
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )
        val insertedId = messageDao.insertMessage(message)

        // When
        messageDao.deleteMessage(insertedId)
        val messages = messageDao.getAllMessages().first()

        // Then
        assertTrue(messages.isEmpty())
    }

    @Test
    fun deleteLoadingMessages_shouldRemoveOnlyLoadingMessages() = runTest {
        // Given
        val normalMessage = MessageEntity(
            id = null,
            content = "Normal message",
            timestamp = System.currentTimeMillis(),
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )
        val loadingMessage = MessageEntity(
            id = null,
            content = "",
            timestamp = System.currentTimeMillis() + 1,
            isFromUser = false,
            isLoading = true,
            errorMessage = null
        )
        messageDao.insertMessage(normalMessage)
        messageDao.insertMessage(loadingMessage)

        // When
        messageDao.deleteLoadingMessages()
        val messages = messageDao.getAllMessages().first()

        // Then
        assertEquals(1, messages.size)
        assertEquals("Normal message", messages[0].content)
        assertFalse(messages[0].isLoading)
    }

    @Test
    fun clearAllMessages_shouldRemoveAllMessages() = runTest {
        // Given
        val messages = listOf(
            MessageEntity(
                id = null,
                content = "Message 1",
                timestamp = System.currentTimeMillis(),
                isFromUser = true,
                isLoading = false,
                errorMessage = null
            ),
            MessageEntity(
                id = null,
                content = "Message 2",
                timestamp = System.currentTimeMillis() + 1,
                isFromUser = false,
                isLoading = true,
                errorMessage = null
            )
        )
        messageDao.insertMessages(messages)

        // When
        messageDao.clearAllMessages()
        val allMessages = messageDao.getAllMessages().first()

        // Then
        assertTrue(allMessages.isEmpty())
    }

    @Test
    fun getMessageCount_shouldReturnCorrectCount() = runTest {
        // Given
        assertEquals(0, messageDao.getMessageCount())

        // When
        val messages = listOf(
            MessageEntity(
                id = null,
                content = "Message 1",
                timestamp = System.currentTimeMillis(),
                isFromUser = true,
                isLoading = false,
                errorMessage = null
            ),
            MessageEntity(
                id = null,
                content = "Message 2",
                timestamp = System.currentTimeMillis() + 1,
                isFromUser = false,
                isLoading = false,
                errorMessage = null
            )
        )
        messageDao.insertMessages(messages)

        // Then
        assertEquals(2, messageDao.getMessageCount())
    }

    @Test
    fun upsertMessage_shouldReplaceExistingMessage() = runTest {
        // Given
        val originalMessage = MessageEntity(
            id = null,
            content = "Original message",
            timestamp = System.currentTimeMillis(),
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )
        val insertedId = messageDao.insertMessage(originalMessage)

        // When
        val updatedMessage = MessageEntity(
            id = insertedId,
            content = "Updated message",
            timestamp = System.currentTimeMillis(),
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )
        messageDao.insertMessage(updatedMessage)
        val messages = messageDao.getAllMessages().first()

        // Then
        assertEquals(1, messages.size)
        assertEquals("Updated message", messages[0].content)
        assertEquals(insertedId, messages[0].id)
    }
}