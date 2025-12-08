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
import pro.sihao.jarvis.data.service.ProviderHealthResult
import pro.sihao.jarvis.data.service.ProviderHealthService
import javax.inject.Inject

@HiltViewModel
class ProviderListViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val providerHealthService: ProviderHealthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderListUiState())
    val uiState: StateFlow<ProviderListUiState> = _uiState.asStateFlow()

    fun loadProviders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                providerRepository.getAllProviders().collect { providers ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        providers = providers,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun toggleProviderActive(providerId: Long, isActive: Boolean) {
        viewModelScope.launch {
            try {
                if (isActive) {
                    providerRepository.setActiveProvider(providerId)
                }
                // Provider will be updated in the flow
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update provider: ${e.message}"
                )
            }
        }
    }

    fun deleteProvider(provider: LLMProviderEntity) {
        viewModelScope.launch {
            try {
                providerRepository.deleteProvider(provider)
                // Provider will be removed from the flow automatically
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete provider: ${e.message}"
                )
            }
        }
    }

    suspend fun checkProviderHealth(providerId: Long): ProviderHealthResult {
        return try {
            providerHealthService.checkProviderHealth(providerId)
        } catch (e: Exception) {
            ProviderHealthResult(
                providerId = providerId,
                providerName = "Unknown",
                isHealthy = false,
                responseTime = 0L,
                error = e.message ?: "Health check failed"
            )
        }
    }

    suspend fun checkAllProvidersHealth(): List<ProviderHealthResult> {
        return try {
            providerHealthService.checkAllProvidersHealth()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class ProviderListUiState(
    val isLoading: Boolean = false,
    val providers: List<LLMProviderEntity> = emptyList(),
    val error: String? = null
)