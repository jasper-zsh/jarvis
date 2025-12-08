package pro.sihao.jarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pro.sihao.jarvis.data.network.APIConfig
import pro.sihao.jarvis.data.network.api.OpenAICompatibleApiService
import pro.sihao.jarvis.data.network.dto.ModelsResponse
import pro.sihao.jarvis.data.storage.SecureStorage
import pro.sihao.jarvis.data.repository.ProviderRepository
import pro.sihao.jarvis.data.repository.ModelConfigRepository
import pro.sihao.jarvis.data.repository.ProviderConfig
import pro.sihao.jarvis.data.repository.ModelConfiguration
import pro.sihao.jarvis.data.database.entity.LLMProviderEntity
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    private val providerRepository: ProviderRepository,
    private val modelConfigRepository: ModelConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadCurrentSettings()
    }

    fun fetchAvailableModels(apiKey: String, baseUrl: String) {
        if (apiKey.isBlank() || baseUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(
                availableModels = emptyList(),
                isLoadingModels = false
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingModels = true)

                val apiService = createApiService(baseUrl)
                val response = apiService.listModels("Bearer $apiKey")

                if (response.isSuccessful) {
                    val models = response.body()?.data?.map { it.id } ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        availableModels = models,
                        isLoadingModels = false,
                        modelsError = null
                    )

                    // If current model is not in the list, keep it but allow user to change
                    if (models.isNotEmpty() && !models.contains(_uiState.value.modelName)) {
                        _uiState.value = _uiState.value.copy(modelName = models.first())
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = _uiState.value.copy(
                        availableModels = emptyList(),
                        isLoadingModels = false,
                        modelsError = "Failed to fetch models: ${errorBody ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    availableModels = emptyList(),
                    isLoadingModels = false,
                    modelsError = "Network error: ${e.message}"
                )
            }
        }
    }

    private fun createApiService(baseUrl: String): OpenAICompatibleApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAICompatibleApiService::class.java)
    }

    private fun loadCurrentSettings() {
        val currentKey = secureStorage.getApiKey() ?: ""
        val currentProvider = secureStorage.getApiProvider()
        val currentBaseUrl = secureStorage.getBaseUrl()
        val currentModel = secureStorage.getModelName()
        val currentTemp = secureStorage.getTemperature()
        val currentMaxTokens = secureStorage.getMaxTokens()

        val apiConfig = getApiConfig(currentProvider)
        val effectiveModel = if (currentModel.isBlank()) apiConfig.defaultModel else currentModel
        val effectiveBaseUrl = if (currentBaseUrl.isBlank()) apiConfig.baseUrl else currentBaseUrl

        _uiState.value = _uiState.value.copy(
            apiKey = currentKey,
            selectedProvider = currentProvider,
            baseUrl = effectiveBaseUrl,
            modelName = effectiveModel,
            temperature = currentTemp,
            maxTokens = currentMaxTokens
        )
    }

    private fun getApiConfig(providerName: String): APIConfig {
        return when (providerName) {
            "DEEPSEEK" -> APIConfig.DEEPSEEK
            "LOCAL_AI" -> APIConfig.LOCAL_AI
            "TOGETHER_AI" -> APIConfig.TOGETHER_AI
            "GROQ" -> APIConfig.GROQ
            else -> APIConfig.OPENAI
        }
    }

    fun onApiKeyChanged(apiKey: String) {
        _uiState.value = _uiState.value.copy(
            apiKey = apiKey,
            errorMessage = null
        )

        // Auto-fetch models when API key changes
        if (apiKey.isNotBlank() && _uiState.value.baseUrl.isNotBlank()) {
            fetchAvailableModels(apiKey, _uiState.value.baseUrl)
        }
    }

    fun onProviderChanged(provider: String) {
        val apiConfig = getApiConfig(provider)
        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            baseUrl = apiConfig.baseUrl,
            modelName = apiConfig.defaultModel,
            errorMessage = null
        )
    }

    fun onBaseUrlChanged(baseUrl: String) {
        _uiState.value = _uiState.value.copy(
            baseUrl = baseUrl,
            errorMessage = null
        )

        // Auto-fetch models when base URL changes
        if (_uiState.value.apiKey.isNotBlank() && baseUrl.isNotBlank()) {
            fetchAvailableModels(_uiState.value.apiKey, baseUrl)
        }
    }

    fun onModelChanged(modelName: String) {
        _uiState.value = _uiState.value.copy(
            modelName = modelName,
            errorMessage = null
        )
    }

    fun onTemperatureChanged(temperature: Float) {
        _uiState.value = _uiState.value.copy(
            temperature = temperature,
            errorMessage = null
        )
    }

    fun onMaxTokensChanged(maxTokens: String) {
        val tokens = maxTokens.toIntOrNull()
        _uiState.value = _uiState.value.copy(
            maxTokens = tokens ?: _uiState.value.maxTokens,
            errorMessage = if (tokens == null) "Invalid number" else null
        )
    }

    fun saveSettings() {
        val apiKey = _uiState.value.apiKey.trim()

        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "API key cannot be empty"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Validate base URL format
                val baseUrl = _uiState.value.baseUrl.trim()
                if (baseUrl.isNotEmpty() && !isValidUrl(baseUrl)) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Invalid base URL format. Should include protocol (http:// or https://)"
                    )
                    return@launch
                }

                // Save all settings
                secureStorage.saveApiKey(apiKey)
                secureStorage.saveApiProvider(_uiState.value.selectedProvider)
                secureStorage.saveBaseUrl(baseUrl)
                secureStorage.saveModelName(_uiState.value.modelName)
                secureStorage.saveTemperature(_uiState.value.temperature)
                secureStorage.saveMaxTokens(_uiState.value.maxTokens)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    saveSuccess = true,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save settings: ${e.message}"
                )
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val urlObj = java.net.URL(url)
            urlObj.protocol in listOf("http", "https")
        } catch (e: Exception) {
            false
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun getAvailableProviders(): List<String> {
        return listOf("OPENAI", "DEEPSEEK", "LOCAL_AI", "TOGETHER_AI", "GROQ")
    }

    // New method to get providers from database with display names
    fun getProvidersWithDisplayNames(): List<String> {
        return listOf("OpenAI", "DeepSeek", "Local AI", "Together AI", "Groq")
    }

    // Method to check if provider management is available
    fun isProviderManagementAvailable(): Boolean {
        return true // Always available now that we have the UI
    }
}

data class SettingsUiState(
    val apiKey: String = "",
    val selectedProvider: String = "OPENAI",
    val baseUrl: String = "https://api.openai.com/",
    val modelName: String = "gpt-3.5-turbo",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val availableModels: List<String> = emptyList(),
    val isLoadingModels: Boolean = false,
    val modelsError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)