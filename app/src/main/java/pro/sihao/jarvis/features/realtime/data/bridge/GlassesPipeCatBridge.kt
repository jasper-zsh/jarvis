package pro.sihao.jarvis.features.realtime.data.bridge

import android.content.Context
import android.util.Log
import com.rokid.cxr.client.extend.CxrApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pro.sihao.jarvis.platform.android.connection.GlassesConnectionManager
import pro.sihao.jarvis.platform.android.audio.AudioRoutingManager
import pro.sihao.jarvis.platform.network.webrtc.PipeCatConnectionManager
import pro.sihao.jarvis.core.domain.model.GlassesConnectionStatus
import pro.sihao.jarvis.core.domain.repository.MessageRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge between glasses and PipeCat functionality
 */
@Singleton
class GlassesPipeCatBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val glassesConnectionManager: GlassesConnectionManager,
    private val pipeCatConnectionManager: PipeCatConnectionManager,
    private val messageRepository: MessageRepository,
    private val audioRoutingManager: AudioRoutingManager
) {
    companion object {
        private const val TAG = "GlassesPipeCatBridge"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var glassesObservationJob: Job? = null
    private var pipeCatObservationJob: Job? = null

    init {
        observeGlassesConnection()
        observePipeCatEvents()
    }

    /**
     * Observe glasses connection state and manage audio routing
     */
    private fun observeGlassesConnection() {
        glassesObservationJob?.cancel()
        glassesObservationJob = scope.launch {
            glassesConnectionManager.connectionState.collect { glassesState ->
                when (glassesState.connectionStatus) {
                    GlassesConnectionStatus.CONNECTED -> {
                        Log.i(TAG, "Glasses connected, setting up audio integration")
                        setupGlassesAudioInput()
                    }
                    GlassesConnectionStatus.DISCONNECTED -> {
                        Log.i(TAG, "Glasses disconnected, falling back to phone microphone")
                        // Fallback to phone microphone
                        pipeCatConnectionManager.toggleMicrophone(true)
                    }
                    GlassesConnectionStatus.CONNECTING -> {
                        Log.d(TAG, "Glasses connecting...")
                    }
                    GlassesConnectionStatus.ERROR -> {
                        Log.e(TAG, "Glasses connection error: ${glassesState.errorMessage}")
                        // Fallback to phone microphone on error
                        pipeCatConnectionManager.toggleMicrophone(true)
                    }
                }
            }
        }
    }

    /**
     * Observe PipeCat events and handle TTS output to glasses
     */
    private fun observePipeCatEvents() {
        pipeCatObservationJob?.cancel()
        pipeCatObservationJob = scope.launch {
            pipeCatConnectionManager.connectionState.collect { pipeCatState ->
                when {
                    pipeCatState.botIsSpeaking -> {
                        Log.d(TAG, "Bot is speaking, preparing TTS for glasses")
                        // TTS will be handled when bot response is received
                        // The audio will automatically play through glasses TTS
                    }

                    pipeCatState.isConnected && !pipeCatState.botReady -> {
                        Log.d(TAG, "PipeCat connected, setting up glasses audio")
                        // When PipeCat connects and bot is ready, ensure glasses audio is active
                        if (isGlassesConnected()) {
                            setupGlassesAudioInput()
                        }
                    }

                    !pipeCatState.isConnected -> {
                        Log.d(TAG, "PipeCat disconnected, stopping glasses audio routing")
                        // Stop audio routing when PipeCat disconnects
                        audioRoutingManager.stopAudioRouting()
                    }
                }
            }
        }
    }

    /**
     * Setup glasses audio input for PipeCat
     */
    private fun setupGlassesAudioInput() {
        try {
            Log.i(TAG, "Setting up glasses audio input with real PipeCat integration")

            // Setup audio routing from glasses to PipeCat
            val success = audioRoutingManager.startAudioRouting { audioData ->
                // Send audio data to PipeCat when received from glasses
                pipeCatConnectionManager.sendAudioData(audioData)
            }

            if (success) {
                // Disable phone microphone since we're using glasses
                pipeCatConnectionManager.toggleMicrophone(false)

                // Enable custom audio input in PipeCat service
                // This will be handled by the connection manager

                Log.i(TAG, "Glasses audio routing successfully established")
            } else {
                Log.e(TAG, "Failed to start glasses audio routing")
                // Fallback to phone microphone
                pipeCatConnectionManager.toggleMicrophone(true)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup glasses audio input", e)
            // Fallback to phone microphone
            pipeCatConnectionManager.toggleMicrophone(true)
        }
    }

    /**
     * Send TTS content to glasses
     */
    private suspend fun sendTtsToGlasses(content: String) {
        try {
            val glassesState = glassesConnectionManager.connectionState.firstOrNull()
            if (glassesState?.connectionStatus == GlassesConnectionStatus.CONNECTED) {
                Log.d(TAG, "Sending TTS to glasses: $content")
                CxrApi.getInstance().sendTtsContent(content)
            } else {
                Log.w(TAG, "Glasses not connected, skipping TTS")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send TTS to glasses", e)
        }
    }

    /**
     * Send latest bot response to glasses TTS
     */
    suspend fun sendLatestBotResponseToGlasses() {
        try {
            val messages = messageRepository.getRecentMessages(10).firstOrNull() ?: emptyList()
            val latestBotMessage = messages.lastOrNull { !it.isFromUser }
            
            if (latestBotMessage != null) {
                sendTtsToGlasses(latestBotMessage.content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send latest bot response to glasses", e)
        }
    }

    /**
     * Check if glasses are connected and ready
     */
    fun isGlassesConnected(): Boolean {
        return glassesConnectionManager.connectionState.value.connectionStatus == GlassesConnectionStatus.CONNECTED
    }

    /**
     * Enable or disable glasses integration
     */
    fun setGlassesIntegrationEnabled(enabled: Boolean) {
        if (enabled && isGlassesConnected()) {
            setupGlassesAudioInput()
        } else {
            // Disable glasses audio and fallback to phone
            audioRoutingManager.stopAudioRouting()
            pipeCatConnectionManager.toggleMicrophone(true)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        glassesObservationJob?.cancel()
        pipeCatObservationJob?.cancel()

        // Stop audio routing
        audioRoutingManager.stopAudioRouting()

        try {
            CxrApi.getInstance().closeAudioRecord(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up glasses audio", e)
        }
    }
    
    /**
     * Switch to glasses mode for PipeCat
     */
    fun switchToGlassesMode() {
        if (isGlassesConnected()) {
            setupGlassesAudioInput()
            Log.i(TAG, "Switched to glasses mode")
        } else {
            throw IllegalStateException("Glasses not connected")
        }
    }
    
    /**
     * Flow of glasses connection state for UI observation
     */
    val glassesConnected = glassesConnectionManager.connectionState.map {
        it.connectionStatus == GlassesConnectionStatus.CONNECTED
    }

    /**
     * Get current audio routing status
     */
    fun getAudioRoutingStatus() = audioRoutingManager.getRoutingStatus()

    /**
     * Observe audio routing status
     */
    fun observeAudioRoutingStatus() = audioRoutingManager.isRoutingActive

    /**
     * Observe audio level from glasses microphone
     */
    fun observeAudioLevel() = audioRoutingManager.audioLevel
}