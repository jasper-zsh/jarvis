package pro.sihao.jarvis.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import pro.sihao.jarvis.data.storage.SecureStorage
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var secureStorage: SecureStorage

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Setup default mocks
        whenever(secureStorage.getApiKey()).thenReturn(null)

        viewModel = SettingsViewModel(secureStorage)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty API key`() = runTest {
        // When
        val initialState = viewModel.uiState.first()

        // Then
        assertNotNull(initialState)
        assertEquals("", initialState.apiKey)
        assertFalse(initialState.showApiKey)
        assertNull(initialState.errorMessage)
        assertFalse(initialState.hasChanges)
    }

    @Test
    fun `initial state should load existing API key from secure storage`() = runTest {
        // Given
        val existingApiKey = "existing-api-key"
        whenever(secureStorage.getApiKey()).thenReturn(existingApiKey)

        // When
        viewModel = SettingsViewModel(secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(existingApiKey, state.apiKey)
    }

    @Test
    fun `onApiKeyChanged should update API key in state`() = runTest {
        // Given
        val newApiKey = "new-api-key"

        // When
        viewModel.onApiKeyChanged(newApiKey)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(newApiKey, state.apiKey)
    }

    @Test
    fun `onApiKeyChanged should detect changes when different from saved key`() = runTest {
        // Given
        val savedApiKey = "saved-api-key"
        val newApiKey = "new-api-key"
        whenever(secureStorage.getApiKey()).thenReturn(savedApiKey)

        // When
        viewModel = SettingsViewModel(secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onApiKeyChanged(newApiKey)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state.hasChanges)
    }

    @Test
    fun `onApiKeyChanged should not detect changes when same as saved key`() = runTest {
        // Given
        val savedApiKey = "same-api-key"
        whenever(secureStorage.getApiKey()).thenReturn(savedApiKey)

        // When
        viewModel = SettingsViewModel(secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onApiKeyChanged(savedApiKey)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.hasChanges)
    }

    @Test
    fun `toggleApiKeyVisibility should toggle showApiKey flag`() = runTest {
        // Given
        val initialState = viewModel.uiState.first()
        assertFalse(initialState.showApiKey)

        // When
        viewModel.toggleApiKeyVisibility()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should be true now
        var state = viewModel.uiState.first()
        assertTrue(state.showApiKey)

        // When - toggle again
        viewModel.toggleApiKeyVisibility()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should be false again
        state = viewModel.uiState.first()
        assertFalse(state.showApiKey)
    }

    @Test
    fun `saveApiKey with empty key should show error`() = runTest {
        // When
        viewModel.saveApiKey()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals("API key cannot be empty", state.errorMessage)
        verifyNoInteractions(secureStorage)
    }

    @Test
    fun `saveApiKey with valid key should save to secure storage`() = runTest {
        // Given
        val apiKey = "valid-api-key"
        viewModel.onApiKeyChanged(apiKey)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.saveApiKey()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(secureStorage).saveApiKey(apiKey)
        val state = viewModel.uiState.first()
        assertNull(state.errorMessage)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `saveApiKey should clear error message on success`() = runTest {
        // Given
        viewModel.onApiKeyChanged("valid-key")

        // Set an error first
        viewModel.onApiKeyChanged("")
        viewModel.saveApiKey()
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.first()
        assertNotNull(state.errorMessage)

        // When
        viewModel.onApiKeyChanged("valid-api-key")
        viewModel.saveApiKey()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        state = viewModel.uiState.first()
        assertNull(state.errorMessage)
    }

    @Test
    fun `clearApiKey should remove API key from secure storage`() = runTest {
        // Given
        whenever(secureStorage.getApiKey()).thenReturn("existing-key")
        viewModel = SettingsViewModel(secureStorage)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearApiKey()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(secureStorage).clearApiKey()
        val state = viewModel.uiState.first()
        assertEquals("", state.apiKey)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `clearError should remove error message from state`() = runTest {
        // Given - create error first
        viewModel.saveApiKey()
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.first()
        assertNotNull(state.errorMessage)

        // When
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        state = viewModel.uiState.first()
        assertNull(state.errorMessage)
    }
}