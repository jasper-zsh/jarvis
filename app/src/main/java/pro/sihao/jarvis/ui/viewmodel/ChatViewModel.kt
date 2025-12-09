package pro.sihao.jarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import pro.sihao.jarvis.data.network.NetworkMonitor
import pro.sihao.jarvis.data.repository.ProviderRepository
import pro.sihao.jarvis.data.repository.ModelConfigRepository
import pro.sihao.jarvis.data.repository.ModelConfiguration
import pro.sihao.jarvis.domain.model.ContentType
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.domain.repository.MessageRepository
import pro.sihao.jarvis.domain.service.LLMService
import pro.sihao.jarvis.media.VoiceRecorder
import pro.sihao.jarvis.media.VoicePlayer
import pro.sihao.jarvis.media.PhotoCaptureManager
import pro.sihao.jarvis.permission.PermissionManager
import pro.sihao.jarvis.data.storage.MediaStorageManager
import java.io.File
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val llmService: LLMService,
    private val networkMonitor: NetworkMonitor,
    private val providerRepository: ProviderRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val permissionManager: PermissionManager,
    private val mediaStorageManager: MediaStorageManager,
    private val voiceRecorder: VoiceRecorder,
    private val voicePlayer: VoicePlayer,
    private val photoCaptureManager: PhotoCaptureManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
        checkApiKey()
        observeNetworkState()
        loadAvailableModels()
        loadCurrentModel()
        observeMediaStates()
        checkPermissions()
    }

    fun refreshPermissions() {
        checkPermissions()
    }

    private fun observeMediaStates() {
        // Observe voice recorder state
        viewModelScope.launch {
            voiceRecorder.recordingState.collect { state ->
                _uiState.update { it.copy(isRecording = state == VoiceRecorder.RecordingState.RECORDING) }
            }
        }

        viewModelScope.launch {
            voiceRecorder.recordingDuration.collect { duration ->
                _uiState.update { it.copy(recordingDuration = duration) }
            }
        }

        // Observe voice player state
        viewModelScope.launch {
            voicePlayer.playbackState.collect { state ->
                _uiState.update {
                    it.copy(
                        isPlayingVoice = state == VoicePlayer.PlaybackState.PLAYING,
                        isPausedVoice = state == VoicePlayer.PlaybackState.PAUSED
                    )
                }
            }
        }

        viewModelScope.launch {
            voicePlayer.playbackPosition.collect { position ->
                _uiState.update { it.copy(playbackPosition = position) }
            }
        }
    }

    private fun checkPermissions() {
        val permissionSummary = permissionManager.getMediaPermissionsSummary()
        _uiState.update { it.copy(permissionStatus = permissionSummary) }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            messageRepository.getAllMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun checkApiKey() {
        viewModelScope.launch {
            val activeProviderId = providerRepository.getActiveProviderId()
            val hasApiKey = if (activeProviderId != -1L) {
                providerRepository.hasApiKeyForProvider(activeProviderId)
            } else {
                false
            }
            _uiState.update { it.copy(hasApiKey = hasApiKey) }
        }
    }

    fun refreshApiKeyStatus() {
        checkApiKey()
    }

    fun refreshAvailableModels() {
        refreshModels()
    }

    
    private fun observeNetworkState() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                _uiState.update { it.copy(isConnected = isConnected) }
            }
        }
    }

    fun onMessageChanged(message: String) {
        _uiState.update { it.copy(inputMessage = message) }
    }

    fun sendMessage() {
        val currentMessage = _uiState.value.inputMessage.trim()
        if (currentMessage.isBlank()) return

        if (!uiState.value.hasApiKey) {
            _uiState.update {
                it.copy(
                    errorMessage = "Please configure a provider with API key first"
                )
            }
            return
        }

        if (!_uiState.value.isConnected) {
            _uiState.update {
                it.copy(
                    errorMessage = "No internet connection",
                    isLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                llmService.setPartialListener { partial ->
                    _uiState.update { it.copy(streamingContent = partial) }
                }

                // Create user message
                val userMessage = Message(
                    content = currentMessage,
                    timestamp = Date(),
                    isFromUser = true
                )

                // Insert user message
                messageRepository.insertMessage(userMessage)

                // Clear input and show loading
                _uiState.update {
                    it.copy(
                        inputMessage = "",
                        isLoading = true,
                        errorMessage = null
                    )
                }

                // Get current conversation history
                val messages = _uiState.value.messages + userMessage

                // Send to LLM service
                try {
                    llmService.sendMessage(currentMessage, messages, null as String?).collect { result ->
                        result.fold(
                            onSuccess = { aiResponse ->
                                // Remove loading message and add actual AI response
                                messageRepository.deleteLoadingMessages()

                                val aiMessage = Message(
                                    content = aiResponse,
                                    timestamp = Date(),
                                    isFromUser = false
                                )
                                messageRepository.insertMessage(aiMessage)
                                _uiState.update { it.copy(isLoading = false, streamingContent = null) }
                            },
                            onFailure = { error ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        streamingContent = null,
                                        errorMessage = "Failed to get response: ${error.message ?: error::class.java.simpleName}"
                                    )
                                }
                            }
                        )
                    }
                } catch (e: Exception) {
                    // Remove loading message on error
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            streamingContent = null,
                            errorMessage = "Error sending message: ${e.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        streamingContent = null,
                        errorMessage = "Unexpected error sending message: ${e.message}"
                    )
                }
            } finally {
                llmService.setPartialListener(null)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun navigateToSettings() {
        _uiState.update { it.copy(navigateToSettings = true) }
    }

    fun onSettingsNavigated() {
        _uiState.update { it.copy(navigateToSettings = false) }
    }

    fun onApiKeyUpdated() {
        checkApiKey()
    }

    // Voice recording methods
    fun startVoiceRecording() {
        if (!permissionManager.hasVoiceRecordingPermissions()) {
            _uiState.update {
                it.copy(errorMessage = "Microphone permission required for voice recording")
            }
            return
        }

        viewModelScope.launch {
            try {
                val outputFile = mediaStorageManager.createVoiceFile()
                voiceRecorder.startRecording(outputFile).fold(
                    onSuccess = {
                        _uiState.update { it.copy(errorMessage = null) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = "Failed to start recording: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error starting voice recording: ${e.message}") }
            }
        }
    }

    fun stopVoiceRecording() {
        viewModelScope.launch {
            try {
                voiceRecorder.stopRecording().fold(
                    onSuccess = { (audioFile, durationMs) ->
                        sendVoiceMessage(audioFile, durationMs)
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = "Failed to stop recording: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error stopping voice recording: ${e.message}") }
            }
        }
    }

    fun cancelVoiceRecording() {
        try {
            voiceRecorder.cancelRecording()
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Error canceling voice recording: ${e.message}") }
        }
    }

    fun cancelPendingResponse() {
        viewModelScope.launch {
            llmService.cancelActiveRequest()
            _uiState.update { it.copy(isLoading = false, streamingContent = null, errorMessage = "Canceled") }
            llmService.setPartialListener(null)
        }
    }

    private suspend fun sendVoiceMessage(audioFile: File, durationMs: Long) {
        try {
            val voiceMessage = Message(
                content = "Voice message (${formatDuration(durationMs)})",
                timestamp = Date(),
                isFromUser = true,
                contentType = ContentType.VOICE,
                mediaUrl = audioFile.absolutePath,
                duration = durationMs,
                mediaSize = audioFile.length()
            )

            messageRepository.insertMessage(voiceMessage)

            // Send to LLM for transcription and response
            sendMediaMessageToLLM(voiceMessage)
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Error sending voice message: ${e.message}") }
        }
    }

    // Voice playback methods
    fun playVoiceMessage(mediaUrl: String) {
        val file = File(mediaUrl)
        if (file.exists()) {
            voicePlayer.play(file).fold(
                onSuccess = {
                    _uiState.update { it.copy(errorMessage = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = "Failed to play voice message: ${error.message}") }
                }
            )
        } else {
            _uiState.update { it.copy(errorMessage = "Voice file not found") }
        }
    }

    fun pauseVoicePlayback() {
        voicePlayer.pause().fold(
            onSuccess = {
                _uiState.update { it.copy(errorMessage = null) }
            },
            onFailure = { error ->
                _uiState.update { it.copy(errorMessage = "Failed to pause playback: ${error.message}") }
            }
        )
    }

    fun resumeVoicePlayback() {
        voicePlayer.resume().fold(
            onSuccess = {
                _uiState.update { it.copy(errorMessage = null) }
            },
            onFailure = { error ->
                _uiState.update { it.copy(errorMessage = "Failed to resume playback: ${error.message}") }
            }
        )
    }

    fun stopVoicePlayback() {
        voicePlayer.stop()
    }

    // Photo handling methods
    fun capturePhoto() {
        if (!permissionManager.hasCameraPermissions()) {
            _uiState.update {
                it.copy(errorMessage = "Camera permission required for photo capture")
            }
            return
        }

        _uiState.update { it.copy(showCameraDialog = true) }
    }

    fun selectPhotoFromGallery() {
        if (!permissionManager.hasGalleryPermissions()) {
            _uiState.update {
                it.copy(errorMessage = "Gallery permission required for photo selection")
            }
            return
        }

        _uiState.update { it.copy(showGalleryPicker = true) }
    }

    fun onPhotoCaptured(bitmap: android.graphics.Bitmap) {
        _uiState.update {
            it.copy(
                pendingPhoto = bitmap,
                showPhotoPreview = true,
                showCameraDialog = false,
                showGalleryPicker = false,
                errorMessage = null
            )
        }
    }

    fun confirmPendingPhoto() {
        viewModelScope.launch {
            val pendingPhoto = _uiState.value.pendingPhoto ?: return@launch
            saveAndSendPhoto(pendingPhoto)
        }
    }

    fun cancelPendingPhoto() {
        _uiState.update {
            it.copy(
                pendingPhoto = null,
                showPhotoPreview = false,
                errorMessage = null
            )
        }
    }

    fun onPhotoSelected(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                photoCaptureManager.processGalleryResult(uri).fold(
                    onSuccess = { bitmap ->
                        onPhotoCaptured(bitmap)
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                errorMessage = "Failed to process photo: ${error.message}",
                                showGalleryPicker = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Error selecting photo: ${e.message}",
                        showGalleryPicker = false
                    )
                }
            }
        }
    }

    private suspend fun saveAndSendPhoto(bitmap: Bitmap) {
        try {
            mediaStorageManager.savePhoto(bitmap).fold(
                onSuccess = { (photoUrl, thumbnailUrl) ->
                    val photoFile = File(photoUrl)
                    val photoMessage = Message(
                        content = "Photo",
                        timestamp = Date(),
                        isFromUser = true,
                        contentType = ContentType.PHOTO,
                        mediaUrl = photoUrl,
                        thumbnailUrl = thumbnailUrl,
                        mediaSize = photoFile.length()
                    )

                    messageRepository.insertMessage(photoMessage)
                    sendMediaMessageToLLM(photoMessage)

                    _uiState.update {
                        it.copy(
                            showCameraDialog = false,
                            showGalleryPicker = false,
                            showPhotoPreview = false,
                            pendingPhoto = null,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to save photo: ${error.message}",
                            showCameraDialog = false,
                            showGalleryPicker = false,
                            showPhotoPreview = false,
                            pendingPhoto = null
                        )
                    }
                }
            )
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    errorMessage = "Error processing photo: ${e.message}",
                    showCameraDialog = false,
                    showGalleryPicker = false,
                    showPhotoPreview = false,
                    pendingPhoto = null
                )
            }
        }
    }

    fun dismissPhotoDialogs() {
        _uiState.update {
            it.copy(
                showCameraDialog = false,
                showGalleryPicker = false
            )
        }
    }

    private suspend fun sendMediaMessageToLLM(mediaMessage: Message) {
        try {
            llmService.setPartialListener { partial ->
                _uiState.update { it.copy(streamingContent = partial) }
            }

            // Get current conversation history
            val messages = _uiState.value.messages + mediaMessage

            // Send to LLM service with media content
            try {
                llmService.sendMessage(mediaMessage.content, messages, mediaMessage).collect { result ->
                    result.fold(
                        onSuccess = { aiResponse ->
                            val aiMessage = Message(
                                content = aiResponse,
                                timestamp = Date(),
                                isFromUser = false
                            )
                            messageRepository.insertMessage(aiMessage)
                            _uiState.update { it.copy(isLoading = false, streamingContent = null) }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    streamingContent = null,
                                    errorMessage = "Failed to get response: ${error.message ?: error::class.java.simpleName}"
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        streamingContent = null,
                        errorMessage = "Error processing media message: ${e.message}"
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    streamingContent = null,
                    errorMessage = "Error processing media message: ${e.message}"
                )
            }
        } finally {
            llmService.setPartialListener(null)
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    // Model selection methods

    private fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                val activeProviders = providerRepository.getActiveProviders().first()
                val allModels = mutableListOf<ModelConfiguration>()

                activeProviders.forEach { provider ->
                    val models = modelConfigRepository.getActiveModelsForProvider(provider.id).first()
                    allModels.addAll(models)
                }

                _uiState.update { it.copy(availableModels = allModels) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(modelSwitchingError = "Failed to load models: ${e.message}")
                }
            }
        }
    }

    
    private fun loadCurrentModel() {
        viewModelScope.launch {
            try {
                val activeModelConfig = modelConfigRepository.getActiveModelConfig()
                if (activeModelConfig != null) {
                    _uiState.update { it.copy(currentModel = activeModelConfig) }
                } else {
                    // No model configured - try to auto-select a default model
                    tryAutoSelectDefaultModel()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(modelSwitchingError = "Failed to load current model: ${e.message}")
                }
            }
        }
    }

    private fun tryAutoSelectDefaultModel() {
        viewModelScope.launch {
            try {
                // Get the active provider first
                val activeProviderId = providerRepository.getActiveProviderId()
                if (activeProviderId != -1L) {
                    // Try to find a default model for this provider
                    val defaultModel = modelConfigRepository.getDefaultModelForProvider(activeProviderId)
                    if (defaultModel != null) {
                        // Auto-select this model
                        modelConfigRepository.setActiveModelConfig(defaultModel.id)
                        _uiState.update { it.copy(currentModel = defaultModel) }
                        // Re-check API key status after setting the model
                        checkApiKey()
                        return@launch
                    }
                }

                // If no default model found, try to get any active model
                val activeModel = modelConfigRepository.getFirstActiveModel()
                if (activeModel != null) {
                    modelConfigRepository.setActiveModelConfig(activeModel.id)
                    _uiState.update { it.copy(currentModel = activeModel) }
                    // Re-check API key status after setting the model
                    checkApiKey()
                } else {
                    // No models available at all
                    _uiState.update { it.copy(currentModel = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(currentModel = null) }
            }
        }
    }

    
    fun toggleModelSwitcher() {
        _uiState.update { it.copy(showModelSwitcher = !it.showModelSwitcher) }
    }

    fun selectModel(model: ModelConfiguration) {
        viewModelScope.launch {
            try {
                // Set the active model configuration
                modelConfigRepository.setActiveModelConfig(model.id)

                _uiState.update {
                    it.copy(
                        currentModel = model,
                        showModelSwitcher = false,
                        modelSwitchingError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(modelSwitchingError = "Failed to select model: ${e.message}")
                }
            }
        }
    }

    fun clearModelSwitchingError() {
        _uiState.update { it.copy(modelSwitchingError = null) }
    }

    fun refreshModels() {
        loadAvailableModels()
        loadCurrentModel()
    }

    fun getCurrentModelInfo(): String {
        val model = _uiState.value.currentModel
        return if (model != null) {
            "${model.displayName} (${model.modelName})"
        } else {
            "No model selected"
        }
    }
}

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputMessage: String = "",
    val isLoading: Boolean = false,
    val streamingContent: String? = null,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = false,
    val isConnected: Boolean = false,
    val navigateToSettings: Boolean = false,
    // Model selection information
    val availableModels: List<ModelConfiguration> = emptyList(),
    val currentModel: ModelConfiguration? = null,
    val showModelSwitcher: Boolean = false,
    val modelSwitchingError: String? = null,
    // Media recording state
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0,
    // Voice playback state
    val isPlayingVoice: Boolean = false,
    val isPausedVoice: Boolean = false,
    val playbackPosition: Long = 0,
    // Photo capture state
    val showCameraDialog: Boolean = false,
    val showGalleryPicker: Boolean = false,
    val showPhotoPreview: Boolean = false,
    val pendingPhoto: Bitmap? = null,
    // Permission status
    val permissionStatus: PermissionManager.MediaPermissionsSummary = PermissionManager.MediaPermissionsSummary(
        voiceRecordingStatus = PermissionManager.PermissionStatus.NOT_REQUIRED,
        cameraStatus = PermissionManager.PermissionStatus.NOT_REQUIRED,
        galleryStatus = PermissionManager.PermissionStatus.NOT_REQUIRED,
        hasMicrophoneHardware = false,
        hasCameraHardware = false
    )
)
