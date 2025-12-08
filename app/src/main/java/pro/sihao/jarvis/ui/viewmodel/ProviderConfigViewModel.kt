package pro.sihao.jarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pro.sihao.jarvis.data.repository.ProviderRepository
import pro.sihao.jarvis.data.database.entity.LLMProviderEntity
import javax.inject.Inject

@HiltViewModel
class ProviderConfigViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val secureStorage: pro.sihao.jarvis.data.storage.SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderConfigUiState())
    val uiState: StateFlow<ProviderConfigUiState> = _uiState.asStateFlow()

    fun loadProvider(providerId: Long?) {
        viewModelScope.launch {
            if (providerId == null) {
                // Creating new provider - use defaults
                _uiState.value = ProviderConfigUiState()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)

                try {
                    val provider = providerRepository.getProviderById(providerId)
                    if (provider != null) {
                        // Try to get API key from secure storage
                        val apiKey = secureStorage.getApiKeyForProvider(provider.id)

                        _uiState.value = ProviderConfigUiState(
                            isLoading = false,
                            providerId = provider.id,
                            name = provider.name,
                            displayName = provider.displayName,
                            baseUrl = provider.baseUrl,
                            authenticationType = provider.authenticationType,
                            defaultModel = provider.defaultModel ?: "",
                            maxTokens = provider.maxTokens?.toString() ?: "",
                            description = provider.description ?: "",
                            supportsModelDiscovery = provider.supportsModelDiscovery,
                            apiKey = apiKey ?: ""
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Provider not found"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load provider: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = if (name.isBlank()) "Provider name is required" else null
        )
    }

    fun updateDisplayName(displayName: String) {
        _uiState.value = _uiState.value.copy(
            displayName = displayName,
            displayNameError = if (displayName.isBlank()) "Display name is required" else null
        )
    }

    fun updateBaseUrl(baseUrl: String) {
        _uiState.value = _uiState.value.copy(
            baseUrl = baseUrl,
            baseUrlError = if (baseUrl.isBlank()) "Base URL is required" else {
                if (!isValidUrl(baseUrl)) "Please enter a valid URL" else null
            }
        )
    }

    fun updateAuthenticationType(authType: String) {
        _uiState.value = _uiState.value.copy(authenticationType = authType)
    }

    fun updateDefaultModel(defaultModel: String) {
        _uiState.value = _uiState.value.copy(defaultModel = defaultModel)
    }

    fun updateApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(
            apiKey = apiKey,
            apiKeyError = if (apiKey.isBlank()) "API key is required for authentication" else null
        )
    }

    fun updateMaxTokens(maxTokens: String) {
        _uiState.value = _uiState.value.copy(
            maxTokens = maxTokens,
            maxTokensError = if (maxTokens.isNotBlank()) {
                maxTokens.toIntOrNull()?.let {
                    if (it <= 0) "Must be greater than 0" else null
                } ?: "Please enter a valid number"
            } else null
        )
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateSupportsModelDiscovery(supports: Boolean) {
        _uiState.value = _uiState.value.copy(supportsModelDiscovery = supports)
    }

    fun toggleAuthTypeMenu() {
        _uiState.value = _uiState.value.copy(authTypeMenuExpanded = !_uiState.value.authTypeMenuExpanded)
    }

    fun saveProvider() {
        if (!validateForm()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val provider = LLMProviderEntity(
                    id = _uiState.value.providerId ?: 0,
                    name = _uiState.value.name,
                    displayName = _uiState.value.displayName,
                    baseUrl = _uiState.value.baseUrl,
                    authenticationType = _uiState.value.authenticationType,
                    defaultModel = _uiState.value.defaultModel.takeIf { it.isNotBlank() },
                    maxTokens = _uiState.value.maxTokens.toIntOrNull(),
                    description = _uiState.value.description.takeIf { it.isNotBlank() },
                    supportsModelDiscovery = _uiState.value.supportsModelDiscovery,
                    isActive = true,
                    createdTimestamp = if (_uiState.value.providerId == null) System.currentTimeMillis() else 0,
                    updatedTimestamp = System.currentTimeMillis()
                )

                val isNewProvider = _uiState.value.providerId == null

                if (isNewProvider) {
                    providerRepository.insertProvider(provider)
                } else {
                    providerRepository.updateProvider(provider)
                }

                // Save API key to secure storage (for both new and existing providers)
                if (_uiState.value.apiKey.isNotBlank()) {
                    secureStorage.saveApiKeyForProvider(provider.id, _uiState.value.apiKey)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to save provider: ${e.message}"
                )
            }
        }
    }

    private fun validateForm(): Boolean {
        val currentState = _uiState.value
        var isValid = true

        // Validate name
        val nameError = if (currentState.name.isBlank()) "Provider name is required" else null
        if (nameError != null) isValid = false

        // Validate display name
        val displayNameError = if (currentState.displayName.isBlank()) "Display name is required" else null
        if (displayNameError != null) isValid = false

        // Validate base URL
        val baseUrlError = if (currentState.baseUrl.isBlank()) {
            "Base URL is required"
        } else if (!isValidUrl(currentState.baseUrl)) {
            "Please enter a valid URL"
        } else null
        if (baseUrlError != null) isValid = false

        // Validate API key
        val apiKeyError = if (currentState.apiKey.isBlank()) {
            "API key is required for authentication"
        } else null
        if (apiKeyError != null) isValid = false

        // Validate max tokens if provided
        val maxTokensError = if (currentState.maxTokens.isNotBlank()) {
            currentState.maxTokens.toIntOrNull()?.let {
                if (it <= 0) "Must be greater than 0" else null
            } ?: "Please enter a valid number"
        } else null
        if (maxTokensError != null) isValid = false

        _uiState.value = currentState.copy(
            nameError = nameError,
            displayNameError = displayNameError,
            baseUrlError = baseUrlError,
            apiKeyError = apiKeyError,
            maxTokensError = maxTokensError
        )

        return isValid
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val trimmedUrl = url.trim()
            (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) &&
                    trimmedUrl.contains('.')
        } catch (e: Exception) {
            false
        }
    }
}

data class ProviderConfigUiState(
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,

    // Form fields
    val providerId: Long? = null,
    val name: String = "",
    val displayName: String = "",
    val baseUrl: String = "",
    val authenticationType: String = "API_KEY",
    val apiKey: String = "",
    val defaultModel: String = "",
    val maxTokens: String = "",
    val description: String = "",
    val supportsModelDiscovery: Boolean = true,

    // UI state
    val authTypeMenuExpanded: Boolean = false,

    // Validation errors
    val nameError: String? = null,
    val displayNameError: String? = null,
    val baseUrlError: String? = null,
    val apiKeyError: String? = null,
    val maxTokensError: String? = null
) {
    fun isFormValid(): Boolean {
        return name.isNotBlank() &&
                displayName.isNotBlank() &&
                baseUrl.isNotBlank() &&
                apiKey.isNotBlank() &&
                nameError == null &&
                displayNameError == null &&
                baseUrlError == null &&
                apiKeyError == null &&
                maxTokensError == null
    }
}