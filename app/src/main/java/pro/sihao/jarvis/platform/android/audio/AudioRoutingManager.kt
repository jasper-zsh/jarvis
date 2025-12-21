package pro.sihao.jarvis.platform.android.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.rokid.cxr.client.extend.CxrApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio routing between glasses microphone and PipeCat
 */
@Singleton
class AudioRoutingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioRoutingManager"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 4
        private const val GLASSES_AUDIO_BUFFER_SIZE = 1024 * 4 // 4KB buffer
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var audioRoutingJob: Job? = null
    private var glassesAudioRecord: AudioRecord? = null
    private var pipeCatAudioTrack: AudioTrack? = null
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val _isRoutingActive = MutableStateFlow(false)
    val isRoutingActive: StateFlow<Boolean> = _isRoutingActive.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    /**
     * Setup glasses audio input for PipeCat
     */
    fun setupGlassesAudioInput(): Boolean {
        return try {
            Log.i(TAG, "Setting up glasses audio input for PipeCat")

            // Initialize glasses audio
            CxrApi.getInstance().openAudioRecord(1, null)

            // Create AudioRecord for glasses microphone
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ) * BUFFER_SIZE_FACTOR

            glassesAudioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (glassesAudioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize glasses AudioRecord")
                return false
            }

            _isRoutingActive.value = true
            Log.i(TAG, "Glasses audio input setup successful")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up glasses audio input", e)
            false
        }
    }

    /**
     * Start routing glasses audio to PipeCat
     * This reads from glasses microphone and provides audio data to PipeCat
     */
    fun startAudioRouting(onAudioData: (ByteArray) -> Unit): Boolean {
        return try {
            if (_isRoutingActive.value) {
                Log.w(TAG, "Audio routing already active")
                return true
            }

            if (!setupGlassesAudioInput()) {
                Log.e(TAG, "Failed to setup glasses audio input")
                return false
            }

            audioRoutingJob = scope.launch {
                val audioRecord = glassesAudioRecord ?: return@launch
                val buffer = ByteArray(GLASSES_AUDIO_BUFFER_SIZE)

                audioRecord.startRecording()
                Log.i(TAG, "Started audio routing from glasses to PipeCat")

                while (isActive && _isRoutingActive.value) {
                    val bytesRead = audioRecord.read(buffer, 0, buffer.size)

                    if (bytesRead > 0) {
                        // Calculate audio level for visualization
                        calculateAudioLevel(buffer, bytesRead)

                        // Send audio data to PipeCat
                        val audioChunk = buffer.copyOf(bytesRead)
                        onAudioData(audioChunk)

                        Log.v(TAG, "Routed $bytesRead bytes from glasses to PipeCat")
                    } else {
                        Log.w(TAG, "No audio data read from glasses")
                        kotlinx.coroutines.delay(10) // Small delay to prevent busy waiting
                    }
                }
            }

            true

        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio routing", e)
            stopAudioRouting()
            false
        }
    }

    /**
     * Stop audio routing
     */
    fun stopAudioRouting() {
        try {
            Log.i(TAG, "Stopping audio routing")

            _isRoutingActive.value = false
            audioRoutingJob?.cancel()
            audioRoutingJob = null

            glassesAudioRecord?.let { record ->
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    record.stop()
                    record.release()
                }
            }
            glassesAudioRecord = null

            pipeCatAudioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.stop()
                    track.release()
                }
            }
            pipeCatAudioTrack = null

            // Close glasses audio
            CxrApi.getInstance().closeAudioRecord(null)

            _audioLevel.value = 0f
            Log.i(TAG, "Audio routing stopped")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio routing", e)
        }
    }

    /**
     * Calculate audio level from PCM data
     */
    private fun calculateAudioLevel(buffer: ByteArray, bytesRead: Int) {
        try {
            var sum = 0.0
            val samples = bytesRead / 2 // 16-bit samples = 2 bytes each

            for (i in 0 until bytesRead step 2) {
                val sample = ((buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)).toShort()
                sum += sample * sample
            }

            val rms = kotlin.math.sqrt(sum / samples)
            val normalizedLevel = (rms / 32767.0).toFloat()

            // Apply logarithmic scaling for better visualization
            val scaledLevel = if (normalizedLevel > 0) {
                (20 * kotlin.math.log10(normalizedLevel.coerceAtLeast(0.001f))).coerceIn(0f, 1f)
            } else {
                0f
            }

            _audioLevel.value = scaledLevel

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating audio level", e)
        }
    }

    /**
     * Set audio output to speaker
     */
    fun setSpeakerMode(enabled: Boolean) {
        try {
            if (enabled) {
                // Force audio to speaker
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = true

                // Set stream type for voice communication
                audioManager.setStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                    0
                )

                Log.i(TAG, "Speaker mode enabled")
            } else {
                // Reset to normal mode
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.isSpeakerphoneOn = false

                Log.i(TAG, "Speaker mode disabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speaker mode", e)
        }
    }

    /**
     * Initialize audio track for speaker output
     */
    private fun initializeSpeakerAudioTrack(): AudioTrack? {
        return try {
            val bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AUDIO_FORMAT
            ) * BUFFER_SIZE_FACTOR

            val audioTrack = AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AUDIO_FORMAT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )

            if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize speaker AudioTrack")
                audioTrack.release()
                null
            } else {
                Log.i(TAG, "Speaker AudioTrack initialized successfully")
                audioTrack
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing speaker AudioTrack", e)
            null
        }
    }

    /**
     * Play audio data through speaker
     */
    fun playAudioThroughSpeaker(audioData: ByteArray) {
        try {
            var audioTrack = pipeCatAudioTrack

            if (audioTrack == null || audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                audioTrack = initializeSpeakerAudioTrack()
                pipeCatAudioTrack = audioTrack
            }

            audioTrack?.let { track ->
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                }

                val bytesWritten = track.write(audioData, 0, audioData.size)
                if (bytesWritten > 0) {
                    Log.v(TAG, "Played $bytesWritten bytes through speaker")
                } else {
                    Log.w(TAG, "Failed to write audio data to speaker")
                }
            } ?: Log.e(TAG, "AudioTrack not available for speaker output")

        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio through speaker", e)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        stopAudioRouting()

        // Clean up speaker audio track
        pipeCatAudioTrack?.let { track ->
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.stop()
                track.release()
            }
        }
        pipeCatAudioTrack = null

        // Reset audio mode
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting audio mode", e)
        }
    }

    /**
     * Get current audio routing status
     */
    fun getRoutingStatus(): AudioRoutingStatus {
        return AudioRoutingStatus(
            isActive = _isRoutingActive.value,
            audioLevel = _audioLevel.value,
            isGlassesConnected = glassesAudioRecord?.state == AudioRecord.STATE_INITIALIZED
        )
    }
}

/**
 * Audio routing status information
 */
data class AudioRoutingStatus(
    val isActive: Boolean,
    val audioLevel: Float,
    val isGlassesConnected: Boolean
)