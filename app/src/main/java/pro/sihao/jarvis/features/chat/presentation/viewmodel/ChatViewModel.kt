package pro.sihao.jarvis.features.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pro.sihao.jarvis.platform.network.NetworkMonitor
import pro.sihao.jarvis.core.domain.model.Message
import pro.sihao.jarvis.core.domain.model.ChatMode
import pro.sihao.jarvis.core.domain.repository.MessageRepository
import pro.sihao.jarvis.core.domain.service.PipeCatService
import pro.sihao.jarvis.core.domain.model.PipeCatEvent
import pro.sihao.jarvis.core.domain.model.PipeCatConfig
import pro.sihao.jarvis.platform.network.webrtc.PipeCatConnectionManager
import pro.sihao.jarvis.features.realtime.data.bridge.GlassesPipeCatBridge
import java.util.Date
import javax.inject.Inject

/**
 * Voice-only real-time ChatViewModel for PipeCat architecture
 * Handles voice real-time chat through PipeCat service
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val pipeCatService: PipeCatService,
    private val networkMonitor: NetworkMonitor,
    private val pipeCatConnectionManager: PipeCatConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeNetworkState()
        observePipeCatState()
        startRealtimeMode() // Start in real-time mode by default
    }

    
    private fun observeNetworkState() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                _uiState.update {
                    it.copy(
                        isNetworkAvailable = isConnected,
                        errorMessage = if (!isConnected) {
                            "No network connection. Please check your internet connection."
                        } else null
                    )
                }
            }
        }
    }

    
    private fun observePipeCatState() {
        viewModelScope.launch {
            pipeCatService.connectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        pipeCatConnectionState = state,
                        isConnected = state.isConnected
                    )
                }
            }
        }
    }

    // Voice-only real-time chat implementation - no text processing needed

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Voice real-time chat methods
    fun startRealtimeMode() {
        _uiState.update {
            it.copy(
                chatMode = ChatMode.REALTIME,
                errorMessage = null
            )
        }
    }

    fun switchToGlassesMode() {
        _uiState.update { it.copy(chatMode = ChatMode.GLASSES) }
    }

    fun connectToRealtimeChat(config: PipeCatConfig) {
        viewModelScope.launch {
            try {
                pipeCatService.startRealtimeSession(config).collect { event ->
                    handleRealtimeEvent(event)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to connect to real-time chat: ${e.message}") }
            }
        }
    }

    fun disconnectFromRealtimeChat() {
        viewModelScope.launch {
            try {
                pipeCatService.stopRealtimeSession()
                _uiState.update { it.copy(isConnected = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to disconnect: ${e.message}") }
            }
        }
    }

    fun toggleMicrophone(enabled: Boolean) {
        pipeCatService.toggleMicrophone(enabled)
    }

    fun toggleCamera(enabled: Boolean) {
        pipeCatService.toggleCamera(enabled)
    }

    private fun handleRealtimeEvent(event: PipeCatEvent) {
        when (event) {
            is PipeCatEvent.BotReady -> {
                _uiState.update {
                    it.copy(
                        isConnected = true,
                        errorMessage = null
                    )
                }
            }
            is PipeCatEvent.Error -> {
                _uiState.update {
                    it.copy(
                        errorMessage = "Real-time chat error: ${event.message}"
                    )
                }
            }
            is PipeCatEvent.Disconnected -> {
                _uiState.update { it.copy(isConnected = false) }
            }
            else -> {
                // Handle other real-time events as needed
            }
        }
    }
}

/**
 * UI State for Voice Real-time Chat
 */
data class ChatUiState(
    val isLoading: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val isConnected: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val chatMode: ChatMode = ChatMode.REALTIME,
    val pipeCatConnectionState: pro.sihao.jarvis.core.domain.model.PipeCatConnectionState = pro.sihao.jarvis.core.domain.model.PipeCatConnectionState()
)