package pro.sihao.jarvis.pipecat.audio

import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlin.math.min

private const val TAG = "UnifiedAudioBuffer"

/**
 * Thread-safe unified audio buffer implementation using Kotlin Channels.
 *
 * This buffer accumulates incoming audio data and batches it into frames of a specified size.
 * It uses a Channel for thread-safe communication between producers (audio sources) and
 * consumers (audio sender).
 *
 * @param frameSize The target frame size in bytes (default: 2560 bytes for 80ms at 16kHz)
 * @param channelCapacity The maximum number of frames to buffer (default: 10 frames = 800ms)
 */
class UnifiedAudioBuffer(
    private val frameSize: Int = 2560,
    private val channelCapacity: Int = 10
) : AudioBuffer {

    // Channel for batched audio frames
    private val audioChannel = Channel<ByteArray>(capacity = channelCapacity)

    // Accumulated audio data that hasn't reached frame size yet
    private val accumulatedAudio = mutableListOf<Byte>()

    // Track the number of frames in the channel (since Channel doesn't expose size)
    private var channelSize = 0

    // Flag to track if buffer is closed
    private var isClosed = false

    override suspend fun write(audioData: ByteArray, offset: Int, length: Int) {
        if (isClosed) {
            Log.d(TAG, "Buffer is closed, ignoring write of $length bytes")
            return
        }

        // Extract the relevant portion of audio data
        val dataToWrite = if (offset == 0 && length == audioData.size) {
            audioData
        } else {
            audioData.copyOfRange(offset, offset + length)
        }

        synchronized(accumulatedAudio) {
            // Add incoming audio data to accumulation buffer
            accumulatedAudio.addAll(dataToWrite.toList())

            // When we have at least one full frame, extract and send to channel
            while (accumulatedAudio.size >= frameSize) {
                val frameData = accumulatedAudio.take(frameSize).toByteArray()
                accumulatedAudio.subList(0, frameSize).clear()

                // Non-blocking send with backpressure handling
                val result = audioChannel.trySend(frameData)
                if (result.isFailure) {
                    Log.w(TAG, "Audio buffer full, dropping frame. Consider increasing channelCapacity")
                    // Don't block the producer - just drop the frame and continue
                    break
                }
                channelSize++
            }
        }
    }

    override suspend fun read(): ByteArray? {
        // If closed and channel is empty, return null to signal end of stream
        if (isClosed && audioChannel.isEmpty) {
            return null
        }
        val data = audioChannel.receive()
        if (data != null) {
            channelSize--
        }
        return data
    }

    override fun clear() {
        synchronized(accumulatedAudio) {
            accumulatedAudio.clear()
        }
        // Also clear any pending frames in the channel
        while (!audioChannel.isEmpty) {
            audioChannel.tryReceive().getOrNull()
            channelSize--
        }
    }

    override fun size(): Int {
        return channelSize
    }

    override fun close() {
        isClosed = true
        audioChannel.close()
        Log.d(TAG, "Audio buffer closed")
    }
}
