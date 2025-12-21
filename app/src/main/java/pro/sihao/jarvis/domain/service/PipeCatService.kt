package pro.sihao.jarvis.domain.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pro.sihao.jarvis.domain.model.PipeCatConfig
import pro.sihao.jarvis.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.domain.model.PipeCatEvent
import pro.sihao.jarvis.domain.model.Message

/**
 * Service interface for PipeCat functionality - handles all chat modes (text, voice, media)
 */
interface PipeCatService {
    /**
     * Current connection state
     */
    val connectionState: StateFlow<PipeCatConnectionState>

    /**
     * Send a text message for processing
     * @param message The text message to send
     * @param conversationHistory Previous conversation context
     * @return Flow of response events
     */
    suspend fun sendTextMessage(
        message: String,
        conversationHistory: List<Message>
    ): Flow<PipeCatEvent>

    /**
     * Send a media message for processing
     * @param message Text message context
     * @param mediaMessage Media file (voice, photo) to process
     * @param conversationHistory Previous conversation context
     * @return Flow of response events
     */
    suspend fun sendMediaMessage(
        message: String,
        mediaMessage: Message,
        conversationHistory: List<Message>
    ): Flow<PipeCatEvent>

    /**
     * Start a real-time session with PipeCat
     * @param config Configuration for the PipeCat session
     * @return Flow of PipeCat events
     */
    suspend fun startRealtimeSession(config: PipeCatConfig): Flow<PipeCatEvent>

    /**
     * Stop the current real-time session
     */
    suspend fun stopRealtimeSession()

    /**
     * Cancel the active request (text or media)
     */
    fun cancelActiveRequest()

    /**
     * Toggle microphone on/off
     * @param enabled Whether microphone should be enabled
     */
    fun toggleMicrophone(enabled: Boolean)

    /**
     * Toggle camera on/off
     * @param enabled Whether camera should be enabled
     */
    fun toggleCamera(enabled: Boolean)

    /**
     * Get the current connection state
     * @return Current PipeCatConnectionState
     */
    fun getCurrentState(): PipeCatConnectionState

    /**
     * Set custom audio input callback for glasses integration
     * @param audioCallback Function to receive audio data from glasses
     */
    fun setCustomAudioInput(audioCallback: ((ByteArray) -> Unit)?)

    /**
     * Check if using custom audio input (glasses)
     * @return True if using glasses audio input
     */
    fun isUsingCustomAudioInput(): Boolean

    /**
     * Send audio data to PipeCat (from glasses)
     * @param audioData Raw audio data bytes
     */
    fun sendAudioData(audioData: ByteArray)

    /**
     * Set speaker mode for audio output
     * @param enabled Whether to route audio through speaker
     */
    fun setSpeakerMode(enabled: Boolean)
}