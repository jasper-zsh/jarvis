package pro.sihao.jarvis.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pro.sihao.jarvis.data.network.NetworkMonitor
import pro.sihao.jarvis.data.storage.SecureStorage
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.domain.repository.MessageRepository
import pro.sihao.jarvis.domain.service.LLMService
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class ChatViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var messageRepository: MessageRepository

    @Mock
    private lateinit var llmService: LLMService

    @Mock
    private lateinit var secureStorage: SecureStorage

    @Mock
    private lateinit var networkMonitor: NetworkMonitor

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Setup default mocks
        whenever(messageRepository.getAllMessages()).thenReturn(flowOf(emptyList()))
        whenever(secureStorage.hasApiKey()).thenReturn(true)
        whenever(secureStorage.getApiKey()).thenReturn("test-api-key")
        whenever(networkMonitor.isConnected).thenReturn(flowOf(true))

        viewModel = ChatViewModel(
            messageRepository,
            llmService,
            secureStorage,
            networkMonitor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have correct default values`() = runTest {
        // When
        val initialState = viewModel.uiState.first()

        // Then
        assertNotNull(initialState)
        assertTrue(initialState.messages.isEmpty())
        assertEquals("", initialState.inputMessage)
        assertFalse(initialState.isLoading)
        assertNull(initialState.errorMessage)
        assertTrue(initialState.hasApiKey)
        assertTrue(initialState.isConnected)
        assertFalse(initialState.navigateToSettings)
    }

    @Test
    fun `onMessageChanged should update input message in state`() = runTest {
        // Given
        val newMessage = "Hello world"

        // When
        viewModel.onMessageChanged(newMessage)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val updatedState = viewModel.uiState.first()
        assertEquals(newMessage, updatedState.inputMessage)
    }

    @Test
    fun `sendMessage with empty message should not send`() = runTest {
        // When
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(messageRepository).getAllMessages()
        // Should not call insertMessage or llmService.sendMessage
    }

    @Test
    fun `sendMessage without API key should show error`() = runTest {
        // Given
        whenever(secureStorage.hasApiKey()).thenReturn(false)
        viewModel = ChatViewModel(messageRepository, llmService, secureStorage, networkMonitor)
        viewModel.onMessageChanged("Test message")

        // When
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals("Please set up your API key first", state.errorMessage)
    }

    @Test
    fun `sendMessage without network should show error`() = runTest {
        // Given
        whenever(networkMonitor.isConnected).thenReturn(flowOf(false))
        viewModel = ChatViewModel(messageRepository, llmService, secureStorage, networkMonitor)
        viewModel.onMessageChanged("Test message")

        // When
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals("No internet connection", state.errorMessage)
    }

    @Test
    fun `sendMessage should insert user message and clear input`() = runTest {
        // Given
        val testMessage = "Hello world"
        val timestamp = Date()
        viewModel.onMessageChanged(testMessage)

        // When
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(messageRepository).insertMessage(org.mockito.kotlin.any())
        val state = viewModel.uiState.first()
        assertEquals("", state.inputMessage)
        assertTrue(state.isLoading)
    }

    @Test
    fun `clearError should remove error message from state`() = runTest {
        // Given - set an error state first
        whenever(secureStorage.hasApiKey()).thenReturn(false)
        viewModel = ChatViewModel(messageRepository, llmService, secureStorage, networkMonitor)
        viewModel.onMessageChanged("Test message")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error is present
        var state = viewModel.uiState.first()
        assertNotNull(state.errorMessage)

        // When
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        state = viewModel.uiState.first()
        assertNull(state.errorMessage)
    }

    @Test
    fun `refreshApiKeyStatus should update hasApiKey in state`() = runTest {
        // Given
        whenever(secureStorage.hasApiKey()).thenReturn(false)

        // When
        viewModel.refreshApiKeyStatus()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.hasApiKey)
    }

    @Test
    fun `navigateToSettings should update navigation flag`() = runTest {
        // When
        viewModel.navigateToSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state.navigateToSettings)
    }

    @Test
    fun `onSettingsNavigated should clear navigation flag`() = runTest {
        // Given - set navigation flag first
        viewModel.navigateToSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify flag is set
        var state = viewModel.uiState.first()
        assertTrue(state.navigateToSettings)

        // When
        viewModel.onSettingsNavigated()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        state = viewModel.uiState.first()
        assertFalse(state.navigateToSettings)
    }
}