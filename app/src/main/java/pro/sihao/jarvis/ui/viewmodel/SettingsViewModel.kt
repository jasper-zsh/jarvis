package pro.sihao.jarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import pro.sihao.jarvis.data.repository.ProviderRepository
import pro.sihao.jarvis.data.repository.ModelConfigRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val modelConfigRepository: ModelConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadConfigurationStatus()
    }

    private fun loadConfigurationStatus() {
        viewModelScope.launch {
            try {
                val hasActiveProvider = providerRepository.hasActiveProvider()
                val activeProvider = providerRepository.getActiveProvider()
                val hasActiveModel = modelConfigRepository.hasActiveModelConfig()
                val activeModel = modelConfigRepository.getActiveModelConfig()
                val hasApiKey = activeProvider?.let {
                    providerRepository.hasApiKeyForProvider(it.id)
                } ?: false

                _uiState.value = _uiState.value.copy(
                    hasActiveProvider = hasActiveProvider,
                    activeProviderName = activeProvider?.displayName ?: "None",
                    hasActiveModel = hasActiveModel,
                    activeModelName = activeModel?.displayName ?: "None",
                    hasApiKey = hasApiKey,
                    isConfigured = hasActiveProvider && hasActiveModel && hasApiKey
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load configuration: ${e.message}"
                )
            }
        }
    }

    fun refreshConfigurationStatus() {
        loadConfigurationStatus()
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Navigation methods
    fun navigateToProviderManagement() {
        _uiState.value = _uiState.value.copy(navigateToProviderManagement = true)
    }

    fun onProviderManagementNavigated() {
        _uiState.value = _uiState.value.copy(navigateToProviderManagement = false)
    }

    fun navigateToModelSelection() {
        _uiState.value = _uiState.value.copy(navigateToModelSelection = true)
    }

    fun onModelSelectionNavigated() {
        _uiState.value = _uiState.value.copy(navigateToModelSelection = false)
    }
}

data class SettingsUiState(
    val hasActiveProvider: Boolean = false,
    val activeProviderName: String = "None",
    val hasActiveModel: Boolean = false,
    val activeModelName: String = "None",
    val hasApiKey: Boolean = false,
    val isConfigured: Boolean = false,
    val errorMessage: String? = null,
    // Navigation states
    val navigateToProviderManagement: Boolean = false,
    val navigateToModelSelection: Boolean = false
)