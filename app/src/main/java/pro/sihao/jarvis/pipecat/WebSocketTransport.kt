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
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.rokid.cxr.client.extend.CxrApi
import com.rokid.cxr.client.extend.listeners.AudioStreamListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
) : Transport<WebSocketTransportConnectParams>(), AudioStreamListener {

    companion object {
        private const val TAG = "WebSocketTransport"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 4
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

    // Audio recording for phone mic
    private var audioRecord: AudioRecord? = null
    private var audioRecordJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.IO)
    private var bufferSize: Int = 0

    // Audio playback for received audio
    private var audioTrack: AudioTrack? = null
    private var playbackInitialized = false

    override fun initialize(ctx: TransportContext) {
        transportContext = ctx
        thread = ctx.thread
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR
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
        disconnect().logError(TAG, "Disconnect triggered by release() failed")
        stopAudioRecording()
        stopAudioPlayback()
        audioScope.cancel()
    }

    override fun connect(transportParams: WebSocketTransportConnectParams): Future<Unit, RTVIError> =
        thread.runOnThreadReturningFuture {
            withPromise(thread) { promise ->
                try {
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
                        }

                        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                            Log.i(TAG, "WebSocket closed: $code - $reason")
                            thread.runOnThread {
                                setState(TransportState.Disconnected)
                                transportContext.onConnectionEnd()
                                transportContext.callbacks.onDisconnected()
                            }
                        }

                        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                            Log.e(TAG, "WebSocket error", t)
                            thread.runOnThread {
                                setState(TransportState.Disconnected)
                                promise.resolveErr(RTVIError.OtherError(t.message ?: "WebSocket connection failed"))
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
                stopAudioRecording()
                webSocket?.close(1000, "Client disconnect")
                webSocket = null
                setState(TransportState.Disconnected)
                transportContext.callbacks.onDisconnected()
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

    // AudioStreamListener implementation for glasses mic
    override fun onStartAudioStream(p0: Int, p1: String?) {
        Log.d(TAG, "onStartAudioStream: rate=$p0, id=$p1")
    }

    override fun onAudioStream(data: ByteArray?, offset: Int, length: Int) {
        if (data != null && micEnabled.get() && selectedMicId?.id == "glasses") {
            // Send glasses audio data through WebSocket
            val audioData = if (offset == 0 && length == data.size) {
                data
            } else {
                data.copyOfRange(offset, offset + length)
            }

            // Convert to ByteString and send
            webSocket?.send(ByteString.of(*audioData))
        }
    }

    private fun startAudioRecording() {
        val micId = selectedMicId?.id

        when (micId) {
            "glasses" -> {
                Log.i(TAG, "Starting glasses audio recording")
                // Register this transport as the audio stream listener for glasses
                CxrApi.getInstance().setAudioStreamListener(this)
            }
            "speakerphone" -> {
                Log.i(TAG, "Starting phone microphone recording")
                startPhoneMicRecording()
            }
            else -> {
                Log.w(TAG, "No mic selected, defaulting to speakerphone")
                selectedMicId = MediaDeviceId("speakerphone")
                startPhoneMicRecording()
            }
        }
    }

    private fun stopAudioRecording() {
        Log.i(TAG, "Stopping audio recording")

        // Stop glasses audio
        CxrApi.getInstance().setAudioStreamListener(null)

        // Stop phone mic recording
        audioRecordJob?.cancel()
        audioRecordJob = null
        audioRecord?.release()
        audioRecord = null
    }

    private fun startPhoneMicRecording() {
        try {
            // Check and request audio permissions
            val audioRecordPermission = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            if (audioRecordPermission != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "RECORD_AUDIO permission not granted")
                return
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            audioRecord?.startRecording()

            audioRecordJob = audioScope.launch {
                val buffer = ByteArray(bufferSize)
                val accumulatedAudio = mutableListOf<Byte>()
                val batchSize = (SAMPLE_RATE * 2 * 50) / 1000 // 50ms of audio data (16-bit samples)
                var lastSendTime = System.currentTimeMillis()

                while (micEnabled.get() && audioRecord != null) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Accumulate audio data
                        accumulatedAudio.addAll(buffer.toList().take(read))

                        val currentTime = System.currentTimeMillis()
                        val timeSinceLastSend = currentTime - lastSendTime

                        // Send every 50ms or when accumulated enough data
                        if (accumulatedAudio.size >= batchSize || timeSinceLastSend >= 50) {
                            val audioData = accumulatedAudio.toByteArray()
                            accumulatedAudio.clear()

                            // Send audio data through WebSocket
                            val frame = AudioFrame(
                                Base64.encode(audioData),
                                audioRecord!!.sampleRate,
                                audioRecord!!.channelCount
                            )
                            val data = JSON_INSTANCE.encodeToString(frame)
                            webSocket?.send(data)

                            lastSendTime = currentTime
                        }
                    }
                }
            }

            Log.i(TAG, "Phone microphone recording started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start phone microphone recording", e)
            audioRecord?.release()
            audioRecord = null
        }
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
}
