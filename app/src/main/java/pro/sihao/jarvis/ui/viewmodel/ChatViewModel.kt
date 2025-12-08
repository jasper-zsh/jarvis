package pro.sihao.jarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pro.sihao.jarvis.data.network.NetworkMonitor
import pro.sihao.jarvis.data.storage.SecureStorage
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.domain.repository.MessageRepository
import pro.sihao.jarvis.domain.service.LLMService
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val llmService: LLMService,
    private val secureStorage: SecureStorage,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
        checkApiKey()
        observeNetworkState()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            messageRepository.getAllMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun checkApiKey() {
        _uiState.update { it.copy(hasApiKey = secureStorage.hasApiKey()) }
    }

    fun refreshApiKeyStatus() {
        checkApiKey()
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                _uiState.update { it.copy(isConnected = isConnected) }
            }
        }
    }

    fun onMessageChanged(message: String) {
        _uiState.update { it.copy(inputMessage = message) }
    }

    fun sendMessage() {
        val currentMessage = _uiState.value.inputMessage.trim()
        if (currentMessage.isBlank()) return

        val apiKey = secureStorage.getApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Please set up your API key first"
                )
            }
            return
        }

        if (!_uiState.value.isConnected) {
            _uiState.update {
                it.copy(
                    errorMessage = "No internet connection",
                    isLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                // Create user message
                val userMessage = Message(
                    content = currentMessage,
                    timestamp = Date(),
                    isFromUser = true
                )

                // Insert user message
                messageRepository.insertMessage(userMessage)

                // Clear input and show loading
                _uiState.update {
                    it.copy(
                        inputMessage = "",
                        isLoading = true,
                        errorMessage = null
                    )
                }

                // Create loading message for AI response
                val loadingMessage = Message(
                    content = "",
                    timestamp = Date(),
                    isFromUser = false,
                    isLoading = true
                )
                messageRepository.insertMessage(loadingMessage)

                // Get current conversation history
                val messages = _uiState.value.messages + userMessage

                // Send to LLM service
                llmService.sendMessage(currentMessage, messages, apiKey).collect { result ->
                    result.fold(
                        onSuccess = { aiResponse ->
                            // Remove loading message and add actual AI response
                            messageRepository.deleteLoadingMessages()

                            val aiMessage = Message(
                                content = aiResponse,
                                timestamp = Date(),
                                isFromUser = false
                            )
                            messageRepository.insertMessage(aiMessage)
                            _uiState.update { it.copy(isLoading = false) }
                        },
                        onFailure = { error ->
                            // Remove loading message on error
                            messageRepository.deleteLoadingMessages()

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Failed to get response: ${error.message}"
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error sending message: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun navigateToSettings() {
        _uiState.update { it.copy(navigateToSettings = true) }
    }

    fun onSettingsNavigated() {
        _uiState.update { it.copy(navigateToSettings = false) }
    }

    fun onApiKeyUpdated() {
        checkApiKey()
    }
}

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputMessage: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = false,
    val isConnected: Boolean = false,
    val navigateToSettings: Boolean = false
)