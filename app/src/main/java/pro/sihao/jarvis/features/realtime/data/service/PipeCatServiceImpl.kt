package pro.sihao.jarvis.features.realtime.data.service

import android.content.Context
import android.util.Log
import ai.pipecat.client.PipecatClient
import ai.pipecat.client.PipecatClientOptions
import ai.pipecat.client.PipecatEventCallbacks
import ai.pipecat.client.result.Future
import ai.pipecat.client.result.RTVIError
import ai.pipecat.client.small_webrtc_transport.SmallWebRTCTransport
import ai.pipecat.client.types.APIRequest
import ai.pipecat.client.types.BotOutputData
import ai.pipecat.client.types.BotReadyData
import ai.pipecat.client.types.LLMFunctionCallData
import ai.pipecat.client.types.LLMFunctionCallHandler
import ai.pipecat.client.types.Participant
import ai.pipecat.client.types.PipecatMetrics
import ai.pipecat.client.types.Tracks
import ai.pipecat.client.types.Transcript
import ai.pipecat.client.types.TransportState as PipecatTransportState
import ai.pipecat.client.types.Value
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import pro.sihao.jarvis.platform.android.audio.AudioRoutingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import pro.sihao.jarvis.core.domain.model.ContentType
import pro.sihao.jarvis.core.domain.model.Message
import pro.sihao.jarvis.core.domain.model.PipeCatConfig
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.core.domain.model.PipeCatEvent
import pro.sihao.jarvis.core.domain.model.TransportState as AppTransportState
import pro.sihao.jarvis.core.domain.service.PipeCatService
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PipeCatService using PipeCat SDK
 */
@Singleton
class PipeCatServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRoutingManager: AudioRoutingManager
) : PipeCatService {

    companion object {
        private const val TAG = "PipeCatServiceImpl"
    }

    private var pipecatClient: PipecatClient<*, *>? = null

    // Audio input management
    private var audioInputCallback: ((ByteArray) -> Unit)? = null
    private var usingGlassesAudio = false

    // Extension function for error handling
    private fun <E> Future<E, RTVIError>.displayErrors() = withErrorCallback { error ->
        Log.e(TAG, "PipeCat operation failed: ${error.description}", error.exception)
        _connectionState.update {
            it.copy(
                errorMessage = error.description,
                transportState = AppTransportState.ERROR,
                isConnecting = false
            )
        }
    }
    private val _connectionState = MutableStateFlow(PipeCatConnectionState())
    override val connectionState: StateFlow<PipeCatConnectionState> = _connectionState.asStateFlow()

    private val _eventFlow = MutableStateFlow<PipeCatEvent?>(null)
    private val cancelFlag = AtomicBoolean(false)

    // Real-time session management
    private var isSessionActive = false

    override suspend fun sendTextMessage(
        message: String,
        conversationHistory: List<Message>
    ): Flow<PipeCatEvent> = flow {
        cancelFlag.set(false)
        try {
            emit(PipeCatEvent.TextResponseStarted())

            // Build conversation context
            val context = buildConversationContext(conversationHistory, message)

            // For now, simulate a mock response
            // In a real implementation, this would call the PipeCat SDK
            val mockResponse = generateMockResponse(message, conversationHistory)

            // Simulate streaming response
            emit(PipeCatEvent.TextResponsePartial(mockResponse.substring(0, mockResponse.length / 2)))
            kotlinx.coroutines.delay(100)

            if (!currentCoroutineContext().isActive || cancelFlag.get()) {
                emit(PipeCatEvent.RequestCanceled)
                return@flow
            }

            emit(PipeCatEvent.TextResponseComplete(mockResponse))

        } catch (e: CancellationException) {
            emit(PipeCatEvent.RequestCanceled)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text message", e)
            emit(PipeCatEvent.Error("Failed to send message: ${e.message}", e))
        } finally {
            cancelFlag.set(false)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun sendMediaMessage(
        message: String,
        mediaMessage: Message,
        conversationHistory: List<Message>
    ): Flow<PipeCatEvent> = flow {
        cancelFlag.set(false)
        try {
            val mediaType = when (mediaMessage.contentType) {
                ContentType.VOICE -> "voice"
                ContentType.PHOTO -> "photo"
                else -> "unknown"
            }

            emit(PipeCatEvent.MediaProcessingStarted(mediaType))

            // Simulate media processing
            when (mediaMessage.contentType) {
                ContentType.VOICE -> {
                    // Simulate voice transcription
                    kotlinx.coroutines.delay(500)
                    if (!currentCoroutineContext().isActive || cancelFlag.get()) {
                        emit(PipeCatEvent.RequestCanceled)
                        return@flow
                    }

                    val transcription = "Voice message: ${message.ifBlank { "Transcribed audio content" }}"
                    emit(PipeCatEvent.MediaTranscriptionComplete(transcription))

                    // Generate response based on transcription
                    val response = generateMockResponse(transcription, conversationHistory)
                    emit(PipeCatEvent.TextResponseComplete(response))
                }

                ContentType.PHOTO -> {
                    // Simulate image analysis
                    kotlinx.coroutines.delay(800)
                    if (!currentCoroutineContext().isActive || cancelFlag.get()) {
                        emit(PipeCatEvent.RequestCanceled)
                        return@flow
                    }

                    val analysis = "I can see you've sent a photo. ${message.ifBlank { "What would you like me to tell you about this image?" }}"
                    emit(PipeCatEvent.MediaAnalysisComplete(analysis))

                    // Generate response based on analysis
                    val response = generateMockResponse(analysis, conversationHistory)
                    emit(PipeCatEvent.TextResponseComplete(response))
                }

                else -> {
                    emit(PipeCatEvent.Error("Unsupported media type: ${mediaMessage.contentType}"))
                }
            }

        } catch (e: CancellationException) {
            emit(PipeCatEvent.RequestCanceled)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing media message", e)
            emit(PipeCatEvent.Error("Failed to process media: ${e.message}", e))
        } finally {
            cancelFlag.set(false)
        }
    }.flowOn(Dispatchers.IO)

    override fun cancelActiveRequest() {
        cancelFlag.set(true)
    }

    private fun buildConversationContext(history: List<Message>, currentMessage: String): String {
        val context = StringBuilder()
        context.appendLine("System: You are Jarvis, a helpful AI assistant.")

        // Add last 10 messages from history
        history.takeLast(10).forEach { msg ->
            val role = if (msg.isFromUser) "User" else "Assistant"
            context.appendLine("$role: ${msg.content}")
        }

        context.appendLine("User: $currentMessage")
        return context.toString()
    }

    private fun generateMockResponse(message: String, history: List<Message>): String {
        // Simple mock response generation
        // In real implementation, this would be replaced with actual PipeCat SDK calls
        val lowerMessage = message.lowercase()

        return when {
            lowerMessage.contains("hello") || lowerMessage.contains("hi") ->
                "Hello! I'm ready to help you. How can I assist you today?"

            lowerMessage.contains("how are you") ->
                "I'm doing well, thank you for asking! I'm here to help with whatever you need."

            lowerMessage.contains("weather") ->
                "I don't have access to current weather information, but I can help you with many other tasks!"

            lowerMessage.contains("help") ->
                "I can help you with conversations, analyze images, transcribe voice messages, and assist with various tasks through your glasses integration. What would you like help with?"

            lowerMessage.contains("jokes") || lowerMessage.contains("funny") ->
                "Why don't scientists trust atoms? Because they make up everything! 😄"

            history.isNotEmpty() ->
                "I understand your message. Based on our conversation, I'm here to continue helping you. Could you tell me more about what you'd like assistance with?"

            else ->
                "I understand you've sent: \"$message\". I'm here to help you with text conversations, media analysis, and real-time communication through your glasses. How can I assist you today?"
        }
    }

    override suspend fun startRealtimeSession(config: PipeCatConfig): Flow<PipeCatEvent> = flow {
        try {
            if (isSessionActive) {
                Log.w(TAG, "Session already active, stopping existing session")
                stopRealtimeSession()
            }

            // Enable speaker mode for audio output
            audioRoutingManager.setSpeakerMode(true)
            Log.i(TAG, "Speaker mode enabled for PipeCat session")

            // Update connection state to connecting
            _connectionState.update {
                it.copy(
                    isConnecting = true,
                    isConnected = false,
                    config = config,
                    errorMessage = null,
                    transportState = AppTransportState.CONNECTING
                )
            }

            // Create PipeCat event callbacks
            val callbacks = object : PipecatEventCallbacks() {
                override fun onTransportStateChanged(state: PipecatTransportState) {
                    Log.i(TAG, "Transport state changed: $state")
                    _connectionState.update { current ->
                        current.copy(
                            transportState = when (state.name) {
                                "IDLE", "Initializing", "Initialized" -> AppTransportState.IDLE
                                "Authorizing", "Authorized", "Connecting" -> AppTransportState.CONNECTING
                                "Connected" -> AppTransportState.CONNECTED
                                "Disconnecting" -> AppTransportState.DISCONNECTING
                                "ERROR" -> AppTransportState.ERROR
                                else -> AppTransportState.IDLE
                            },
                            isConnected = state.name == "Connected",
                            isConnecting = state.name in listOf("Authorizing", "Authorized", "Connecting")
                        )
                    }
                }

                override fun onBackendError(message: String) {
                    Log.e(TAG, "Backend error: $message")
                    _connectionState.update {
                        it.copy(errorMessage = "Backend error: $message")
                    }
                    _eventFlow.value = PipeCatEvent.Error("Backend error: $message")
                }

                override fun onBotReady(data: BotReadyData) {
                    Log.i(TAG, "Bot ready: $data")
                    _connectionState.update {
                        it.copy(
                            botReady = true,
                            isConnecting = false,
                            isConnected = true
                        )
                    }
                    _eventFlow.value = PipeCatEvent.BotReady(
                        pro.sihao.jarvis.core.domain.model.BotReadyData(
                            botId = "jarvis-assistant", // Use default for now
                            capabilities = listOf("voice", "text")
                        )
                    )
                }

                override fun onMetrics(data: PipecatMetrics) {
                    Log.i(TAG, "Pipecat metrics: $data")
                    // Update connection metrics in state - placeholder for now
                    // TODO: Implement proper audio level extraction from PipecatMetrics
                }

                override fun onBotOutput(data: BotOutputData) {
                    Log.i(TAG, "Bot output: $data")
                    // Handle bot output if needed
                }

                override fun onUserTranscript(data: Transcript) {
                    Log.i(TAG, "User transcript: $data")
                    _eventFlow.value = PipeCatEvent.UserTranscript(
                        text = data.text,
                        timestamp = Date()
                    )
                }

                override fun onBotStartedSpeaking() {
                    Log.i(TAG, "Bot started speaking")
                    _connectionState.update {
                        it.copy(botIsSpeaking = true)
                    }
                    _eventFlow.value = PipeCatEvent.BotStartedSpeaking()
                }

                override fun onBotStoppedSpeaking() {
                    Log.i(TAG, "Bot stopped speaking")
                    _connectionState.update {
                        it.copy(botIsSpeaking = false)
                    }
                    _eventFlow.value = PipeCatEvent.BotStoppedSpeaking()
                }

                override fun onUserStartedSpeaking() {
                    Log.i(TAG, "User started speaking")
                    _connectionState.update {
                        it.copy(userIsSpeaking = true)
                    }
                }

                override fun onUserStoppedSpeaking() {
                    Log.i(TAG, "User stopped speaking")
                    _connectionState.update {
                        it.copy(userIsSpeaking = false)
                    }
                }

                override fun onTracksUpdated(tracks: Tracks) {
                    Log.i(TAG, "Tracks updated: $tracks")
                    // Handle track updates
                }

                override fun onInputsUpdated(camera: Boolean, mic: Boolean) {
                    Log.i(TAG, "Inputs updated - Camera: $camera, Mic: $mic")
                    // Update input states if needed
                }

                override fun onDisconnected() {
                    Log.i(TAG, "Disconnected")
                    isSessionActive = false
                    _connectionState.update {
                        PipeCatConnectionState()
                    }
                    _eventFlow.value = PipeCatEvent.Disconnected
                }

                override fun onUserAudioLevel(level: Float) {
                    _connectionState.update { current ->
                        current.copy(userAudioLevel = level)
                    }
                }

                override fun onRemoteAudioLevel(level: Float, participant: Participant) {
                    _connectionState.update { current ->
                        current.copy(botAudioLevel = level)
                    }
                }
            }

            // Create PipeCat client options
            val options = PipecatClientOptions(
                enableMic = config.enableMic,
                enableCam = config.enableCam,
                callbacks = callbacks
            )

            // Initialize PipeCat client with SmallWebRTC transport on main thread
            pipecatClient = withContext(Dispatchers.Main) {
                PipecatClient(
                    transport = SmallWebRTCTransport(context),
                    options = options
                )
            }

            pipecatClient?.registerFunctionCallHandler("CloseWhenNothingToDo", object : LLMFunctionCallHandler {
                override fun handleFunctionCall(
                    data: LLMFunctionCallData,
                    onResult: (Value) -> Unit
                ) {
                    onResult(Value.Str("Session closed"))
                    CoroutineScope(Dispatchers.IO).launch {
                        this@PipeCatServiceImpl.stopRealtimeSession()
                    }
                }

            })

            // Build API request headers
            val headers = buildMap {
                config.apiKey?.takeIf { it.isNotEmpty() }?.let {
                    put("Authorization", "Bearer $it")
                }
                config.customHeaders.forEach { (key, value) ->
                    put(key, value)
                }
            }

            // Start bot and connect
            val apiRequest = APIRequest(
                endpoint = config.baseUrl,
                requestData = Value.Object(),
                headers = headers
            )


            pipecatClient?.startBotAndConnect(apiRequest)?.displayErrors()?.withErrorCallback {
                // Session ended or disconnected
                isSessionActive = false
            }


            isSessionActive = true

            // Emit events from the flow
            var lastEvent: PipeCatEvent? = null
            while (isSessionActive) {
                val currentEvent = _eventFlow.value
                if (currentEvent != null && currentEvent != lastEvent) {
                    emit(currentEvent)
                    lastEvent = currentEvent
                }
                kotlinx.coroutines.delay(50) // Poll for events
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting PipeCat session", e)
            isSessionActive = false
            _connectionState.update {
                it.copy(
                    errorMessage = e.message ?: "Unknown error",
                    transportState = AppTransportState.ERROR,
                    isConnecting = false
                )
            }
            emit(PipeCatEvent.Error(e.message ?: "Unknown error", e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun stopRealtimeSession() {
        try {
            isSessionActive = false

            _connectionState.update {
                it.copy(
                    isConnecting = false,
                    isConnected = false,
                    botReady = false,
                    transportState = AppTransportState.DISCONNECTING
                )
            }

            // Disconnect the PipeCat client on main thread
            withContext(Dispatchers.Main) {
                pipecatClient?.disconnect()?.displayErrors()
                pipecatClient?.release()
                pipecatClient = null
            }

            // Disable speaker mode when session ends
            audioRoutingManager.setSpeakerMode(false)
            Log.i(TAG, "Speaker mode disabled after PipeCat session end")

            _connectionState.update {
                PipeCatConnectionState()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping PipeCat session", e)
            _connectionState.update {
                it.copy(errorMessage = e.message ?: "Error stopping session")
            }
        }
    }

    override fun toggleMicrophone(enabled: Boolean) {
        try {
            // Run SDK calls on main thread
            CoroutineScope(Dispatchers.Main).launch {
                pipecatClient?.enableMic(enabled)?.displayErrors()
            }
            _connectionState.update {
                it.copy(config = it.config?.copy(enableMic = enabled))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling microphone", e)
            _connectionState.update {
                it.copy(errorMessage = e.message ?: "Microphone error")
            }
        }
    }

    override fun toggleCamera(enabled: Boolean) {
        try {
            // Run SDK calls on main thread
            CoroutineScope(Dispatchers.Main).launch {
                pipecatClient?.enableCam(enabled)?.displayErrors()
            }
            _connectionState.update {
                it.copy(config = it.config?.copy(enableCam = enabled))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling camera", e)
            _connectionState.update {
                it.copy(errorMessage = e.message ?: "Camera error")
            }
        }
    }

    override fun getCurrentState(): PipeCatConnectionState {
        return _connectionState.value
    }

    override fun setCustomAudioInput(audioCallback: ((ByteArray) -> Unit)?) {
        this.audioInputCallback = audioCallback
        this.usingGlassesAudio = audioCallback != null
        Log.i(TAG, "Custom audio input ${if (usingGlassesAudio) "enabled" else "disabled"}")
    }

    override fun isUsingCustomAudioInput(): Boolean {
        return usingGlassesAudio
    }

    override fun sendAudioData(audioData: ByteArray) {
        try {
            if (isSessionActive && usingGlassesAudio) {
                // Send audio data to PipeCat - this will need to be implemented
                // based on the actual PipeCat SDK audio input capabilities
                Log.v(TAG, "Sending ${audioData.size} bytes of audio data to PipeCat")

                // TODO: Implement actual audio data sending to PipeCat SDK
                // This depends on the PipeCat SDK's audio input interface
                // For now, we'll log the audio data reception

            } else {
                Log.w(TAG, "Cannot send audio data: session not active or not using custom audio")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audio data to PipeCat", e)
        }
    }

    override fun setSpeakerMode(enabled: Boolean) {
        try {
            audioRoutingManager.setSpeakerMode(enabled)
            Log.i(TAG, "Speaker mode ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speaker mode", e)
        }
    }
}