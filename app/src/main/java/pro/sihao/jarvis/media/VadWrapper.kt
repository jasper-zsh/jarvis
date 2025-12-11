package pro.sihao.jarvis.media

import android.content.Context
import com.konovalov.vad.silero.Vad
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate

/**
 * Small helper around Silero VAD to keep configuration in one place.
 * We use 16 kHz, 20 ms frames (320 samples) and NORMAL mode with short speech/silence durations.
 */
class VadWrapper(
    context: Context,
    sampleRate: SampleRate = SampleRate.SAMPLE_RATE_16K,
    frameSize: FrameSize = FrameSize.FRAME_SIZE_512,
    mode: Mode = Mode.NORMAL,
    silenceDurationMs: Int = 400,
    speechDurationMs: Int = 50
) {

    private val vad: VadSilero = Vad.builder()
        .setContext(context)
        .setSampleRate(sampleRate)
        .setFrameSize(frameSize)
        .setMode(mode)
        .setSilenceDurationMs(silenceDurationMs)
        .setSpeechDurationMs(speechDurationMs)
        .build()

    val frameSizeSamples: Int = vad.frameSize.value
    val sampleRateHz: Int = vad.sampleRate.value

    /**
     * @return true when speech detected for this frame, false otherwise.
     */
    fun isSpeech(frame: ShortArray): Boolean = vad.isSpeech(frame)

    fun close() {
        vad.close()
    }
}
