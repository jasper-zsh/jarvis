package pro.sihao.jarvis.features.realtime.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pro.sihao.jarvis.platform.network.webrtc.PipeCatConnectionManager
import pro.sihao.jarvis.core.domain.model.PipeCatConfig
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.core.domain.model.ChatMode
import pro.sihao.jarvis.core.presentation.navigation.NavigationManager
import pro.sihao.jarvis.core.presentation.navigation.TabNavigation
import javax.inject.Inject

@HiltViewModel
class PipeCatViewModel @Inject constructor(
    private val pipeCatConnectionManager: PipeCatConnectionManager,
    private val configurationManager: pro.sihao.jarvis.features.realtime.data.config.ConfigurationManager,
    private val navigationManager: NavigationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PipeCatUiState())
    val uiState: StateFlow<PipeCatUiState> = _uiState.asStateFlow()

    init {
        observeConnectionState()
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
        _uiState.update {
            it.copy(
                currentMode = ChatMode.GLASSES
            )
        }
        // Navigate to Glasses tab when switching to glasses mode
        navigationManager.navigateToTab(TabNavigation.Glasses)
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
    val errorMessage: String? = null,
    val currentMode: ChatMode = ChatMode.TEXT
)