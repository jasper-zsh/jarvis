package pro.sihao.jarvis.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pro.sihao.jarvis.core.domain.model.MediaConstants
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class VoiceRecorder(
    private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var amplitudeJob: Job? = null
    private var durationJob: Job? = null
    private val isRecording = AtomicBoolean(false)
    private var startTime: Long = 0
    private var currentOutputFile: File? = null

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    private val _recordingAmplitude = MutableStateFlow(0f)
    val recordingAmplitude: StateFlow<Float> = _recordingAmplitude.asStateFlow()

    enum class RecordingState {
        IDLE,
        RECORDING,
        PAUSED,
        ERROR
    }

    fun startRecording(outputFile: File): Result<Unit> {
        return try {
            if (isRecording.get()) {
                return Result.failure(Exception("Already recording"))
            }

            if (outputFile.exists()) {
                outputFile.delete()
            }

            currentOutputFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(MediaConstants.VOICE_BIT_RATE)
                setAudioSamplingRate(MediaConstants.VOICE_SAMPLE_RATE)
                setOutputFile(outputFile.absolutePath)

                prepare()
                start()
            }

            isRecording.set(true)
            startTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.RECORDING

            startAmplitudeMonitoring()
            startDurationTimer()

            Result.success(Unit)
        } catch (e: IOException) {
            _recordingState.value = RecordingState.ERROR
            Result.failure(e)
        } catch (e: IllegalStateException) {
            _recordingState.value = RecordingState.ERROR
            Result.failure(e)
        }
    }

    fun stopRecording(): Result<Pair<File, Long>> {
        return try {
            if (!isRecording.get()) {
                return Result.failure(Exception("Not recording"))
            }

            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            isRecording.set(false)
            _recordingState.value = RecordingState.IDLE

            stopAmplitudeMonitoring()
            stopDurationTimer()

            val duration = System.currentTimeMillis() - startTime
            if (duration < 1000) { // Less than 1 second
                return Result.failure(Exception("Recording too short"))
            }

            currentOutputFile?.let { Result.success(Pair(it, duration)) }
                ?: Result.failure(Exception("No recording file"))
        } catch (e: Exception) {
            _recordingState.value = RecordingState.ERROR
            cleanup()
            Result.failure(e)
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording.get()) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null

                isRecording.set(false)
                _recordingState.value = RecordingState.IDLE

                stopAmplitudeMonitoring()
                stopDurationTimer()

                // Delete temporary file
                currentOutputFile?.takeIf { it.exists() }?.delete()
                currentOutputFile = null
            }
        } catch (e: Exception) {
            _recordingState.value = RecordingState.ERROR
            cleanup()
        }
    }

    private fun startAmplitudeMonitoring() {
        amplitudeJob = CoroutineScope(Dispatchers.IO).launch {
            while (isRecording.get()) {
                try {
                    val amplitude = mediaRecorder?.getMaxAmplitude()?.toFloat() ?: 0f
                    val normalizedAmplitude = amplitude / 32767f // Normalize to 0-1 range
                    _recordingAmplitude.value = normalizedAmplitude
                    delay(100) // Update every 100ms
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun stopAmplitudeMonitoring() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _recordingAmplitude.value = 0f
    }

    private fun startDurationTimer() {
        durationJob = CoroutineScope(Dispatchers.Main).launch {
            while (isRecording.get()) {
                val duration = System.currentTimeMillis() - startTime

                // Check if recording exceeds maximum duration
                if (duration >= MediaConstants.MAX_VOICE_DURATION_MS) {
                    stopRecording()
                    break
                }

                _recordingDuration.value = duration
                delay(100)
            }
        }
    }

    private fun stopDurationTimer() {
        durationJob?.cancel()
        durationJob = null
        _recordingDuration.value = 0L
    }

    private fun cleanup() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            // Ignore cleanup errors
        }

        isRecording.set(false)
        _recordingState.value = RecordingState.IDLE
        _recordingDuration.value = 0L
        _recordingAmplitude.value = 0f
        stopAmplitudeMonitoring()
        stopDurationTimer()
        currentOutputFile = null
    }

    fun release() {
        cleanup()
    }
}
