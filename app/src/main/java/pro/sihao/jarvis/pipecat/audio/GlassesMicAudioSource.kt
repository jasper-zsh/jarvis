package pro.sihao.jarvis.pipecat.audio

import android.util.Log
import com.rokid.cxr.client.extend.CxrApi
import com.rokid.cxr.client.extend.listeners.AudioStreamListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "GlassesMicAudioSource"
private const val GLASSES_SCENE_NAME = "AI_assistant"

/**
 * Audio source that captures audio from the glasses microphone via CxrApi.
 *
 * This implementation:
 * - Implements AudioStreamListener to receive audio callbacks from glasses
 * - Writes audio data to the provided AudioBuffer
 * - Manages the glasses audio recording lifecycle
 *
 * @param scope Coroutine scope for writing audio data to buffer
 */
class GlassesMicAudioSource(
    private val scope: CoroutineScope
) : AudioSource, AudioStreamListener {

    override val name: String = "glasses_mic"

    private var currentBuffer: AudioBuffer? = null

    override fun start(buffer: AudioBuffer) {
        Log.i(TAG, "Starting glasses audio recording")
        currentBuffer = buffer

        // Register this transport as the audio stream listener for glasses
        CxrApi.getInstance().setAudioStreamListener(this)
        CxrApi.getInstance().openAudioRecord(1, GLASSES_SCENE_NAME)
    }

    override fun stop() {
        Log.i(TAG, "Stopping glasses audio recording")
        currentBuffer = null
        CxrApi.getInstance().setAudioStreamListener(null)
        CxrApi.getInstance().closeAudioRecord(GLASSES_SCENE_NAME)
    }

    // AudioStreamListener implementation

    override fun onStartAudioStream(codec: Int, id: String?) {
        Log.d(TAG, "onStartAudioStream: codec=$codec, id=$id")
    }

    override fun onAudioStream(data: ByteArray?, offset: Int, length: Int) {
        if (data != null && currentBuffer != null) {
            // Write to unified buffer (non-blocking)
            // Launch a separate coroutine to avoid blocking the callback thread
            scope.launch(Dispatchers.IO) {
                try {
                    currentBuffer?.write(data, offset, length)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write audio data to buffer", e)
                }
            }
        }
    }
}
