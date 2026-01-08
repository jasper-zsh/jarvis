package pro.sihao.jarvis.pipecat.audio

/**
 * Interface for audio sources that can provide audio data to an AudioBuffer.
 *
 * Implementations of this interface encapsulate the logic for capturing audio
 * from different sources (e.g., phone microphone, glasses microphone, Bluetooth devices).
 * They write captured audio data to the provided AudioBuffer, which handles
 * buffering and batching.
 */
interface AudioSource {
    /**
     * Start capturing audio and writing it to the provided buffer.
     *
     * @param buffer The audio buffer to write captured audio data to
     */
    fun start(buffer: AudioBuffer)

    /**
     * Stop capturing audio and release all resources.
     * Should clean up any allocated resources (AudioRecord, listeners, etc.)
     */
    fun stop()

    /**
     * A human-readable name for this audio source, useful for logging.
     */
    val name: String
}
