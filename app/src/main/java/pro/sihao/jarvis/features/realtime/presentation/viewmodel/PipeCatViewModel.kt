package pro.sihao.jarvis.features.realtime.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import pro.sihao.jarvis.platform.android.service.PipeCatServiceManager
import pro.sihao.jarvis.core.domain.model.PipeCatConfig
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.core.domain.model.TranscriptMessage
import pro.sihao.jarvis.core.domain.model.MessageRole
import pro.sihao.jarvis.core.domain.model.PipeCatEvent
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PipeCatViewModel @Inject constructor(
    private val pipeCatServiceManager: PipeCatServiceManager,
    private val configurationManager: pro.sihao.jarvis.features.realtime.data.config.ConfigurationManager
) : ViewModel() {
    companion object {
        private const val TAG = "PipeCatViewModel"
    }

    private val _uiState = MutableStateFlow(PipeCatUiState())
    val uiState: StateFlow<PipeCatUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "PipeCatViewModel initialized")
        observeConnectionState()
        observeTranscripts()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            pipeCatServiceManager.connectionState.collect { state ->
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

    private fun observeTranscripts() {
        viewModelScope.launch {
            Log.d(TAG, "observeTranscripts() started collecting eventFlow")
            pipeCatServiceManager.eventFlow.collect { event ->
                Log.d(TAG, "Received event: $event")
                when (event) {
                    is PipeCatEvent.UserTranscript -> {
                        Log.d(TAG, "Handling UserTranscript: ${event.text}")
                        handleUserTranscript(event.text, event.timestamp, event.isFinal)
                    }
                    is PipeCatEvent.BotLLMText -> {
                        Log.d(TAG, "Handling BotLLMText: ${event.text}")
                        handleBotLLMText(event.text, event.timestamp)
                    }
                    is PipeCatEvent.BotResponse -> {
                        Log.d(TAG, "Handling BotResponse: ${event.text}")
                        handleBotTranscript(event.text, event.timestamp)
                    }
                    is PipeCatEvent.BotStartedSpeaking -> {
                        Log.d(TAG, "Handling BotStartedSpeaking")
                        handleBotStartedSpeaking(event.timestamp)
                    }
                    is PipeCatEvent.BotStoppedSpeaking -> {
                        Log.d(TAG, "Handling BotStoppedSpeaking")
                        handleBotStoppedSpeaking(event.timestamp)
                    }
                    else -> {}
                }
            }
        }
    }

    private var lastUserMessageFinal = true

    private fun handleUserTranscript(text: String, timestamp: Date, isFinal: Boolean = false) {
        _uiState.update { state ->
            val transcripts = state.transcripts
            val updated = if (lastUserMessageFinal) {
                // Create new message (new turn)
                lastUserMessageFinal = isFinal  // Set based on SDK's final flag
                transcripts + TranscriptMessage(
                    id = generateMessageId(),
                    role = MessageRole.USER,
                    text = text,
                    timestamp = timestamp,
                    isFinal = isFinal
                )
            } else {
                // Update existing user message (same turn)
                val lastIndex = transcripts.indexOfLast { it.role == MessageRole.USER }
                if (lastIndex >= 0) {
                    transcripts.toMutableList().apply {
                        set(lastIndex, transcripts[lastIndex].copy(
                            text = text,
                            timestamp = timestamp,
                            isFinal = isFinal
                        ))
                        // If this is the final update, reset flag for next turn
                        if (isFinal) {
                            lastUserMessageFinal = true
                        }
                    }
                } else {
                    // Fallback: create new message if none exists
                    lastUserMessageFinal = isFinal
                    transcripts + TranscriptMessage(
                        id = generateMessageId(),
                        role = MessageRole.USER,
                        text = text,
                        timestamp = timestamp,
                        isFinal = isFinal
                    )
                }
            }
            state.copy(transcripts = updated)
        }
    }

    private var botSpeaking = false

    /**
     * Handle streaming LLM text chunks
     * Always appends to the current bot message to group all chunks in one message box
     */
    private fun handleBotLLMText(text: String, timestamp: Date) {
        _uiState.update { state ->
            val transcripts = state.transcripts
            val lastBotIndex = transcripts.indexOfLast { it.role == MessageRole.BOT }

            // Check if we should append to existing message or create new one
            val shouldAppend = lastBotIndex >= 0 &&
                !transcripts[lastBotIndex].isFinal &&
                botSpeaking

            val updated = if (shouldAppend) {
                // Append to existing bot message (streaming)
                transcripts.toMutableList().apply {
                    val lastMsg = transcripts[lastBotIndex]
                    set(lastBotIndex, lastMsg.copy(text = lastMsg.text + text, timestamp = timestamp))
                }
            } else {
                // Create new bot message
                botSpeaking = true
                transcripts + TranscriptMessage(
                    id = generateMessageId(),
                    role = MessageRole.BOT,
                    text = text,
                    timestamp = timestamp,
                    isFinal = false
                )
            }
            state.copy(transcripts = updated)
        }
    }

    private fun handleBotTranscript(text: String, timestamp: Date) {
        _uiState.update { state ->
            val transcripts = state.transcripts
            val updated = if (botSpeaking) {
                // Append to last bot message
                val lastIndex = transcripts.indexOfLast { it.role == MessageRole.BOT }
                if (lastIndex >= 0) {
                    transcripts.toMutableList().apply {
                        val lastMsg = transcripts[lastIndex]
                        set(lastIndex, lastMsg.copy(text = lastMsg.text + text, timestamp = timestamp))
                    }
                } else {
                    // Fallback: no bot message exists, create new one
                    botSpeaking = true
                    transcripts + TranscriptMessage(
                        id = generateMessageId(),
                        role = MessageRole.BOT,
                        text = text,
                        timestamp = timestamp
                    )
                }
            } else {
                // Check if we should append to the last bot message instead of creating new
                val lastBotIndex = transcripts.indexOfLast { it.role == MessageRole.BOT }
                val shouldAppend = lastBotIndex >= 0 &&
                    !transcripts[lastBotIndex].isFinal &&
                    (timestamp.time - transcripts[lastBotIndex].timestamp.time) < 5000 // Within 5 seconds

                if (shouldAppend) {
                    // Append to existing bot message (same round)
                    botSpeaking = true
                    transcripts.toMutableList().apply {
                        val lastMsg = transcripts[lastBotIndex]
                        set(lastBotIndex, lastMsg.copy(text = lastMsg.text + text, timestamp = timestamp))
                    }
                } else {
                    // Create new bot message
                    botSpeaking = true
                    transcripts + TranscriptMessage(
                        id = generateMessageId(),
                        role = MessageRole.BOT,
                        text = text,
                        timestamp = timestamp
                    )
                }
            }
            state.copy(transcripts = updated)
        }
    }

    /**
     * Handle bot started speaking event
     * Does NOT reset the flag to prevent splitting responses
     */
    private fun handleBotStartedSpeaking(timestamp: Date) {
        // Don't reset botSpeaking - this event can arrive mid-response
        // Only BotStoppedSpeaking should reset the flag for the next round
        Log.d(TAG, "Bot started speaking, keeping botSpeaking=$botSpeaking")
    }

    /**
     * Handle bot stopped speaking event
     * Marks the current bot message as complete and resets flag for next round
     */
    private fun handleBotStoppedSpeaking(timestamp: Date) {
        _uiState.update { state ->
            val transcripts = state.transcripts
            val lastIndex = transcripts.indexOfLast { it.role == MessageRole.BOT }
            if (lastIndex >= 0) {
                val updated = transcripts.toMutableList()
                updated[lastIndex] = transcripts[lastIndex].copy(isFinal = true)
                state.copy(transcripts = updated)
            } else {
                state
            }
        }
        // Reset flag so the next bot response will create a new message
        botSpeaking = false
        Log.d(TAG, "Bot stopped speaking, marked last bot message as final and reset botSpeaking flag")
    }

    private fun generateMessageId(): String = "${System.currentTimeMillis()}-${(0..999).random()}"

    fun clearTranscripts() {
        _uiState.update { it.copy(transcripts = emptyList()) }
        lastUserMessageFinal = true
        botSpeaking = false
    }

  
    fun connect(config: PipeCatConfig) {
        Log.d(TAG, "connect() called with config: $config")
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
            try {
                Log.d(TAG, "Calling pipeCatServiceManager.connect()")
                pipeCatServiceManager.connect(config)
                Log.d(TAG, "pipeCatServiceManager.connect() returned successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect", e)
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
        Log.d(TAG, "connectWithDefaultConfig() called")
        viewModelScope.launch {
            try {
                // Use ConfigurationManager to get current settings
                val config = configurationManager.getCurrentConfig()
                Log.d(TAG, "Got config: $config")

                // Validate configuration before connecting
                val validationResult = configurationManager.validateConfiguration()
                Log.d(TAG, "Validation result: isValid=${validationResult.isValid}, message=${validationResult.message}")
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
                Log.e(TAG, "Exception in connectWithDefaultConfig", e)
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
                pipeCatServiceManager.disconnect()
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
                pipeCatServiceManager.toggleMicrophone(enabled)
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
                pipeCatServiceManager.toggleCamera(enabled)
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

    /**
     * Start the PipeCat foreground service
     * Call this when the user wants to begin a voice session
     */
    fun startService() {
        try {
            pipeCatServiceManager.startPersistentService()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "Failed to start service: ${e.message}")
            }
        }
    }

    /**
     * Stop the PipeCat foreground service
     * Call this when the user wants to end the voice session completely
     */
    fun stopService() {
        try {
            pipeCatServiceManager.stopService()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "Failed to stop service: ${e.message}")
            }
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
    val transcripts: List<TranscriptMessage> = emptyList()
)