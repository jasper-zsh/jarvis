package pro.sihao.jarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pro.sihao.jarvis.domain.service.LLMService
import pro.sihao.jarvis.domain.service.LLMStreamEvent
import pro.sihao.jarvis.data.repository.ModelConfiguration
import pro.sihao.jarvis.data.repository.ProviderRepository
import javax.inject.Inject

@HiltViewModel
class ModelTestViewModel @Inject constructor(
    private val llmService: LLMService,
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelTestUiState())
    val uiState: StateFlow<ModelTestUiState> = _uiState.asStateFlow()

    fun setTestModel(model: ModelConfiguration) {
        _uiState.value = _uiState.value.copy(
            testModel = model,
            response = null,
            error = null,
            responseTime = null,
            tokensUsed = null
        )
    }

    fun updateTestPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(testPrompt = prompt)
    }

    fun runTest() {
        val currentState = _uiState.value
        if (currentState.testPrompt.isBlank() || currentState.testModel == null) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            try {
                val startTime = System.currentTimeMillis()

                // Get API key for the provider
                val apiKey = providerRepository.getApiKeyForProvider(currentState.testModel!!.providerId)

                if (apiKey.isNullOrEmpty()) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = "No API key configured for this provider"
                    )
                    return@launch
                }

                llmService.sendMessage(
                    message = currentState.testPrompt,
                    conversationHistory = emptyList(),
                    apiKey = apiKey
                ).collect { event ->
                    when (event) {
                        is LLMStreamEvent.Partial -> {
                            _uiState.value = _uiState.value.copy(
                                response = event.content,
                                error = null
                            )
                        }

                        is LLMStreamEvent.Complete -> {
                            val endTime = System.currentTimeMillis()
                            val responseTime = (endTime - startTime).toInt()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                response = event.content,
                                error = null,
                                responseTime = responseTime,
                                tokensUsed = estimateTokens(currentState.testPrompt, event.content)
                            )
                        }

                        is LLMStreamEvent.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = event.throwable.message ?: "Unknown error occurred",
                                response = null
                            )
                        }

                        is LLMStreamEvent.Canceled -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Test canceled",
                                response = null
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = "Test failed: ${e.message}",
                    response = null
                )
            }
        }
    }

    private fun estimateTokens(input: String, output: String?): Int {
        // Rough estimation: ~4 characters per token for English text
        val inputTokens = (input.length / 4.0).toInt()
        val outputTokens = output?.let { (it.length / 4.0).toInt() } ?: 0
        return inputTokens + outputTokens
    }
}

data class ModelTestUiState(
    val testModel: ModelConfiguration? = null,
    val testPrompt: String = "Hello! Can you introduce yourself briefly?",
    val isLoading: Boolean = false,
    val response: String? = null,
    val error: String? = null,
    val responseTime: Int? = null,
    val tokensUsed: Int? = null
)
