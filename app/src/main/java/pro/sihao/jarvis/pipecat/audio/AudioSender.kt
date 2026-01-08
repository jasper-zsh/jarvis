package pro.sihao.jarvis.pipecat.audio

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket
import pro.sihao.jarvis.pipecat.AudioFrame
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.encoding.Base64

private const val TAG = "AudioSender"
private const val SAMPLE_RATE = 16000
private const val NUM_CHANNELS = 1

private val JSON_INSTANCE = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Consumer that reads audio data from the buffer and sends it via WebSocket.
 *
 * This component:
 * - Runs in a separate coroutine on Dispatchers.IO
 * - Reads batched audio frames from the AudioBuffer
 * - Encodes audio data as AudioFrame JSON with Base64
 * - Sends via WebSocket when available
 * - Handles WebSocket unavailability gracefully
 *
 * @param buffer The audio buffer to read from
 * @param webSocketProvider A lambda that provides the current WebSocket instance
 */
class AudioSender(
    private val buffer: AudioBuffer,
    private val webSocketProvider: () -> WebSocket?
) {
    private var senderJob: Job? = null
    private val isActive = AtomicBoolean(false)

    /**
     * Start the audio sender consumer.
     *
     * @param scope Coroutine scope to run the consumer in
     */
    fun start(scope: CoroutineScope) {
        if (isActive.get()) {
            Log.d(TAG, "Audio sender already running")
            return
        }

        Log.i(TAG, "Starting audio sender")
        isActive.set(true)

        senderJob = scope.launch(Dispatchers.IO) {
            while (isActive.get()) {
                try {
                    val audioData = buffer.read() ?: continue

                    // Send as AudioFrame JSON (consistent format for both sources)
                    val frame = AudioFrame(
                        audio = Base64.encode(audioData),
                        sampleRate = SAMPLE_RATE,
                        numChannels = NUM_CHANNELS
                    )
                    val jsonData = JSON_INSTANCE.encodeToString(frame)

                    val webSocket = webSocketProvider()
                    if (webSocket != null) {
                        val sent = webSocket.send(jsonData)
                        if (!sent) {
                            Log.w(TAG, "Failed to send audio frame: WebSocket send returned false")
                        }
                    } else {
                        Log.d(TAG, "WebSocket not available, skipping send")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending audio frame", e)
                    if (e is kotlinx.coroutines.CancellationException) {
                        throw e
                    }
                    // Continue loop for other exceptions
                }
            }
            Log.d(TAG, "Audio sender loop ended")
        }
    }

    /**
     * Stop the audio sender consumer.
     */
    fun stop() {
        Log.i(TAG, "Stopping audio sender")
        isActive.set(false)
        senderJob?.cancel()
        senderJob = null
    }
}
