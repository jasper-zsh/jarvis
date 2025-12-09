package pro.sihao.jarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pro.sihao.jarvis.data.repository.ModelConfigRepository
import pro.sihao.jarvis.data.repository.ProviderRepository
import pro.sihao.jarvis.data.repository.ModelConfiguration
import pro.sihao.jarvis.data.network.api.OpenAICompatibleApiService
import pro.sihao.jarvis.data.network.dto.ModelsResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import javax.inject.Inject
import java.util.concurrent.TimeUnit

// Cache constants
private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
private const val CACHE_PROVIDER_ID = "model_discovery_cache"

@HiltViewModel
class ModelSelectorViewModel @Inject constructor(
    private val modelConfigRepository: ModelConfigRepository,
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSelectorUiState())
    val uiState: StateFlow<ModelSelectorUiState> = _uiState.asStateFlow()

    // Simple cache for discovered models (providerId -> list of models with timestamp)
    private val modelCache = mutableMapOf<Long, Pair<List<String>, Long>>()

    fun loadModelsForProvider(providerId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                providerId = providerId
            )

            try {
                // Get provider info to check model discovery support
                val provider = providerRepository.getProviderById(providerId)

                // Load existing models from database
                modelConfigRepository.getActiveModelsForProvider(providerId).collect { models ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        models = models,
                        error = null,
                        supportsModelDiscovery = provider?.supportsModelDiscovery ?: false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load models: ${e.message}"
                )
            }
        }
    }

    fun refreshModels() {
        val providerId = _uiState.value.providerId
        if (providerId != null) {
            loadModelsForProvider(providerId)
        }
    }

    fun setDefaultModel(modelConfigId: Long) {
        viewModelScope.launch {
            try {
                // First get the current provider ID for this model
                val currentModels = _uiState.value.models
                val model = currentModels.find { it.id == modelConfigId }
                if (model != null) {
                    modelConfigRepository.setActiveAndDefaultModel(model.providerId, modelConfigId)
                    // The flow will update automatically
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to set default model: ${e.message}"
                )
            }
        }
    }

    fun toggleModelActive(modelConfigId: Long, isActive: Boolean) {
        viewModelScope.launch {
            try {
                modelConfigRepository.setModelConfigActiveStatus(modelConfigId, isActive)
                // The flow will update automatically
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update model: ${e.message}"
                )
            }
        }
    }

    fun discoverModels() {
        val providerId = _uiState.value.providerId ?: return
        if (!_uiState.value.supportsModelDiscovery) {
            _uiState.value = _uiState.value.copy(
                error = "Model discovery not supported for this provider"
            )
            return
        }

        viewModelScope.launch {
            try {
                // Check cache first
                val currentTime = System.currentTimeMillis()
                val cachedData = modelCache[providerId]

                if (cachedData != null && (currentTime - cachedData.second) < CACHE_DURATION_MS) {
                    // Use cached data
                    _uiState.value = _uiState.value.copy(
                        isDiscoveringModels = false,
                        discoveredModels = cachedData.first,
                        discoveryError = null
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(isDiscoveringModels = true, discoveryError = null)

                // Get provider info
                val provider = providerRepository.getProviderById(providerId)
                if (provider == null) {
                    _uiState.value = _uiState.value.copy(
                        isDiscoveringModels = false,
                        discoveryError = "Provider not found"
                    )
                    return@launch
                }

                // Get API key
                val apiKey = providerRepository.getApiKeyForProvider(providerId)
                if (apiKey.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isDiscoveringModels = false,
                        discoveryError = "No API key configured for this provider"
                    )
                    return@launch
                }

                // Create API service with timeout
                val retrofit = Retrofit.Builder()
                    .baseUrl(provider.baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(
                        OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .build()
                    )
                    .build()

                val apiService = retrofit.create(OpenAICompatibleApiService::class.java)
                val response = apiService.listModels("Bearer $apiKey")

                if (response.isSuccessful) {
                    val responseModel = response.body()
                    val discoveredModels = responseModel?.data?.map { it.id } ?: emptyList()

                    // Update cache
                    modelCache[providerId] = Pair(discoveredModels, currentTime)

                    _uiState.value = _uiState.value.copy(
                        isDiscoveringModels = false,
                        discoveredModels = discoveredModels,
                        discoveryError = null
                    )
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = _uiState.value.copy(
                        isDiscoveringModels = false,
                        discoveredModels = emptyList(),
                        discoveryError = "Failed to discover models: ${errorBody ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDiscoveringModels = false,
                    discoveredModels = emptyList(),
                    discoveryError = "Network error: ${e.message}"
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

    fun addDiscoveredModel(modelName: String) {
        val providerId = _uiState.value.providerId ?: return

        viewModelScope.launch {
            try {
                // Create a new model config with default settings
                val newModel = pro.sihao.jarvis.data.database.entity.ModelConfigEntity(
                    id = 0, // Room will auto-generate
                    providerId = providerId,
                    modelName = modelName,
                    displayName = modelName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }.replace("-", " "),
                    maxTokens = null,
                    contextWindow = null,
                    temperature = 0.7f,
                    topP = 1.0f,
                    inputCostPer1K = null,
                    outputCostPer1K = null,
                    description = "Auto-discovered model",
                    isDefault = false,
                    isActive = true,
                    createdTimestamp = System.currentTimeMillis(),
                    updatedTimestamp = System.currentTimeMillis()
                )

                modelConfigRepository.insertModelConfig(newModel)

                // Remove from discovered models list
                val currentDiscovered = _uiState.value.discoveredModels.toMutableList()
                currentDiscovered.remove(modelName)
                _uiState.value = _uiState.value.copy(discoveredModels = currentDiscovered)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    discoveryError = "Failed to add model: ${e.message}"
                )
            }
        }
    }
}

data class ModelSelectorUiState(
    val isLoading: Boolean = false,
    val models: List<ModelConfiguration> = emptyList(),
    val error: String? = null,
    val providerId: Long? = null,
    val supportsModelDiscovery: Boolean = false,
    val isDiscoveringModels: Boolean = false,
    val discoveredModels: List<String> = emptyList(),
    val discoveryError: String? = null
)