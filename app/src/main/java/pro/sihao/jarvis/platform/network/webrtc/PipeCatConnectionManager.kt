package pro.sihao.jarvis.platform.network.webrtc

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pro.sihao.jarvis.features.realtime.data.service.PipeCatServiceImpl
import pro.sihao.jarvis.core.domain.model.ContentType
import pro.sihao.jarvis.core.domain.model.Message
import pro.sihao.jarvis.core.domain.model.PipeCatConfig
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.core.domain.model.PipeCatEvent
import pro.sihao.jarvis.core.domain.repository.MessageRepository
import pro.sihao.jarvis.core.domain.service.PipeCatService
import pro.sihao.jarvis.features.realtime.data.config.ConfigurationManager
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for PipeCat connections and state
 */
@Singleton
class PipeCatConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pipeCatService: PipeCatService,
    private val messageRepository: MessageRepository,
    private val configurationManager: ConfigurationManager
) {
    companion object {
        private const val TAG = "PipeCatConnectionManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var connectionJob: Job? = null

    private val _connectionState = MutableStateFlow(PipeCatConnectionState())
    val connectionState: StateFlow<PipeCatConnectionState> = _connectionState.asStateFlow()

    init {
        // Observe PipeCat service connection state
        scope.launch {
            pipeCatService.connectionState.collect { serviceState ->
                _connectionState.update { serviceState }
            }
        }
    }

    /**
     * Connect to PipeCat with the given configuration
     */
    suspend fun connect(config: PipeCatConfig) {
        if (_connectionState.value.isConnecting || _connectionState.value.isConnected) {
            Log.w(TAG, "Already connecting or connected")
            return
        }

        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                _connectionState.update { 
                    it.copy(
                        isConnecting = true,
                        isConnected = false,
                        errorMessage = null
                    )
                }

                pipeCatService.startRealtimeSession(config).collect { event ->
                    handlePipeCatEvent(event)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to PipeCat", e)
                _connectionState.update { 
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = e.message ?: "Connection error"
                    )
                }
            }
        }
    }

    /**
     * Disconnect from PipeCat
     */
    suspend fun disconnect() {
        connectionJob?.cancel()
        pipeCatService.stopRealtimeSession()
    }

    /**
     * Toggle microphone on/off
     */
    fun toggleMicrophone(enabled: Boolean) {
        pipeCatService.toggleMicrophone(enabled)
    }

    /**
     * Toggle camera on/off
     */
    fun toggleCamera(enabled: Boolean) {
        pipeCatService.toggleCamera(enabled)
    }

    /**
     * Send audio data to PipeCat
     * Note: This is now primarily handled by PipeCatForegroundService for glasses integration
     */
    fun sendAudioData(audioData: ByteArray) {
        pipeCatService.sendAudioData(audioData)
    }

    /**
     * Handle PipeCat events
     */
    private suspend fun handlePipeCatEvent(event: PipeCatEvent) {
        when (event) {
            is PipeCatEvent.UserTranscript -> {
                val message = Message(
                    content = event.text,
                    timestamp = event.timestamp,
                    isFromUser = true,
                    contentType = ContentType.REALTIME_TRANSCRIPT
                )
                messageRepository.insertMessage(message)
                Log.d(TAG, "Inserted user transcript message: ${event.text}")
            }
            
            is PipeCatEvent.BotResponse -> {
                val message = Message(
                    content = event.text,
                    timestamp = event.timestamp,
                    isFromUser = false,
                    contentType = ContentType.REALTIME_RESPONSE
                )
                messageRepository.insertMessage(message)
                Log.d(TAG, "Inserted bot response message: ${event.text}")
            }
            
            is PipeCatEvent.BotStartedSpeaking -> {
                Log.d(TAG, "Bot started speaking at ${event.timestamp}")
            }
            
            is PipeCatEvent.BotStoppedSpeaking -> {
                Log.d(TAG, "Bot stopped speaking at ${event.timestamp}")
            }
            
            is PipeCatEvent.AudioLevelChanged -> {
                // Audio level is already handled in the service state
                Log.v(TAG, "Audio level changed: ${event.level} (user: ${event.isUser})")
            }
            
            is PipeCatEvent.Error -> {
                Log.e(TAG, "PipeCat error: ${event.message}", event.throwable)
                _connectionState.update { 
                    it.copy(errorMessage = event.message)
                }
            }
            
            is PipeCatEvent.Disconnected -> {
                Log.i(TAG, "PipeCat disconnected")
                _connectionState.update { 
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        botReady = false,
                        botIsSpeaking = false,
                        userIsSpeaking = false
                    )
                }
            }
            
            is PipeCatEvent.TransportStateChanged -> {
                Log.d(TAG, "Transport state changed to: ${event.state}")
                _connectionState.update { 
                    it.copy(
                        isConnected = event.state == pro.sihao.jarvis.core.domain.model.TransportState.CONNECTED,
                        isConnecting = event.state == pro.sihao.jarvis.core.domain.model.TransportState.CONNECTING
                    )
                }
            }
            
            is PipeCatEvent.BotReady -> {
                Log.i(TAG, "Bot ready with ID: ${event.data.botId}")
                _connectionState.update {
                    it.copy(botReady = true)
                }
                // Note: ASR content handling is now done in PipeCatForegroundService
            }

            // New text chat events
            is PipeCatEvent.TextResponseStarted -> {
                Log.d(TAG, "Text response started at ${event.timestamp}")
            }

            is PipeCatEvent.TextResponsePartial -> {
                Log.v(TAG, "Text response partial: ${event.content}")
            }

            is PipeCatEvent.TextResponseComplete -> {
                Log.d(TAG, "Text response completed: ${event.content}")
            }

            is PipeCatEvent.MediaProcessingStarted -> {
                Log.d(TAG, "Media processing started for ${event.mediaType}")
            }

            is PipeCatEvent.MediaTranscriptionComplete -> {
                Log.d(TAG, "Media transcription: ${event.transcription}")
            }

            is PipeCatEvent.MediaAnalysisComplete -> {
                Log.d(TAG, "Media analysis: ${event.analysis}")
            }

            is PipeCatEvent.RequestCanceled -> {
                Log.d(TAG, "Request was canceled")
            }
        }
    }
}