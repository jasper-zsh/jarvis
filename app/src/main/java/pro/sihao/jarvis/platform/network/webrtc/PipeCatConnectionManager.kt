package pro.sihao.jarvis.platform.network.webrtc

import android.content.Context
import android.util.Log
import com.rokid.cxr.client.extend.CxrApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pro.sihao.jarvis.core.domain.model.ContentType
import pro.sihao.jarvis.core.domain.model.Message
import pro.sihao.jarvis.core.domain.model.PipeCatConfig
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.core.domain.model.PipeCatEvent
import pro.sihao.jarvis.core.domain.repository.MessageRepository
import pro.sihao.jarvis.core.domain.service.PipeCatService
import pro.sihao.jarvis.features.realtime.data.config.ConfigurationManager
import pro.sihao.jarvis.platform.android.service.PipeCatForegroundService
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified manager for PipeCat connections and message persistence
 *
 * NOTE: This class is now primarily a bridge for message persistence.
 * Most functionality has been moved to PipeCatService and PipeCatForegroundService
 * to reduce code duplication and simplify the architecture.
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

    // Expose the service connection state directly
    val connectionState: StateFlow<PipeCatConnectionState> = pipeCatService.connectionState
  
    /**
     * Connect to PipeCat with the given configuration
     */
    suspend fun connect(config: PipeCatConfig) {
        pipeCatService.startRealtimeSession(config).collect { event ->
            handlePipeCatEvent(event)
        }
    }

    /**
     * Disconnect from PipeCat
     */
    suspend fun disconnect() {
        pipeCatService.stopRealtimeSession()
    }

    /**
     * Handle PipeCat events - simplified to focus on message persistence only
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

            is PipeCatEvent.BotReady -> {
                try {
                    CxrApi.getInstance().sendAsrContent("")
                    Log.d(TAG, "Bot ready - 向glasses发送ASR内容")
                } catch (e: Exception) {
                    Log.e(TAG, "向glasses发送ASR内容失败", e)
                }
            }

            is PipeCatEvent.Bye -> {
                CxrApi.getInstance().sendExitEvent()
            }

            is PipeCatEvent.BotStartedSpeaking,
            is PipeCatEvent.BotStoppedSpeaking,
            is PipeCatEvent.AudioLevelChanged,
            is PipeCatEvent.Error,
            is PipeCatEvent.Disconnected,
            is PipeCatEvent.TransportStateChanged,
            is PipeCatEvent.TextResponseStarted,
            is PipeCatEvent.TextResponsePartial,
            is PipeCatEvent.TextResponseComplete,
            is PipeCatEvent.MediaProcessingStarted,
            is PipeCatEvent.MediaTranscriptionComplete,
            is PipeCatEvent.MediaAnalysisComplete,
            is PipeCatEvent.RequestCanceled -> {
                // These events are now handled by the service itself
                // No need to duplicate handling here
                Log.d(TAG, "PipeCatEvent: $event")
            }
        }
    }
}