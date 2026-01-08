package pro.sihao.jarvis.pipecat.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PhoneMicAudioSource"

private const val SAMPLE_RATE = 16000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
private const val BUFFER_SIZE_FACTOR = 4

/**
 * Audio source that captures audio from the phone's built-in microphone.
 *
 * This implementation:
 * - Uses AudioRecord to capture PCM audio data
 * - Enables acoustic echo cancellation if available
 * - Writes audio data to the provided AudioBuffer
 * - Handles permissions and resource cleanup
 *
 * @param context Android context for permission checking
 * @param scope Coroutine scope for running the recording loop
 */
class PhoneMicAudioSource(
    private val context: Context,
    private val scope: CoroutineScope
) : AudioSource {

    override val name: String = "phone_mic"

    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var recordingJob: Job? = null
    private var currentBuffer: AudioBuffer? = null

    override fun start(buffer: AudioBuffer) {
        Log.i(TAG, "Starting phone microphone recording")
        currentBuffer = buffer

        // Check and request audio permissions
        val audioRecordPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        if (audioRecordPermission != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return
        }

        try {
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            // Enable acoustic echo cancellation
            echoCanceler = AcousticEchoCanceler.create(audioRecord?.audioSessionId ?: 0)
            echoCanceler?.let {
                if (it.enabled) {
                    Log.i(TAG, "AcousticEchoCanceler enabled successfully")
                } else {
                    Log.w(TAG, "AcousticEchoCanceler available but not enabled")
                }
            } ?: Log.w(TAG, "AcousticEchoCanceler not available on this device")

            audioRecord?.startRecording()

            recordingJob = scope.launch(Dispatchers.IO) {
                val readBuffer = ByteArray(bufferSize)

                while (isActive && audioRecord != null) {
                    val read = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                    if (read > 0) {
                        // Write to unified buffer (non-blocking)
                        // Launch a separate coroutine to avoid blocking the recording loop
                        launch(Dispatchers.IO) {
                            try {
                                currentBuffer?.write(readBuffer, 0, read)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to write audio data to buffer", e)
                            }
                        }
                    }
                }
            }

            Log.i(TAG, "Phone microphone recording started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start phone microphone recording", e)
            cleanup()
        }
    }

    override fun stop() {
        Log.i(TAG, "Stopping phone microphone recording")
        recordingJob?.cancel()
        recordingJob = null
        cleanup()
        currentBuffer = null
    }

    private fun cleanup() {
        audioRecord?.release()
        audioRecord = null
        echoCanceler?.release()
        echoCanceler = null
    }
}
