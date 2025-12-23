package pro.sihao.jarvis.features.realtime.data.service

import android.content.Context
import android.media.AudioManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import pro.sihao.jarvis.core.domain.model.PipeCatConfig
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.core.domain.model.PipeCatEvent
import pro.sihao.jarvis.core.domain.model.TransportState as AppTransportState
import pro.sihao.jarvis.core.domain.service.PipeCatService
import pro.sihao.jarvis.platform.network.webrtc.PipeCatConnectionManager
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PipeCatService using PipeCat SDK
 *
 * This service handles real-time voice communication with automatic Bluetooth audio routing.
 * When glasses or other Bluetooth headsets are connected, it configures Android AudioManager
 * to use Bluetooth SCO for voice-quality audio input/output.
 */
@Singleton
class PipeCatServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PipeCatService {

    companion object {
        private const val TAG = "PipeCatServiceImpl"
    }

    private var pipecatClient: PipecatClient<*, *>? = null
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private var originalAudioMode: Int = AudioManager.MODE_NORMAL
    private var originalSpeakerphoneOn: Boolean = false

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

    // Real-time session management
    private var isSessionActive = false

    /**
     * Check if Bluetooth SCO (Synchronous Connection-Oriented) headset is available
     */
    private fun isBluetoothHeadsetAvailable(): Boolean {
        return try {
            audioManager.isBluetoothScoAvailableOffCall
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Bluetooth SCO availability", e)
            false
        }
    }

    /**
     * Check if any Bluetooth headset is connected (including glasses)
     */
    private fun isBluetoothHeadsetConnected(): Boolean {
        return try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter?.let { adapter ->
                // Check if there are any connected Bluetooth devices that could be headsets
                adapter.bondedDevices.any { device ->
                    // Check for common headset/profile characteristics or if glasses are connected
                    device.bluetoothClass?.let { deviceClass ->
                        val deviceClassValue = deviceClass.deviceClass
                        // Audio headset device class
                        deviceClassValue == 0x2404 ||
                        deviceClassValue == 0x2408 ||
                        deviceClassValue == 0x240C ||
                        device.name?.contains("Glasses", ignoreCase = true) == true
                    } ?: false
                }
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Bluetooth headset connection", e)
            false
        }
    }

    /**
     * Configure audio routing for Bluetooth headset when available
     */
    private fun configureBluetoothAudio() {
        try {
            if (isBluetoothHeadsetConnected() && isBluetoothHeadsetAvailable()) {
                // Save current audio state
                originalAudioMode = audioManager.mode
                originalSpeakerphoneOn = audioManager.isSpeakerphoneOn

                Log.i(TAG, "Configuring Bluetooth SCO audio for voice communication")

                // Start Bluetooth SCO for voice communication
                audioManager.startBluetoothSco()

                // Wait for SCO to establish
                Thread.sleep(1000)

                // Set communication mode
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false // Route through Bluetooth, not speaker

                Log.i(TAG, "Bluetooth SCO audio configured successfully")
            } else {
                Log.w(TAG, "Bluetooth headset not connected or SCO not available, using default audio routing")
                // Still set communication mode for better voice input handling
                originalAudioMode = audioManager.mode
                originalSpeakerphoneOn = audioManager.isSpeakerphoneOn
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring Bluetooth audio", e)
        }
    }

    /**
     * Restore original audio routing
     */
    private fun restoreAudioRouting() {
        try {
            // Stop Bluetooth SCO if it was started
            audioManager.stopBluetoothSco()

            // Restore original audio mode and speaker state
            audioManager.mode = originalAudioMode
            audioManager.isSpeakerphoneOn = originalSpeakerphoneOn

            Log.i(TAG, "Audio routing restored to original state")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring audio routing", e)
        }
    }

    override suspend fun startRealtimeSession(config: PipeCatConfig): Flow<PipeCatEvent> = channelFlow {
        try {
            if (isSessionActive) {
                Log.w(TAG, "Session already active, stopping existing session")
                stopRealtimeSession()
            }

            // Configure Bluetooth audio if available (glasses as headset)
            val bluetoothConnected = isBluetoothHeadsetConnected()
            configureBluetoothAudio()
            Log.i(TAG, "PipeCat session started - ${if (bluetoothConnected) "Bluetooth headset connected, using Bluetooth audio" else "using default audio routing"}")

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
                    trySend(PipeCatEvent.Error("Backend error: $message"))
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
                    trySend(PipeCatEvent.BotReady(
                        pro.sihao.jarvis.core.domain.model.BotReadyData(
                            botId = "jarvis-assistant", // Use default for now
                            capabilities = listOf("voice", "text")
                        )
                    ))
                }

                override fun onMetrics(data: PipecatMetrics) {
                    Log.i(TAG, "Pipecat metrics: $data")
                    // Update connection metrics in state - placeholder for now
                    // TODO: Implement proper audio level extraction from PipecatMetrics
                }

                override fun onBotTranscript(text: String) {
                    Log.i(TAG, "Bot transcript: $text")
                }

                override fun onBotLLMText(data: MsgServerToClient.Data.BotLLMTextData) {
                    Log.i(TAG, "Bot LLM text: $data")
                    CxrApi.getInstance().sendTtsContent(data.text)
                }

                override fun onUserTranscript(data: Transcript) {
                    Log.i(TAG, "User transcript: $data")
                    trySend(PipeCatEvent.UserTranscript(
                        text = data.text,
                        timestamp = Date()
                    ))
                    CxrApi.getInstance().sendAsrContent(data.text)
                }

                override fun onBotStartedSpeaking() {
                    Log.i(TAG, "Bot started speaking")
                    _connectionState.update {
                        it.copy(botIsSpeaking = true)
                    }
                    trySend(PipeCatEvent.BotStartedSpeaking())
                }

                override fun onBotStoppedSpeaking() {
                    Log.i(TAG, "Bot stopped speaking")
                    _connectionState.update {
                        it.copy(botIsSpeaking = false)
                    }
                    trySend(PipeCatEvent.BotStoppedSpeaking())
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
                    _connectionState.update {
                        PipeCatConnectionState()
                    }
                    trySend(PipeCatEvent.Disconnected)
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
                    Log.d(TAG, "CloseWhenNothingToDo invoked")
                    try {
                        // Send event to channelFlow from any context
                        trySend(PipeCatEvent.Bye)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending Bye event", e)
                    }
                    onResult(Value.Object())
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