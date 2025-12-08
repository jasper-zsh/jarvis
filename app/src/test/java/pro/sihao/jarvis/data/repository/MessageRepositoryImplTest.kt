package pro.sihao.jarvis.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pro.sihao.jarvis.data.database.JarvisDatabase
import pro.sihao.jarvis.data.database.dao.MessageDao
import pro.sihao.jarvis.data.database.entity.MessageEntity
import pro.sihao.jarvis.domain.model.Message
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class MessageRepositoryImplTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var database: JarvisDatabase

    @Mock
    private lateinit var messageDao: MessageDao

    private lateinit var repository: MessageRepositoryImpl

    @Before
    fun setup() {
        whenever(database.messageDao()).thenReturn(messageDao)
        repository = MessageRepositoryImpl(database)
    }

    @Test
    fun `getAllMessages should return flow of messages from database`() = runTest {
        // Given
        val entities = listOf(
            MessageEntity(
                id = 1,
                content = "Hello",
                timestamp = System.currentTimeMillis(),
                isFromUser = true,
                isLoading = false,
                errorMessage = null
            ),
            MessageEntity(
                id = 2,
                content = "Hi there!",
                timestamp = System.currentTimeMillis(),
                isFromUser = false,
                isLoading = false,
                errorMessage = null
            )
        )
        whenever(messageDao.getAllMessages()).thenReturn(flowOf(entities))

        // When
        val result = repository.getAllMessages().first()

        // Then
        assertEquals(2, result.size)
        assertEquals("Hello", result[0].content)
        assertEquals(true, result[0].isFromUser)
        assertEquals("Hi there!", result[1].content)
        assertEquals(false, result[1].isFromUser)
        verify(messageDao).getAllMessages()
    }

    @Test
    fun `insertMessage should convert to entity and insert to database`() = runTest {
        // Given
        val message = Message(
            id = null,
            content = "Test message",
            timestamp = Date(),
            isFromUser = true,
            isLoading = false,
            errorMessage = null
        )
        val expectedId = 123L
        whenever(messageDao.insertMessage(org.mockito.kotlin.any())).thenReturn(expectedId)

        // When
        val result = repository.insertMessage(message)

        // Then
        assertEquals(expectedId, result)
        verify(messageDao).insertMessage(org.mockito.kotlin.any())
    }

    @Test
    fun `insertMessages should convert to entities and insert to database`() = runTest {
        // Given
        val messages = listOf(
            Message(
                id = null,
                content = "Message 1",
                timestamp = Date(),
                isFromUser = true,
                isLoading = false,
                errorMessage = null
            ),
            Message(
                id = null,
                content = "Message 2",
                timestamp = Date(),
                isFromUser = false,
                isLoading = false,
                errorMessage = null
            )
        )

        // When
        repository.insertMessages(messages)

        // Then
        verify(messageDao).insertMessages(org.mockito.kotlin.any())
    }

    @Test
    fun `deleteLoadingMessages should call dao deleteLoadingMessages`() = runTest {
        // When
        repository.deleteLoadingMessages()

        // Then
        verify(messageDao).deleteLoadingMessages()
    }

    @Test
    fun `clearAllMessages should call dao clearAllMessages`() = runTest {
        // When
        repository.clearAllMessages()

        // Then
        verify(messageDao).clearAllMessages()
    }

    @Test
    fun `getMessageCount should return count from dao`() = runTest {
        // Given
        val expectedCount = 42
        whenever(messageDao.getMessageCount()).thenReturn(expectedCount)

        // When
        val result = repository.getMessageCount()

        // Then
        assertEquals(expectedCount, result)
        verify(messageDao).getMessageCount()
    }
}