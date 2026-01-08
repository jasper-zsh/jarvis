package pro.sihao.jarvis.core.domain.model

import java.util.Date

/**
 * Configuration for PipeCat sessions
 */
data class PipeCatConfig(
    val enableMic: Boolean = false,
    val enableCam: Boolean = false,
    val botId: String? = null,
    val baseUrl: String,
    val apiKey: String? = null,
    val customHeaders: Map<String, String> = emptyMap()
)

/**
 * Sealed class for all PipeCat events
 */
sealed class PipeCatEvent {
    data class TransportStateChanged(val state: TransportState) : PipeCatEvent()
    data class BotReady(val data: BotReadyData) : PipeCatEvent()
    data class UserTranscript(val text: String, val timestamp: Date = Date(), val isFinal: Boolean = false) : PipeCatEvent()
    data class BotResponse(val text: String, val timestamp: Date = Date()) : PipeCatEvent()
    data class BotLLMText(val text: String, val timestamp: Date = Date()) : PipeCatEvent()
    data class BotStartedSpeaking(val timestamp: Date = Date()) : PipeCatEvent()
    data class BotStoppedSpeaking(val timestamp: Date = Date()) : PipeCatEvent()
    data class AudioLevelChanged(val level: Float, val isUser: Boolean) : PipeCatEvent()
    data class Error(val message: String, val throwable: Throwable? = null) : PipeCatEvent()
    object Disconnected : PipeCatEvent()

    // Text chat specific events
    data class TextResponseStarted(val timestamp: Date = Date()) : PipeCatEvent()
    data class TextResponsePartial(val content: String, val timestamp: Date = Date()) : PipeCatEvent()
    data class TextResponseComplete(val content: String, val timestamp: Date = Date()) : PipeCatEvent()
    data class MediaProcessingStarted(val mediaType: String, val timestamp: Date = Date()) : PipeCatEvent()
    data class MediaTranscriptionComplete(val transcription: String, val timestamp: Date = Date()) : PipeCatEvent()
    data class MediaAnalysisComplete(val analysis: String, val timestamp: Date = Date()) : PipeCatEvent()
    object RequestCanceled : PipeCatEvent()

    object Bye : PipeCatEvent()
}

/**
 * Transport state for PipeCat connections
 */
enum class TransportState {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * Connection mode for connection lifecycle management
 */
enum class ConnectionMode {
    NEVER_CONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    MANUALLY_DISCONNECTED,
    AUTO_DISCONNECTED,
    ERROR
}

/**
 * Microphone state for microphone lifecycle management
 */
enum class MicrophoneState {
    CLOSED,
    OPENING,
    OPEN,
    CLOSING
}

/**
 * Data for bot ready event
 */
data class BotReadyData(
    val botId: String,
    val capabilities: List<String> = emptyList()
)

/**
 * Connection management state for tracking connection lifecycle and manual control
 */
data class ConnectionManagementState(
    val connectionMode: ConnectionMode = ConnectionMode.NEVER_CONNECTED,
    val microphoneState: MicrophoneState = MicrophoneState.CLOSED,
    val isAutoReconnectEnabled: Boolean = true,
    val isManuallyDisconnected: Boolean = false,
    val isGlassesAwake: Boolean = false,
    val connectionRetryCount: Int = 0
)

/**
 * Connection state for PipeCat
 */
data class PipeCatConnectionState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val botReady: Boolean = false,
    val botIsSpeaking: Boolean = false,
    val userIsSpeaking: Boolean = false,
    val botAudioLevel: Float = 0f,
    val userAudioLevel: Float = 0f,
    val errorMessage: String? = null,
    val config: PipeCatConfig? = null,
    val transportState: TransportState = TransportState.IDLE,
    val connectionManagementState: ConnectionManagementState = ConnectionManagementState(),
    val isManuallyDisconnected: Boolean = false
)

/**
 * Message role enumeration
 */
enum class MessageRole {
    USER,
    BOT
}

/**
 * Transcript message for conversation history
 */
data class TranscriptMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val timestamp: Date,
    val isFinal: Boolean = true
)