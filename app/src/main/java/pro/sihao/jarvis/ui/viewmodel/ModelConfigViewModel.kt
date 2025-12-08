package pro.sihao.jarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pro.sihao.jarvis.data.repository.ModelConfigRepository
import pro.sihao.jarvis.data.database.entity.ModelConfigEntity
import javax.inject.Inject

@HiltViewModel
class ModelConfigViewModel @Inject constructor(
    private val modelConfigRepository: ModelConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelConfigUiState())
    val uiState: StateFlow<ModelConfigUiState> = _uiState.asStateFlow()

    fun loadModel(modelId: Long?, providerId: Long) {
        viewModelScope.launch {
            if (modelId == null) {
                // Creating new model - use defaults
                _uiState.value = ModelConfigUiState(providerId = providerId)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)

                try {
                    val model = modelConfigRepository.getModelConfigById(modelId)
                    if (model != null && model.providerId == providerId) {
                        _uiState.value = ModelConfigUiState(
                            isLoading = false,
                            modelId = model.id,
                            providerId = model.providerId,
                            modelName = model.modelName,
                            displayName = model.displayName,
                            maxTokens = model.maxTokens?.toString() ?: "",
                            contextWindow = model.contextWindow?.toString() ?: "",
                            temperature = model.temperature.toString(),
                            topP = model.topP.toString(),
                            inputCostPer1K = model.inputCostPer1K?.toString() ?: "",
                            outputCostPer1K = model.outputCostPer1K?.toString() ?: "",
                            description = model.description ?: "",
                            isDefault = model.isDefault,
                            isActive = model.isActive
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Model not found"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load model: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateModelName(modelName: String) {
        _uiState.value = _uiState.value.copy(
            modelName = modelName,
            modelNameError = if (modelName.isBlank()) "Model name is required" else null
        )
    }

    fun updateDisplayName(displayName: String) {
        _uiState.value = _uiState.value.copy(
            displayName = displayName,
            displayNameError = if (displayName.isBlank()) "Display name is required" else null
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

    fun updateContextWindow(contextWindow: String) {
        _uiState.value = _uiState.value.copy(
            contextWindow = contextWindow,
            contextWindowError = if (contextWindow.isNotBlank()) {
                contextWindow.toIntOrNull()?.let {
                    if (it <= 0) "Must be greater than 0" else null
                } ?: "Please enter a valid number"
            } else null
        )
    }

    fun updateTemperature(temperature: String) {
        _uiState.value = _uiState.value.copy(
            temperature = temperature,
            temperatureError = if (temperature.isNotBlank()) {
                temperature.toFloatOrNull()?.let {
                    if (it < 0f || it > 2f) "Must be between 0 and 2" else null
                } ?: "Please enter a valid number"
            } else null
        )
    }

    fun updateTopP(topP: String) {
        _uiState.value = _uiState.value.copy(
            topP = topP,
            topPError = if (topP.isNotBlank()) {
                topP.toFloatOrNull()?.let {
                    if (it < 0f || it > 1f) "Must be between 0 and 1" else null
                } ?: "Please enter a valid number"
            } else null
        )
    }

    fun updateInputCostPer1K(inputCostPer1K: String) {
        _uiState.value = _uiState.value.copy(
            inputCostPer1K = inputCostPer1K,
            inputCostPer1KError = if (inputCostPer1K.isNotBlank()) {
                inputCostPer1K.toDoubleOrNull()?.let {
                    if (it < 0) "Must be greater than or equal to 0" else null
                } ?: "Please enter a valid number"
            } else null
        )
    }

    fun updateOutputCostPer1K(outputCostPer1K: String) {
        _uiState.value = _uiState.value.copy(
            outputCostPer1K = outputCostPer1K,
            outputCostPer1KError = if (outputCostPer1K.isNotBlank()) {
                outputCostPer1K.toDoubleOrNull()?.let {
                    if (it < 0) "Must be greater than or equal to 0" else null
                } ?: "Please enter a valid number"
            } else null
        )
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateIsDefault(isDefault: Boolean) {
        _uiState.value = _uiState.value.copy(isDefault = isDefault)
    }

    fun saveModel() {
        if (!validateForm()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val model = ModelConfigEntity(
                    id = _uiState.value.modelId ?: 0,
                    providerId = _uiState.value.providerId,
                    modelName = _uiState.value.modelName,
                    displayName = _uiState.value.displayName,
                    maxTokens = _uiState.value.maxTokens.toIntOrNull(),
                    contextWindow = _uiState.value.contextWindow.toIntOrNull(),
                    temperature = _uiState.value.temperature.toFloatOrNull() ?: 0.7f,
                    topP = _uiState.value.topP.toFloatOrNull() ?: 1.0f,
                    inputCostPer1K = _uiState.value.inputCostPer1K.toDoubleOrNull(),
                    outputCostPer1K = _uiState.value.outputCostPer1K.toDoubleOrNull(),
                    description = _uiState.value.description.takeIf { it.isNotBlank() },
                    isDefault = _uiState.value.isDefault,
                    isActive = true,
                    createdTimestamp = if (_uiState.value.modelId == null) System.currentTimeMillis() else 0,
                    updatedTimestamp = System.currentTimeMillis()
                )

                if (_uiState.value.modelId == null) {
                    modelConfigRepository.insertModelConfig(model)
                } else {
                    modelConfigRepository.updateModelConfig(model)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to save model: ${e.message}"
                )
            }
        }
    }

    private fun validateForm(): Boolean {
        val currentState = _uiState.value
        var isValid = true

        // Validate model name
        val modelNameError = if (currentState.modelName.isBlank()) "Model name is required" else null
        if (modelNameError != null) isValid = false

        // Validate display name
        val displayNameError = if (currentState.displayName.isBlank()) "Display name is required" else null
        if (displayNameError != null) isValid = false

        // Validate max tokens if provided
        val maxTokensError = if (currentState.maxTokens.isNotBlank()) {
            currentState.maxTokens.toIntOrNull()?.let {
                if (it <= 0) "Must be greater than 0" else null
            } ?: "Please enter a valid number"
        } else null
        if (maxTokensError != null) isValid = false

        // Validate context window if provided
        val contextWindowError = if (currentState.contextWindow.isNotBlank()) {
            currentState.contextWindow.toIntOrNull()?.let {
                if (it <= 0) "Must be greater than 0" else null
            } ?: "Please enter a valid number"
        } else null
        if (contextWindowError != null) isValid = false

        // Validate temperature if provided
        val temperatureError = if (currentState.temperature.isNotBlank()) {
            currentState.temperature.toFloatOrNull()?.let {
                if (it < 0f || it > 2f) "Must be between 0 and 2" else null
            } ?: "Please enter a valid number"
        } else null
        if (temperatureError != null) isValid = false

        // Validate topP if provided
        val topPError = if (currentState.topP.isNotBlank()) {
            currentState.topP.toFloatOrNull()?.let {
                if (it < 0f || it > 1f) "Must be between 0 and 1" else null
            } ?: "Please enter a valid number"
        } else null
        if (topPError != null) isValid = false

        // Validate input cost if provided
        val inputCostPer1KError = if (currentState.inputCostPer1K.isNotBlank()) {
            currentState.inputCostPer1K.toDoubleOrNull()?.let {
                if (it < 0) "Must be greater than or equal to 0" else null
            } ?: "Please enter a valid number"
        } else null
        if (inputCostPer1KError != null) isValid = false

        // Validate output cost if provided
        val outputCostPer1KError = if (currentState.outputCostPer1K.isNotBlank()) {
            currentState.outputCostPer1K.toDoubleOrNull()?.let {
                if (it < 0) "Must be greater than or equal to 0" else null
            } ?: "Please enter a valid number"
        } else null
        if (outputCostPer1KError != null) isValid = false

        _uiState.value = currentState.copy(
            modelNameError = modelNameError,
            displayNameError = displayNameError,
            maxTokensError = maxTokensError,
            contextWindowError = contextWindowError,
            temperatureError = temperatureError,
            topPError = topPError,
            inputCostPer1KError = inputCostPer1KError,
            outputCostPer1KError = outputCostPer1KError
        )

        return isValid
    }
}

data class ModelConfigUiState(
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,

    // Form fields
    val modelId: Long? = null,
    val providerId: Long = 0L,
    val modelName: String = "",
    val displayName: String = "",
    val maxTokens: String = "",
    val contextWindow: String = "",
    val temperature: String = "",
    val topP: String = "",
    val inputCostPer1K: String = "",
    val outputCostPer1K: String = "",
    val description: String = "",
    val isDefault: Boolean = false,
    val isActive: Boolean = true,

    // Validation errors
    val modelNameError: String? = null,
    val displayNameError: String? = null,
    val maxTokensError: String? = null,
    val contextWindowError: String? = null,
    val temperatureError: String? = null,
    val topPError: String? = null,
    val inputCostPer1KError: String? = null,
    val outputCostPer1KError: String? = null
) {
    fun isFormValid(): Boolean {
        return modelName.isNotBlank() &&
                displayName.isNotBlank() &&
                modelNameError == null &&
                displayNameError == null &&
                maxTokensError == null &&
                contextWindowError == null &&
                temperatureError == null &&
                topPError == null &&
                inputCostPer1KError == null &&
                outputCostPer1KError == null
    }
}