package pro.sihao.jarvis.core.domain.model

import java.util.Date

/**
 * Configuration for PipeCat sessions
 */
data class PipeCatConfig(
    val enableMic: Boolean = true,
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
    data class UserTranscript(val text: String, val timestamp: Date = Date()) : PipeCatEvent()
    data class BotResponse(val text: String, val timestamp: Date = Date()) : PipeCatEvent()
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
 * Data for bot ready event
 */
data class BotReadyData(
    val botId: String,
    val capabilities: List<String> = emptyList()
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
    val transportState: TransportState = TransportState.IDLE
)