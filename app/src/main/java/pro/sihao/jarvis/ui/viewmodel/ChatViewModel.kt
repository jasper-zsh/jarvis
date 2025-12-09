package pro.sihao.jarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pro.sihao.jarvis.data.network.NetworkMonitor
import pro.sihao.jarvis.data.repository.ProviderRepository
import pro.sihao.jarvis.data.repository.ModelConfigRepository
import pro.sihao.jarvis.data.repository.ModelConfiguration
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.domain.repository.MessageRepository
import pro.sihao.jarvis.domain.service.LLMService
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val llmService: LLMService,
    private val networkMonitor: NetworkMonitor,
    private val providerRepository: ProviderRepository,
    private val modelConfigRepository: ModelConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
        checkApiKey()
        observeNetworkState()
        loadAvailableModels()
        loadCurrentModel()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            messageRepository.getAllMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun checkApiKey() {
        viewModelScope.launch {
            val activeProviderId = providerRepository.getActiveProviderId()
            val hasApiKey = if (activeProviderId != -1L) {
                providerRepository.hasApiKeyForProvider(activeProviderId)
            } else {
                false
            }
            _uiState.update { it.copy(hasApiKey = hasApiKey) }
        }
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

        if (!uiState.value.hasApiKey) {
            _uiState.update {
                it.copy(
                    errorMessage = "Please configure a provider with API key first"
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
                llmService.sendMessage(currentMessage, messages, null).collect { result ->
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

    // Model selection methods

    private fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                val activeProviders = providerRepository.getActiveProviders().first()
                val allModels = mutableListOf<ModelConfiguration>()

                activeProviders.forEach { provider ->
                    val models = modelConfigRepository.getActiveModelsForProvider(provider.id).first()
                    allModels.addAll(models)
                }

                _uiState.update { it.copy(availableModels = allModels) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(modelSwitchingError = "Failed to load models: ${e.message}")
                }
            }
        }
    }

    fun refreshAvailableModels() {
        loadAvailableModels()
    }

    private fun loadCurrentModel() {
        viewModelScope.launch {
            try {
                val activeModelConfig = modelConfigRepository.getActiveModelConfig()
                if (activeModelConfig != null) {
                    _uiState.update { it.copy(currentModel = activeModelConfig) }
                } else {
                    // No model configured - try to auto-select a default model
                    tryAutoSelectDefaultModel()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(modelSwitchingError = "Failed to load current model: ${e.message}")
                }
            }
        }
    }

    private fun tryAutoSelectDefaultModel() {
        viewModelScope.launch {
            try {
                // Get the active provider first
                val activeProviderId = providerRepository.getActiveProviderId()
                if (activeProviderId != -1L) {
                    // Try to find a default model for this provider
                    val defaultModel = modelConfigRepository.getDefaultModelForProvider(activeProviderId)
                    if (defaultModel != null) {
                        // Auto-select this model
                        modelConfigRepository.setActiveModelConfig(defaultModel.id)
                        _uiState.update { it.copy(currentModel = defaultModel) }
                        // Re-check API key status after setting the model
                        checkApiKey()
                        return@launch
                    }
                }

                // If no default model found, try to get any active model
                val activeModel = modelConfigRepository.getFirstActiveModel()
                if (activeModel != null) {
                    modelConfigRepository.setActiveModelConfig(activeModel.id)
                    _uiState.update { it.copy(currentModel = activeModel) }
                    // Re-check API key status after setting the model
                    checkApiKey()
                } else {
                    // No models available at all
                    _uiState.update { it.copy(currentModel = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(currentModel = null) }
            }
        }
    }

    fun refreshCurrentModel() {
        loadCurrentModel()
    }

    fun toggleModelSwitcher() {
        _uiState.update { it.copy(showModelSwitcher = !it.showModelSwitcher) }
    }

    fun selectModel(model: ModelConfiguration) {
        viewModelScope.launch {
            try {
                // Set the active model configuration
                modelConfigRepository.setActiveModelConfig(model.id)

                _uiState.update {
                    it.copy(
                        currentModel = model,
                        showModelSwitcher = false,
                        modelSwitchingError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(modelSwitchingError = "Failed to select model: ${e.message}")
                }
            }
        }
    }

    fun clearModelSwitchingError() {
        _uiState.update { it.copy(modelSwitchingError = null) }
    }

    fun refreshModels() {
        loadAvailableModels()
        loadCurrentModel()
    }

    fun getCurrentModelInfo(): String {
        val model = _uiState.value.currentModel
        return if (model != null) {
            "${model.displayName} (${model.modelName})"
        } else {
            "No model selected"
        }
    }
}

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputMessage: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = false,
    val isConnected: Boolean = false,
    val navigateToSettings: Boolean = false,
    // Model selection information
    val availableModels: List<ModelConfiguration> = emptyList(),
    val currentModel: ModelConfiguration? = null,
    val showModelSwitcher: Boolean = false,
    val modelSwitchingError: String? = null
)