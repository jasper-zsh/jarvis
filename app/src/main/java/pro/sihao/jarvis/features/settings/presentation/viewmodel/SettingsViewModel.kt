package pro.sihao.jarvis.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pro.sihao.jarvis.features.realtime.data.config.ConfigurationManager
import javax.inject.Inject

/**
 * ViewModel for managing settings screen state and operations
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configurationManager: ConfigurationManager
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Configuration flows
    val baseUrl: StateFlow<String> = configurationManager.baseUrl
    val apiKey: StateFlow<String> = configurationManager.apiKey
    val botId: StateFlow<String> = configurationManager.botId
    val enableMic: StateFlow<Boolean> = configurationManager.enableMic
    val enableCam: StateFlow<Boolean> = configurationManager.enableCam

    init {
        // Initialize UI state with current configuration
        updateUiStateFromConfiguration()

        // Observe configuration changes
        viewModelScope.launch {
            combine(
                baseUrl,
                apiKey,
                botId,
                enableMic,
                enableCam
            ) { url, key, id, mic, cam ->
                _uiState.update { current ->
                    current.copy(
                        baseUrl = url,
                        apiKey = key,
                        botId = id,
                        enableMic = mic,
                        enableCam = cam,
                        hasChanges = false // Reset changes flag when synced
                    )
                }
            }.collect()
        }
    }

    /**
     * Update base URL
     */
    fun updateBaseUrl(url: String) {
        val trimmedUrl = url.trim()
        _uiState.update {
            it.copy(
                baseUrl = trimmedUrl,
                baseUrlError = if (trimmedUrl.isBlank()) "Base URL is required" else null,
                hasChanges = true
            )
        }
    }

    /**
     * Update API key
     */
    fun updateApiKey(key: String) {
        val trimmedKey = key.trim()
        _uiState.update {
            it.copy(
                apiKey = trimmedKey,
                hasChanges = true
            )
        }
    }

    /**
     * Update bot ID
     */
    fun updateBotId(id: String) {
        val trimmedId = id.trim()
        _uiState.update {
            it.copy(
                botId = trimmedId,
                botIdError = if (trimmedId.isBlank()) "Bot ID is required" else null,
                hasChanges = true
            )
        }
    }

    /**
     * Update microphone enabled state
     */
    fun updateMicrophoneEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                enableMic = enabled,
                hasChanges = true
            )
        }
    }

    /**
     * Update camera enabled state
     */
    fun updateCameraEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                enableCam = enabled,
                hasChanges = true
            )
        }
    }

    /**
     * Save all settings
     */
    fun saveSettings() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }

                val state = _uiState.value

                // Validate configuration before saving
                if (!validateConfiguration()) {
                    _uiState.update {
                        it.copy(isSaving = false)
                    }
                    return@launch
                }

                // Save all settings
                configurationManager.setBaseUrl(state.baseUrl)
                configurationManager.setApiKey(state.apiKey)
                configurationManager.setBotId(state.botId)
                configurationManager.setEnableMic(state.enableMic)
                configurationManager.setEnableCam(state.enableCam)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasChanges = false,
                        saveSuccess = true,
                        errorMessage = null
                    )
                }

                // Clear success message after delay
                kotlinx.coroutines.delay(3000)
                _uiState.update { it.copy(saveSuccess = false) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save settings: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Reset to default settings
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }

                configurationManager.resetToDefaults()

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        errorMessage = null
                    )
                }

                // Clear success message after delay
                kotlinx.coroutines.delay(3000)
                _uiState.update { it.copy(saveSuccess = false) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to reset settings: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clear error messages
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    /**
     * Validate current configuration
     */
    private fun validateConfiguration(): Boolean {
        val state = _uiState.value
        var isValid = true

        // Validate base URL
        if (state.baseUrl.isBlank()) {
            _uiState.update { it.copy(baseUrlError = "Base URL is required") }
            isValid = false
        } else if (!state.baseUrl.startsWith("http")) {
            _uiState.update { it.copy(baseUrlError = "Invalid URL format") }
            isValid = false
        } else {
            _uiState.update { it.copy(baseUrlError = null) }
        }

        // Validate bot ID
        if (state.botId.isBlank()) {
            _uiState.update { it.copy(botIdError = "Bot ID is required") }
            isValid = false
        } else {
            _uiState.update { it.copy(botIdError = null) }
        }

        return isValid
    }

    /**
     * Update UI state from current configuration
     */
    private fun updateUiStateFromConfiguration() {
        _uiState.update {
            SettingsUiState(
                baseUrl = configurationManager.baseUrl.value,
                apiKey = configurationManager.apiKey.value,
                botId = configurationManager.botId.value,
                enableMic = configurationManager.enableMic.value,
                enableCam = configurationManager.enableCam.value,
                hasChanges = false
            )
        }
    }
}

/**
 * UI State for Settings Screen
 */
data class SettingsUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val botId: String = "",
    val enableMic: Boolean = true,
    val enableCam: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val hasChanges: Boolean = false,
    val errorMessage: String? = null,
    val baseUrlError: String? = null,
    val botIdError: String? = null
)