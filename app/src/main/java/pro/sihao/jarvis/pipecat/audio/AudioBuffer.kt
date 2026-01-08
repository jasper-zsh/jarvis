package pro.sihao.jarvis.pipecat.audio

/**
 * Thread-safe audio buffer interface for buffering audio data from multiple sources.
 *
 * Implementations should handle:
 * - Thread-safe writes from multiple audio sources
 * - Batched reads of accumulated audio data
 * - Backpressure handling to prevent memory overflow
 * - Graceful shutdown and cleanup
 */
interface AudioBuffer {
    /**
     * Write audio data to the buffer.
     *
     * @param audioData The audio data to write
     * @param offset Optional offset into the audio data array
     * @param length Optional length of data to write (defaults to full array)
     */
    suspend fun write(audioData: ByteArray, offset: Int = 0, length: Int = audioData.size)

    /**
     * Read a batched audio frame from the buffer.
     * Returns null if the buffer is closed and empty.
     *
     * @return ByteArray containing a batched audio frame, or null if no data available
     */
    suspend fun read(): ByteArray?

    /**
     * Clear all buffered audio data.
     */
    fun clear()

    /**
     * Get the current number of buffered frames.
     *
     * @return The number of frames currently in the buffer
     */
    fun size(): Int

    /**
     * Close the buffer and release all resources.
     * After closing, writes are ignored and reads will return null when empty.
     */
    fun close()
}
