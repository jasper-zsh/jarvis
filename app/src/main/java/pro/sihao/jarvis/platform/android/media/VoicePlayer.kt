package pro.sihao.jarvis.media

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

class VoicePlayer(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private var currentFile: File? = null

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _playbackDuration = MutableStateFlow(0L)
    val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    enum class PlaybackState {
        IDLE,
        PLAYING,
        PAUSED,
        COMPLETED,
        ERROR
    }

    fun play(audioFile: File): Result<Unit> {
        return try {
            if (audioFile.exists().not()) {
                return Result.failure(Exception("Audio file does not exist"))
            }

            currentFile = audioFile
            _playbackState.value = PlaybackState.IDLE

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener {
                    start()
                    _playbackDuration.value = duration.toLong()
                    _playbackState.value = PlaybackState.PLAYING
                    startProgressMonitoring()
                }

                setOnCompletionListener {
                    _playbackState.value = PlaybackState.COMPLETED
                    _playbackPosition.value = 0L
                    stopProgressMonitoring()
                }

                setOnErrorListener { _, _, _ ->
                    _playbackState.value = PlaybackState.ERROR
                    stopProgressMonitoring()
                    true
                }

                prepareAsync()
            }

            Result.success(Unit)
        } catch (e: IOException) {
            _playbackState.value = PlaybackState.ERROR
            Result.failure(e)
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            Result.failure(e)
        }
    }

    fun pause(): Result<Unit> {
        return try {
            if (_playbackState.value == PlaybackState.PLAYING) {
                mediaPlayer?.pause()
                _playbackState.value = PlaybackState.PAUSED
                stopProgressMonitoring()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Not currently playing"))
            }
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            Result.failure(e)
        }
    }

    fun resume(): Result<Unit> {
        return try {
            if (_playbackState.value == PlaybackState.PAUSED) {
                mediaPlayer?.start()
                _playbackState.value = PlaybackState.PLAYING
                startProgressMonitoring()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Not currently paused"))
            }
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            Result.failure(e)
        }
    }

    fun stop(): Result<Unit> {
        return try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
            mediaPlayer = null

            _playbackState.value = PlaybackState.IDLE
            _playbackPosition.value = 0L
            _playbackDuration.value = 0L
            stopProgressMonitoring()

            Result.success(Unit)
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            Result.failure(e)
        }
    }

    fun seekTo(position: Long): Result<Unit> {
        return try {
            mediaPlayer?.let { player ->
                if (_playbackState.value != PlaybackState.IDLE) {
                    player.seekTo(position.toInt())
                    _playbackPosition.value = position
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("No audio loaded"))
                }
            } ?: Result.failure(Exception("MediaPlayer not initialized"))
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            Result.failure(e)
        }
    }

    private fun startProgressMonitoring() {
        playbackJob = CoroutineScope(Dispatchers.Main).launch {
            while (_playbackState.value == PlaybackState.PLAYING) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playbackPosition.value = player.currentPosition.toLong()
                    }
                } ?: break

                delay(100) // Update every 100ms
            }
        }
    }

    private fun stopProgressMonitoring() {
        playbackJob?.cancel()
        playbackJob = null
    }

    fun getCurrentFile(): File? = currentFile

    fun isPlaying(): Boolean = _playbackState.value == PlaybackState.PLAYING

    fun isPaused(): Boolean = _playbackState.value == PlaybackState.PAUSED

    fun release() {
        stop()
        currentFile = null
    }
}