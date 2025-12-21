package pro.sihao.jarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pro.sihao.jarvis.data.webrtc.PipeCatConnectionManager
import pro.sihao.jarvis.data.bridge.GlassesPipeCatBridge
import pro.sihao.jarvis.domain.model.PipeCatConfig
import pro.sihao.jarvis.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.domain.model.ChatMode
import javax.inject.Inject

@HiltViewModel
class PipeCatViewModel @Inject constructor(
    private val pipeCatConnectionManager: PipeCatConnectionManager,
    private val glassesPipeCatBridge: GlassesPipeCatBridge,
    private val configurationManager: pro.sihao.jarvis.data.config.ConfigurationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PipeCatUiState())
    val uiState: StateFlow<PipeCatUiState> = _uiState.asStateFlow()

    init {
        observeConnectionState()
        observeGlassesConnection()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            pipeCatConnectionManager.connectionState.collect { state ->
                _uiState.update { 
                    it.copy(
                        connectionState = state,
                        isConnecting = state.isConnecting,
                        isConnected = state.isConnected,
                        errorMessage = state.errorMessage
                    )
                }
            }
        }
    }

    private fun observeGlassesConnection() {
        viewModelScope.launch {
            glassesPipeCatBridge.glassesConnected.collect { isConnected ->
                _uiState.update { 
                    it.copy(
                        glassesConnected = isConnected
                    )
                }
            }
        }
    }

    fun connect(config: PipeCatConfig) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
            try {
                pipeCatConnectionManager.connect(config)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        errorMessage = "Failed to connect: ${e.message}"
                    )
                }
            }
        }
    }

    fun connectWithDefaultConfig() {
        viewModelScope.launch {
            try {
                // Use ConfigurationManager to get current settings
                val config = configurationManager.getCurrentConfig()

                // Validate configuration before connecting
                val validationResult = configurationManager.validateConfiguration()
                if (!validationResult.isValid) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Configuration error: ${validationResult.message}"
                        )
                    }
                    return@launch
                }

                connect(config)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Failed to connect: ${e.message}"
                    )
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                pipeCatConnectionManager.disconnect()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to disconnect: ${e.message}")
                }
            }
        }
    }

    fun toggleMicrophone(enabled: Boolean) {
        viewModelScope.launch {
            try {
                pipeCatConnectionManager.toggleMicrophone(enabled)
                _uiState.update { 
                    it.copy(
                        microphoneEnabled = enabled
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to toggle microphone: ${e.message}")
                }
            }
        }
    }

    fun toggleCamera(enabled: Boolean) {
        viewModelScope.launch {
            try {
                pipeCatConnectionManager.toggleCamera(enabled)
                _uiState.update { 
                    it.copy(
                        cameraEnabled = enabled
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to toggle camera: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun switchToGlassesMode() {
        viewModelScope.launch {
            try {
                glassesPipeCatBridge.switchToGlassesMode()
                _uiState.update { 
                    it.copy(
                        currentMode = ChatMode.GLASSES
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to switch to glasses mode: ${e.message}")
                }
            }
        }
    }

    fun switchToRealtimeMode() {
        _uiState.update { 
            it.copy(
                currentMode = ChatMode.REALTIME
            )
        }
    }

    fun switchToTextMode() {
        _uiState.update { 
            it.copy(
                currentMode = ChatMode.TEXT
            )
        }
    }
}

data class PipeCatUiState(
    val connectionState: PipeCatConnectionState = PipeCatConnectionState(),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val microphoneEnabled: Boolean = true,
    val cameraEnabled: Boolean = false,
    val glassesConnected: Boolean = false,
    val errorMessage: String? = null,
    val currentMode: ChatMode = ChatMode.TEXT
)