package pro.sihao.jarvis.features.realtime.data.service

import android.content.Context
import android.util.Log
import ai.pipecat.client.PipecatClient
import ai.pipecat.client.PipecatClientOptions
import ai.pipecat.client.PipecatEventCallbacks
import ai.pipecat.client.result.Future
import ai.pipecat.client.result.RTVIError
import ai.pipecat.client.small_webrtc_transport.SmallWebRTCTransport
import ai.pipecat.client.transport.MsgServerToClient
import ai.pipecat.client.types.APIRequest
import ai.pipecat.client.types.BotOutputData
import ai.pipecat.client.types.BotReadyData
import ai.pipecat.client.types.LLMFunctionCallData
import ai.pipecat.client.types.LLMFunctionCallHandler
import ai.pipecat.client.types.Participant
import ai.pipecat.client.types.PipecatMetrics
import ai.pipecat.client.types.Tracks
import ai.pipecat.client.types.Transcript
import ai.pipecat.client.types.TransportState as PipecatTransportState
import ai.pipecat.client.types.Value
import com.rokid.cxr.client.extend.CxrApi
import com.rokid.cxr.client.extend.callbacks.PhotoResultCallback
import com.rokid.cxr.client.utils.ValueUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pro.sihao.jarvis.core.domain.model.PipeCatConfig
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.core.domain.model.PipeCatEvent
import pro.sihao.jarvis.core.domain.model.TransportState as AppTransportState
import pro.sihao.jarvis.core.domain.service.PipeCatService
import pro.sihao.jarvis.platform.network.webrtc.PipeCatConnectionManager
import java.util.Date
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

/**
 * Implementation of PipeCatService using PipeCat SDK
 *
 * This service handles real-time voice communication with CxrApi device management.
 * Uses CxrApi.setCommunicationDevice() and CxrApi.clearCommunicationDevice()
 * for audio device routing during pipecat sessions.
 */
@Singleton
class PipeCatServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PipeCatService {

    companion object {
        private const val TAG = "PipeCatServiceImpl"
    }

    private var pipecatClient: PipecatClient<*, *>? = null

    // Extension function for error handling
    private fun <E> Future<E, RTVIError>.displayErrors() = withErrorCallback { error ->
        Log.e(TAG, "PipeCat operation failed: ${error.description}", error.exception)
        _connectionState.update {
            it.copy(
                errorMessage = error.description,
                transportState = AppTransportState.ERROR,
                isConnecting = false
            )
        }
    }
    private val _connectionState = MutableStateFlow(PipeCatConnectionState())
    override val connectionState: StateFlow<PipeCatConnectionState> = _connectionState.asStateFlow()

    // Event bus for transcript and other events
    private val _eventFlow = MutableSharedFlow<PipeCatEvent>(replay = 50)
    override val eventFlow: SharedFlow<PipeCatEvent> = _eventFlow.asSharedFlow()

    // Real-time session management
    private var isSessionActive = false

    /**
     * Configure audio routing for communication device using CxrApi
     */
    private fun configureBluetoothAudio() {
        try {
            Log.i(TAG, "Setting communication device for pipecat session")
            CxrApi.getInstance().setCommunicationDevice()
            Log.i(TAG, "Communication device set successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting communication device", e)
        }
    }

    /**
     * Restore audio routing by clearing communication device
     */
    private fun restoreAudioRouting() {
        try {
            Log.i(TAG, "Clearing communication device")
            CxrApi.getInstance().clearCommunicationDevice()
            Log.i(TAG, "Communication device cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing communication device", e)
        }
    }

    override suspend fun startRealtimeSession(config: PipeCatConfig): Flow<PipeCatEvent> = channelFlow {
        try {
            if (isSessionActive) {
                Log.w(TAG, "Session already active, stopping existing session")
                stopRealtimeSession()
            }

            // Configure communication device
            configureBluetoothAudio()
            Log.i(TAG, "PipeCat session started with CxrApi communication device")

            // Update connection state to connecting
            _connectionState.update {
                it.copy(
                    isConnecting = true,
                    isConnected = false,
                    config = config,
                    errorMessage = null,
                    transportState = AppTransportState.CONNECTING
                )
            }

            // Create PipeCat event callbacks
            val callbacks = object : PipecatEventCallbacks() {
                override fun onTransportStateChanged(state: PipecatTransportState) {
                    Log.i(TAG, "Transport state changed: $state")
                    _connectionState.update { current ->
                        current.copy(
                            transportState = when (state.name) {
                                "IDLE", "Initializing", "Initialized" -> AppTransportState.IDLE
                                "Authorizing", "Authorized", "Connecting" -> AppTransportState.CONNECTING
                                "Connected" -> AppTransportState.CONNECTED
                                "Disconnecting" -> AppTransportState.DISCONNECTING
                                "ERROR" -> AppTransportState.ERROR
                                else -> AppTransportState.IDLE
                            },
                            isConnected = state.name == "Connected",
                            isConnecting = state.name in listOf("Authorizing", "Authorized", "Connecting")
                        )
                    }
                }

                override fun onBackendError(message: String) {
                    Log.e(TAG, "Backend error: $message")
                    _connectionState.update {
                        it.copy(errorMessage = "Backend error: $message")
                    }
                    val event = PipeCatEvent.Error("Backend error: $message")
                    trySend(event)
                    _eventFlow.tryEmit(event)
                }

                override fun onBotReady(data: BotReadyData) {
                    Log.i(TAG, "Bot ready: $data")
                    _connectionState.update {
                        it.copy(
                            botReady = true,
                            isConnecting = false,
                            isConnected = true
                        )
                    }
                    val event = PipeCatEvent.BotReady(
                        pro.sihao.jarvis.core.domain.model.BotReadyData(
                            botId = "jarvis-assistant", // Use default for now
                            capabilities = listOf("voice", "text")
                        )
                    )
                    trySend(event)
                    _eventFlow.tryEmit(event)
                }

                override fun onMetrics(data: PipecatMetrics) {
                    Log.i(TAG, "Pipecat metrics: $data")
                    // Update connection metrics in state - placeholder for now
                    // TODO: Implement proper audio level extraction from PipecatMetrics
                }

                override fun onBotTranscript(text: String) {
                    Log.i(TAG, "Bot transcript: $text")
                    val event = PipeCatEvent.BotResponse(text = text, timestamp = Date())
                    trySend(event)
                    _eventFlow.tryEmit(event)
                }

                override fun onBotLLMText(data: MsgServerToClient.Data.BotLLMTextData) {
                    Log.i(TAG, "Bot LLM text: $data")
                    val event = PipeCatEvent.BotLLMText(text = data.text, timestamp = Date())
                    trySend(event)
                    _eventFlow.tryEmit(event)
                    CxrApi.getInstance().sendTtsContent(data.text)
                }

                override fun onUserTranscript(data: Transcript) {
                    Log.i(TAG, "User transcript: $data")
                    val event = PipeCatEvent.UserTranscript(
                        text = data.text,
                        timestamp = Date(),
                        isFinal = data.final
                    )
                    trySend(event)
                    _eventFlow.tryEmit(event)
                    CxrApi.getInstance().sendAsrContent(data.text)
                }

                override fun onBotStartedSpeaking() {
                    Log.i(TAG, "Bot started speaking")
                    _connectionState.update {
                        it.copy(botIsSpeaking = true)
                    }
                    val event = PipeCatEvent.BotStartedSpeaking()
                    trySend(event)
                    _eventFlow.tryEmit(event)
                }

                override fun onBotStoppedSpeaking() {
                    Log.i(TAG, "Bot stopped speaking")
                    _connectionState.update {
                        it.copy(botIsSpeaking = false)
                    }
                    val event = PipeCatEvent.BotStoppedSpeaking()
                    trySend(event)
                    _eventFlow.tryEmit(event)
                }

                override fun onUserStartedSpeaking() {
                    Log.i(TAG, "User started speaking")
                    _connectionState.update {
                        it.copy(userIsSpeaking = true)
                    }
                }

                override fun onUserStoppedSpeaking() {
                    Log.i(TAG, "User stopped speaking")
                    _connectionState.update {
                        it.copy(userIsSpeaking = false)
                    }
                }

                override fun onTracksUpdated(tracks: Tracks) {
                    Log.i(TAG, "Tracks updated: $tracks")
                    // Handle track updates
                }

                override fun onInputsUpdated(camera: Boolean, mic: Boolean) {
                    Log.i(TAG, "Inputs updated - Camera: $camera, Mic: $mic")
                    // Update input states if needed
                }

                override fun onDisconnected() {
                    Log.i(TAG, "Disconnected")
                    isSessionActive = false

                    // Restore audio routing by clearing communication device
                    restoreAudioRouting()

                    _connectionState.update {
                        PipeCatConnectionState()
                    }

                    // Notify glasses that session has ended
                    try {
                        CxrApi.getInstance().sendExitEvent()
                        Log.d(TAG, "Sent exit event to glasses")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending exit event to glasses", e)
                    }

                    trySend(PipeCatEvent.Disconnected)
                    _eventFlow.tryEmit(PipeCatEvent.Disconnected)
                }

                override fun onUserAudioLevel(level: Float) {
                    _connectionState.update { current ->
                        current.copy(userAudioLevel = level)
                    }
                }

                override fun onRemoteAudioLevel(level: Float, participant: Participant) {
                    _connectionState.update { current ->
                        current.copy(botAudioLevel = level)
                    }
                }
            }

            // Create PipeCat client options
            val options = PipecatClientOptions(
                enableMic = config.enableMic,
                enableCam = config.enableCam,
                callbacks = callbacks
            )

            // Initialize PipeCat client with SmallWebRTC transport on main thread
            pipecatClient = withContext(Dispatchers.Main) {
                PipecatClient(
                    transport = SmallWebRTCTransport(context),
                    options = options
                )
            }

            pipecatClient?.registerFunctionCallHandler("CloseWhenNothingToDo", object : LLMFunctionCallHandler {
                override fun handleFunctionCall(
                    data: LLMFunctionCallData,
                    onResult: (Value) -> Unit
                ) {
                    Log.d(TAG, "CloseWhenNothingToDo invoked - triggering disconnect")
                    try {
                        // Trigger disconnect - this will call onDisconnected() which sends exit event
                        CoroutineScope(Dispatchers.Main).launch {
                            pipecatClient?.disconnect()?.displayErrors()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error triggering disconnect", e)
                    }
                    onResult(Value.Object())
                }
            })
            pipecatClient?.registerFunctionCallHandler("SetBrightness", object : LLMFunctionCallHandler {
                override fun handleFunctionCall(
                    data: LLMFunctionCallData,
                    onResult: (Value) -> Unit
                ) {
                    Log.d(TAG, "SetBrightness invoked $data")
                    val v = data.args.jsonObject.get("value")?.jsonPrimitive?.intOrNull
                    if (v != null) {
                        CxrApi.getInstance().setGlassBrightness(v)
                    } else {
                        Log.w(TAG, "SetBrightness wrong args")
                    }
                }
            })
            pipecatClient?.registerFunctionCallHandler("TakePhoto", object : LLMFunctionCallHandler {
                override fun handleFunctionCall(
                    data: LLMFunctionCallData,
                    onResult: (Value) -> Unit
                ) {
                    val result = CxrApi.getInstance().takeGlassPhoto(640, 480, 80, object : PhotoResultCallback {
                        override fun onPhotoResult(
                            p0: ValueUtil.CxrStatus?,
                            p1: ByteArray?
                        ) {
                            if (p1 != null) {
                                val picUuid = UUID.randomUUID().toString()
                                val encoded = Base64.encode(p1)
                                try {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        pipecatClient?.sendClientRequest(
                                            "pic-result", Value.Object(
                                                Pair("uuid", Value.Str(picUuid)),
                                                Pair("data", Value.Str(encoded))
                                            )
                                        )?.await()
                                        onResult(Value.Str("Photo save as uuid $picUuid"))
                                        Log.i(TAG, "Took photo and sent to bot successfully")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to send photo to bot $e")
                                    onResult(Value.Str("Photo has been taken, but failed to send: $e"))
                                }
                            } else {
                                Log.e(TAG, "Failed to take photo: $p0")
                                onResult(Value.Str("Failed to take photo: $p0"))
                            }
                        }

                    })
                    if (result != ValueUtil.CxrStatus.REQUEST_SUCCEED) {
                        onResult(Value.Str("Failed to requeat take photo: $result"))
                    }
                }

            })

            // Build API request headers
            val headers = buildMap {
                config.apiKey?.takeIf { it.isNotEmpty() }?.let {
                    put("Authorization", "Bearer $it")
                }
                config.customHeaders.forEach { (key, value) ->
                    put(key, value)
                }
            }

            // Start bot and connect
            val apiRequest = APIRequest(
                endpoint = config.baseUrl,
                requestData = Value.Object(),
                headers = headers
            )


            pipecatClient?.startBotAndConnect(apiRequest)?.displayErrors()?.withErrorCallback {
                // Session ended or disconnected
                isSessionActive = false

                // Restore audio routing in case of error
                try {
                    restoreAudioRouting()
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring audio routing during error callback", e)
                }
            }


            isSessionActive = true

            // Send initial connection event
            send(PipeCatEvent.TransportStateChanged(AppTransportState.CONNECTED))

            // Keep the channel flow running while session is active
            while (isSessionActive) {
                kotlinx.coroutines.delay(100) // Keep alive
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting PipeCat session", e)
            isSessionActive = false

            // Restore audio routing in case of error
            try {
                restoreAudioRouting()
            } catch (restoreError: Exception) {
                Log.e(TAG, "Error restoring audio routing during exception handling", restoreError)
            }

            _connectionState.update {
                it.copy(
                    errorMessage = e.message ?: "Unknown error",
                    transportState = AppTransportState.ERROR,
                    isConnecting = false
                )
            }
            send(PipeCatEvent.Error(e.message ?: "Unknown error", e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun stopRealtimeSession() {
        try {
            isSessionActive = false

            _connectionState.update {
                it.copy(
                    isConnecting = false,
                    isConnected = false,
                    botReady = false,
                    transportState = AppTransportState.DISCONNECTING
                )
            }

            // Disconnect the PipeCat client on main thread
            withContext(Dispatchers.Main) {
                pipecatClient?.disconnect()?.displayErrors()
                pipecatClient?.release()
                pipecatClient = null
            }

            // Restore original audio routing
            restoreAudioRouting()
            Log.i(TAG, "PipeCat session ended - audio routing restored")

            _connectionState.update {
                PipeCatConnectionState()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping PipeCat session", e)
            _connectionState.update {
                it.copy(errorMessage = e.message ?: "Error stopping session")
            }
        }
    }

    override fun toggleMicrophone(enabled: Boolean) {
        try {
            // Run SDK calls on main thread
            CoroutineScope(Dispatchers.Main).launch {
                pipecatClient?.enableMic(enabled)?.displayErrors()
            }
            _connectionState.update {
                it.copy(config = it.config?.copy(enableMic = enabled))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling microphone", e)
            _connectionState.update {
                it.copy(errorMessage = e.message ?: "Microphone error")
            }
        }
    }

    override fun toggleCamera(enabled: Boolean) {
        try {
            // Run SDK calls on main thread
            CoroutineScope(Dispatchers.Main).launch {
                pipecatClient?.enableCam(enabled)?.displayErrors()
            }
            _connectionState.update {
                it.copy(config = it.config?.copy(enableCam = enabled))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling camera", e)
            _connectionState.update {
                it.copy(errorMessage = e.message ?: "Camera error")
            }
        }
    }
}