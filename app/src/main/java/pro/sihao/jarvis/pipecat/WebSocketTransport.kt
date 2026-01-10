package pro.sihao.jarvis.pipecat

import ai.pipecat.client.result.Future
import ai.pipecat.client.result.RTVIError
import ai.pipecat.client.result.resolvedPromiseErr
import ai.pipecat.client.result.resolvedPromiseOk
import ai.pipecat.client.result.withPromise
import ai.pipecat.client.transport.MsgClientToServer
import ai.pipecat.client.transport.MsgServerToClient
import ai.pipecat.client.transport.Transport
import ai.pipecat.client.transport.TransportContext
import ai.pipecat.client.types.APIRequest
import ai.pipecat.client.types.MediaDeviceId
import ai.pipecat.client.types.MediaDeviceInfo
import ai.pipecat.client.types.Participant
import ai.pipecat.client.types.ParticipantId
import ai.pipecat.client.types.ParticipantTracks
import ai.pipecat.client.types.Tracks
import ai.pipecat.client.types.TransportState
import ai.pipecat.client.utils.ThreadRef
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import pro.sihao.jarvis.core.domain.model.GlassesConnectionStatus
import pro.sihao.jarvis.platform.android.connection.GlassesConnectionManager
import pro.sihao.jarvis.pipecat.audio.AudioBuffer
import pro.sihao.jarvis.pipecat.audio.AudioSender
import pro.sihao.jarvis.pipecat.audio.AudioSource
import pro.sihao.jarvis.pipecat.audio.GlassesMicAudioSource
import pro.sihao.jarvis.pipecat.audio.PhoneMicAudioSource
import pro.sihao.jarvis.pipecat.audio.UnifiedAudioBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.encoding.Base64

internal val JSON_INSTANCE = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class WebSocketTransportConnectParams(
    val wsUrl: String
)

private val BOT_PARTICIPANT = Participant(
    id = ParticipantId("bot"),
    name = null,
    local = false
)

private val LOCAL_PARTICIPANT = Participant(
    id = ParticipantId("local"),
    name = null,
    local = true
)

class WebSocketTransport(
    private val context: Context,
    private val glassesConnectionManager: GlassesConnectionManager
) : Transport<WebSocketTransportConnectParams>() {

    companion object {
        private const val TAG = "WebSocketTransport"
        private const val SAMPLE_RATE = 16000
        private val EMPTY_TRACKS = Tracks(
            local = ParticipantTracks(audio = null, video = null),
            bot = null
        )
    }

    object AudioDevices {
        val Glasses = MediaDeviceInfo(
            id = MediaDeviceId("glasses"),
            name = "Glasses"
        )

        val Speakerphone = MediaDeviceInfo(
            id = MediaDeviceId("speakerphone"),
            name = "Speakerphone"
        )
    }

    private lateinit var transportContext: TransportContext
    private lateinit var thread: ThreadRef

    private var state = TransportState.Disconnected
    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient()

    // Mic selection
    private var selectedMicId: MediaDeviceId? = null
    private var micEnabled = AtomicBoolean(false)
    private val isReleased = AtomicBoolean(false)
    private val cleanupMutex = Mutex()
    private var notifiedDisconnected = false

    // Unified audio system
    private lateinit var audioBuffer: AudioBuffer
    private lateinit var audioSender: AudioSender
    private val audioSources = mapOf(
        "glasses" to GlassesMicAudioSource(CoroutineScope(Dispatchers.IO)),
        "speakerphone" to PhoneMicAudioSource(context, CoroutineScope(Dispatchers.IO))
    )
    private var currentAudioSource: AudioSource? = null
    private val audioScope = CoroutineScope(Dispatchers.IO)

    // Audio playback for received audio
    private var audioTrack: AudioTrack? = null
    private var playbackInitialized = false

    override fun initialize(ctx: TransportContext) {
        transportContext = ctx
        thread = ctx.thread

        // Initialize unified audio buffer (2560 bytes = 80ms at 16kHz)
        audioBuffer = UnifiedAudioBuffer(
            frameSize = (SAMPLE_RATE * 2 * 80) / 1000, // 2560 bytes
            channelCapacity = 10 // Buffer up to 10 frames (800ms)
        )

        // Initialize audio sender with WebSocket provider
        audioSender = AudioSender(audioBuffer) { webSocket }
    }

    override fun deserializeConnectParams(
        json: String,
        startBotRequest: APIRequest
    ): WebSocketTransportConnectParams {
        val params = JSON_INSTANCE.decodeFromString<WebSocketTransportConnectParams>(json)
        return params
    }

    override fun initDevices(): Future<Unit, RTVIError> = resolvedPromiseOk(thread, Unit)

    override fun release() {
        Log.i(TAG, "release() called")
        performDisconnect(triggeredBy = "release")
        performResourceRelease()
    }

    override fun connect(transportParams: WebSocketTransportConnectParams): Future<Unit, RTVIError> =
        thread.runOnThreadReturningFuture {
            withPromise(thread) { promise ->
                try {
                    // Guard against connecting after release
                    if (isReleased.get()) {
                        Log.w(TAG, "Cannot connect: transport already released")
                        promise.resolveErr(RTVIError.OtherError("Transport released, cannot reconnect"))
                        return@withPromise
                    }

                    // Reset disconnected notification flag for new connection
                    notifiedDisconnected = false

                    Log.i(TAG, "Connecting to WebSocket: ${transportParams.wsUrl}")
                    setState(TransportState.Connecting)

                    val request = Request.Builder()
                        .url(transportParams.wsUrl)
                        .build()

                    webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                        override fun onOpen(ws: WebSocket, response: Response) {
                            Log.i(TAG, "WebSocket connected")
                            thread.runOnThread {
                                setState(TransportState.Connected)
                                sendMessage(MsgClientToServer.ClientReady(
                                    rtviVersion = transportContext.protocolVersion,
                                    library = "Jarvis",
                                    libraryVersion = "0.1.0",
                                    platform = "Android",
                                    platformVersion = Build.VERSION.RELEASE
                                ))
                                val cb = transportContext.callbacks
                                cb.onConnected()
                                cb.onParticipantJoined(LOCAL_PARTICIPANT)
                                cb.onParticipantJoined(BOT_PARTICIPANT)

                                // Enable mic if configured in options
                                if (transportContext.options.enableMic) {
                                    enableMic(true).logError(TAG, "Failed to enable mic")
                                }

                                promise.resolveOk(Unit)
                            }
                        }

                        override fun onMessage(ws: WebSocket, text: String) {
                            Log.d(TAG, "Received message: $text")
                            // Handle server messages
                            thread.runOnThread {
                                try {
                                    val root = JSON_INSTANCE.decodeFromString<JsonElement>(text)
                                    val msgWithType = JSON_INSTANCE.decodeFromJsonElement<MessageWithType>(root)
                                    when (msgWithType.type) {
                                        "audio" -> {
                                            val frame = JSON_INSTANCE.decodeFromJsonElement<AudioFrame>(root)
                                            val audioData = Base64.decode(frame.audio)
                                            // Play received audio samples
                                            playAudioFrame(audioData, frame.sampleRate, frame.numChannels)
                                        }
                                        "message" -> {
                                            val frame = JSON_INSTANCE.decodeFromJsonElement<MessageFrame>(root)
                                            val msg = JSON_INSTANCE.decodeFromJsonElement<MsgServerToClient>(frame.message)
                                            transportContext.onMessage(msg)
                                        }
                                        else -> {
                                            Log.i(TAG, "Received other frame ${msgWithType.frame}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to parse message", e)
                                }
                            }
                        }

                        override fun onMessage(ws: WebSocket, bytes: ByteString) {
                            Log.d(TAG, "Received binary message: ${bytes.size} bytes")
                            // Handle binary messages (audio data)
                        }

                        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                            Log.i(TAG, "WebSocket closing: $code - $reason")

                            // Trigger graceful disconnect of Pipecat client
                            // This allows the SDK to send goodbye messages and clean up internal state
                            thread.runOnThread {
                                try {
                                    setState(TransportState.Disconnected)

                                    // Notify transport context that connection is ending
                                    transportContext.onConnectionEnd()

                                    // Stop audio recording first (don't send more data)
                                    stopAudioRecording()

                                    // IMPORTANT: Notify callbacks immediately in onClosing
                                    // onClosed may not be called if SDK cleans up resources first
                                    // This ensures UI updates and auto-reconnect triggers
                                    notifyDisconnected()

                                    Log.d(TAG, "onClosing: Pipecat disconnect initiated and callbacks notified")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error in onClosing handler", e)
                                }
                            }
                        }

                        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                            Log.i(TAG, "WebSocket closed: $code - $reason")

                            thread.runOnThread {
                                try {
                                    setState(TransportState.Disconnected)

                                    // WebSocket is fully closed, now safe to release all resources
                                    performResourceRelease()

                                    // Notify callbacks after cleanup (only once)
                                    notifyDisconnected()

                                    Log.d(TAG, "onClosed: Resource release complete")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error in onClosed handler", e)
                                }
                            }
                        }

                        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                            Log.e(TAG, "WebSocket error", t)
                            thread.runOnThread {
                                setState(TransportState.Disconnected)

                                // On failure, also release resources to prevent leaks
                                performResourceRelease()

                                // Reject connection promise
                                promise.resolveErr(RTVIError.OtherError(t.message ?: "WebSocket connection failed"))

                                // Notify callbacks of disconnection (only once)
                                notifyDisconnected()
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect", e)
                    setState(TransportState.Disconnected)
                    promise.resolveErr(RTVIError.OtherError(e.message ?: "Connection failed"))
                }
            }
        }

    override fun disconnect(): Future<Unit, RTVIError> = thread.runOnThreadReturningFuture {
        withPromise(thread) { promise ->
            try {
                Log.i(TAG, "Disconnecting WebSocket")

                // Stop audio recording immediately
                stopAudioRecording()

                // Close websocket gracefully (triggers onClosed → resource release)
                webSocket?.close(1000, "Client disconnect")
                webSocket = null

                setState(TransportState.Disconnected)

                // Notify callbacks immediately so UI updates right away
                // onClosed will also try to notify, but notifyDisconnected() is idempotent
                notifyDisconnected()

                // Note: We don't call performResourceRelease() here
                // Resource release happens in onClosed or via explicit release() call

                promise.resolveOk(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect", e)
                promise.resolveErr(RTVIError.OtherError(e.message ?: "Disconnect failed"))
            }
        }
    }

    override fun getAllMics(): Future<List<MediaDeviceInfo>, RTVIError> {
        val availableMics = mutableListOf(AudioDevices.Speakerphone)

        // Only add glasses as an option if connected
        val connectionState = glassesConnectionManager.connectionState.value
        if (connectionState.connectionStatus == GlassesConnectionStatus.CONNECTED) {
            availableMics.add(0, AudioDevices.Glasses)
        }

        return resolvedPromiseOk(thread, availableMics)
    }

    override fun getAllCams(): Future<List<MediaDeviceInfo>, RTVIError> =
        resolvedPromiseOk(thread, emptyList())

    override fun updateMic(micId: MediaDeviceId): Future<Unit, RTVIError> =
        thread.runOnThreadReturningFuture {
            try {
                Log.i(TAG, "Updating mic to: ${micId.id}")

                // Stop current audio recording
                stopAudioRecording()

                selectedMicId = micId

                // Start new audio recording based on selection
                if (micEnabled.get()) {
                    startAudioRecording()
                }

                resolvedPromiseOk(thread, Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update mic", e)
                resolvedPromiseErr(thread, RTVIError.OtherError(e.message ?: "Failed to update mic"))
            }
        }

    override fun updateCam(camId: MediaDeviceId): Future<Unit, RTVIError> =
        resolvedPromiseOk(thread, Unit)

    override fun selectedMic(): MediaDeviceInfo? {
        return when (selectedMicId?.id) {
            "glasses" -> AudioDevices.Glasses
            "speakerphone" -> AudioDevices.Speakerphone
            else -> null
        }
    }

    override fun selectedCam(): MediaDeviceInfo? = null

    override fun enableMic(enable: Boolean): Future<Unit, RTVIError> =
        thread.runOnThreadReturningFuture {
            try {
                Log.i(TAG, "Enable mic: $enable")
                micEnabled.set(enable)

                if (enable) {
                    startAudioRecording()
                } else {
                    stopAudioRecording()
                }

                resolvedPromiseOk(thread, Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable mic", e)
                resolvedPromiseErr(thread, RTVIError.OtherError(e.message ?: "Failed to enable mic"))
            }
        }

    override fun enableCam(enable: Boolean): Future<Unit, RTVIError> =
        resolvedPromiseErr(thread, RTVIError.OtherError("Cam not supported yet"))

    override fun isCamEnabled(): Boolean = false

    override fun isMicEnabled(): Boolean = micEnabled.get()

    override fun sendMessage(message: MsgClientToServer): Future<Unit, RTVIError> {
        return try {
            val msg = JSON_INSTANCE.encodeToJsonElement(MsgClientToServer.serializer(), message)
            val frame = MessageFrame(msg)
            val json = JSON_INSTANCE.encodeToString(frame)
            Log.d(TAG, "Sending message: $json")

            val sent = webSocket?.send(json) ?: false
            if (sent) {
                resolvedPromiseOk(thread, Unit)
            } else {
                resolvedPromiseErr(thread, RTVIError.OtherError("Failed to send message: WebSocket not connected"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            resolvedPromiseErr(thread, RTVIError.OtherError(e.message ?: "Failed to send message"))
        }
    }

    override fun state(): TransportState = state

    override fun setState(state: TransportState) {
        Log.i(TAG, "setState($state)")
        thread.assertCurrent()
        this.state = state
        transportContext.callbacks.onTransportStateChanged(state)
    }

    override fun tracks(): Tracks {
        // WebSocket transport doesn't have WebRTC tracks
        return EMPTY_TRACKS
    }

    private fun startAudioRecording() {
        var micId = selectedMicId?.id

        // Auto-detect: if no mic selected, check if glasses are connected
        if (micId == null) {
            val connectionState = glassesConnectionManager.connectionState.value
            val shouldUseGlasses = connectionState.connectionStatus == GlassesConnectionStatus.CONNECTED

            if (shouldUseGlasses) {
                Log.i(TAG, "No mic selected, auto-detected glasses, using glasses mic")
                selectedMicId = MediaDeviceId("glasses")
                micId = "glasses"
            } else {
                Log.i(TAG, "No mic selected and glasses not connected, using speakerphone")
                selectedMicId = MediaDeviceId("speakerphone")
                micId = "speakerphone"
            }
        }

        // Stop current audio source if any
        currentAudioSource?.stop()

        // Start audio sender consumer
        audioSender.start(audioScope)

        // Start selected audio source
        val audioSource = audioSources[micId]
        if (audioSource != null) {
            Log.i(TAG, "Starting audio source: $micId")
            currentAudioSource = audioSource
            audioSource.start(audioBuffer)
        } else {
            Log.e(TAG, "Unknown audio source: $micId")
        }
    }

    private fun stopAudioRecording() {
        Log.i(TAG, "Stopping audio recording")

        // Stop current audio source
        currentAudioSource?.stop()
        currentAudioSource = null

        // Stop audio sender
        audioSender.stop()

        // Clear audio buffer
        audioBuffer.clear()
    }

    private fun initAudioPlayback(sampleRate: Int, channels: Int) {
        if (playbackInitialized && audioTrack != null) {
            return
        }

        try {
            val channelConfig = if (channels == 2) {
                AudioFormat.CHANNEL_OUT_STEREO
            } else {
                AudioFormat.CHANNEL_OUT_MONO
            }

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            playbackInitialized = true
            Log.i(TAG, "Audio playback initialized: sampleRate=$sampleRate, channels=$channels")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio playback", e)
        }
    }

    private fun playAudioFrame(audioData: ByteArray, sampleRate: Int, channels: Int) {
        try {
            // Initialize audio playback if not already done
            initAudioPlayback(sampleRate, channels)

            audioTrack?.write(audioData, 0, audioData.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio frame", e)
        }
    }

    private fun stopAudioPlayback() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            playbackInitialized = false
            Log.i(TAG, "Audio playback stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop audio playback", e)
        }
    }

    private fun performDisconnect(triggeredBy: String) {
        if (isReleased.get()) {
            Log.d(TAG, "Already released, skipping disconnect (triggered by: $triggeredBy)")
            return
        }

        // Try to disconnect gracefully, but don't fail if already disconnected
        try {
            disconnect().logError(TAG, "Disconnect triggered by $triggeredBy failed")
        } catch (e: Exception) {
            Log.w(TAG, "Disconnect already completed or failed, continuing with release", e)
        }
    }

    private fun notifyDisconnected() {
        if (!notifiedDisconnected) {
            Log.i(TAG, "Notifying callbacks of disconnection - notifying transportContext.callbacks.onDisconnected()")
            notifiedDisconnected = true
            try {
                transportContext.callbacks.onDisconnected()
                Log.i(TAG, "Successfully notified callbacks of disconnection")
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying callbacks of disconnection", e)
            }
        } else {
            Log.d(TAG, "Already notified callbacks of disconnection, skipping")
        }
    }

    private fun performResourceRelease() {
        // Use mutex to prevent concurrent cleanup
        if (!cleanupMutex.tryLock()) {
            Log.d(TAG, "Cleanup already in progress, skipping")
            return
        }

        try {
            if (!isReleased.compareAndSet(false, true)) {
                Log.d(TAG, "Resources already released")
                return
            }

            Log.i(TAG, "Releasing all resources")
            releaseAllResources()
        } finally {
            cleanupMutex.unlock()
        }
    }

    private fun releaseAllResources() {
        // Release each resource category independently with error handling
        releaseAudioRecording()
        releaseAudioPlayback()
        releaseAudioComponents()
    }

    private fun releaseAudioRecording() {
        try {
            stopAudioRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recording", e)
        }
    }

    private fun releaseAudioPlayback() {
        try {
            stopAudioPlayback()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio playback", e)
        }
    }

    private fun releaseAudioComponents() {
        try {
            currentAudioSource?.stop()
            currentAudioSource = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio source", e)
        }

        try {
            audioSender.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio sender", e)
        }

        try {
            audioBuffer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing audio buffer", e)
        }

        try {
            audioScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling audio scope", e)
        }
    }
}
