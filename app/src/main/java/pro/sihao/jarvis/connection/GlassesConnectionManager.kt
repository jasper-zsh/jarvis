package pro.sihao.jarvis.connection

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.rokid.cxr.client.extend.CxrApi
import com.rokid.cxr.client.extend.callbacks.BluetoothStatusCallback
import com.rokid.cxr.client.utils.ValueUtil
import com.rokid.cxr.client.extend.listeners.AudioStreamListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date
import pro.sihao.jarvis.data.storage.GlassesPreferences
import pro.sihao.jarvis.data.storage.MediaStorageManager
import pro.sihao.jarvis.domain.model.GlassesConnectionStatus
import pro.sihao.jarvis.domain.model.RokidGlassesDevice
import pro.sihao.jarvis.domain.model.ContentType
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.media.VadWrapper
import pro.sihao.jarvis.domain.repository.MessageRepository
import pro.sihao.jarvis.domain.service.LLMService
import kotlinx.coroutines.flow.first

/**
 * Headless manager that keeps glasses connection alive in the background and auto-reconnects
 * to persisted devices when possible.
 */
@Singleton
class GlassesConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: GlassesPreferences,
    private val mediaStorageManager: MediaStorageManager,
    private val messageRepository: MessageRepository,
    private val llmService: LLMService
) {

    companion object {
        val REQUIRED_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        private val ROKID_SERVICE_UUID: ParcelUuid =
            ParcelUuid.fromString("00009100-0000-1000-8000-00805f9b34fb")
        private const val SCAN_DURATION_MS = 8_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow(
        GlassesConnectionState(
            persistedDevice = preferences.loadDevice()
        )
    )
    val connectionState: StateFlow<GlassesConnectionState> = _connectionState

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanner: BluetoothLeScanner? = null
    private var scanJob: Job? = null
    private var connectionTimeoutJob: Job? = null
    private var reconnectJob: Job? = null
    private var currentAudioJob: Job? = null
    private var vad: VadWrapper? = null
    private var audioBuffer = mutableListOf<Short>()
    private var isSpeechActive = false
    private var utteranceStartTs: Long = 0L
    private var streamCodecType: Int = 1
    private var streamType: String? = null
    private val discoveredDevices = mutableMapOf<String, BluetoothDevice>()
    private val bondedDevices = mutableMapOf<String, BluetoothDevice>()
    private var currentCallback: BluetoothStatusCallback? = null

    init {
        refreshPersistedDevice()
        updateBluetoothState()
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device ?: return
            val name = device.name ?: device.address ?: return
            discoveredDevices[device.address] = device
            _connectionState.update {
                it.copy(
                    discoveredDevices = discoveredDevices.values.map { d -> d.toUiModel() }
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _connectionState.update {
                it.copy(
                    isScanning = false,
                    errorMessage = "Scan failed: $errorCode"
                )
            }
        }
    }

    fun ensureAutoReconnect() {
        updateBluetoothState()
        refreshPersistedDevice()
        val persisted = _connectionState.value.persistedDevice
        if (_connectionState.value.connectionStatus == GlassesConnectionStatus.CONNECTED) return
        if (_connectionState.value.connectionStatus == GlassesConnectionStatus.CONNECTING) return
        if (persisted?.socketUuid.isNullOrEmpty() || persisted?.macAddress.isNullOrEmpty()) return
        if (!hasPermissions()) return
        if (bluetoothAdapter?.isEnabled != true) return
        connectWithIdentifiers(
            socketUuid = persisted!!.socketUuid!!,
            macAddress = persisted.macAddress!!,
            name = persisted.name,
            rokidAccount = persisted.rokidAccount,
            glassesType = persisted.glassesType
        )
    }

    fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun refreshPersistedDevice() {
        val persisted = preferences.loadDevice()
        _connectionState.update { it.copy(persistedDevice = persisted) }
    }

    fun updateBluetoothState() {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = manager?.adapter
        scanner = bluetoothAdapter?.bluetoothLeScanner
        _connectionState.update { state ->
            state.copy(isBluetoothEnabled = bluetoothAdapter?.isEnabled == true)
        }
    }

    // Audio stream listener for Rokid glasses
    private val audioStreamListener = object : AudioStreamListener {
        override fun onStartAudioStream(codecType: Int, streamType: String?) {
            streamCodecType = codecType
            this@GlassesConnectionManager.streamType = streamType
            if (codecType != 1) {
                // Fallback to PCM if unsupported codec announced
                CxrApi.getInstance().closeAudioRecord(streamType)
                CxrApi.getInstance().openAudioRecord(1, streamType)
                streamCodecType = 1
            }
            resetAudioBuffer()
        }

        override fun onAudioStream(data: ByteArray?, offset: Int, length: Int) {
            if (data == null || streamCodecType != 1) return // handle only PCM
            val vadInstance = vad ?: return
            // Convert little-endian 16-bit PCM to short samples
            val buf = ByteBuffer.wrap(data, offset, length).order(ByteOrder.LITTLE_ENDIAN)
            val samples = ShortArray(length / 2)
            buf.asShortBuffer().get(samples)

            // Feed per frame according to VAD frame size
            var idx = 0
            while (idx + vadInstance.frameSizeSamples <= samples.size) {
                val frame = samples.copyOfRange(idx, idx + vadInstance.frameSizeSamples)
                idx += vadInstance.frameSizeSamples
                val speech = vadInstance.isSpeech(frame)
                if (speech) {
                    isSpeechActive = true
                    if (utteranceStartTs == 0L) utteranceStartTs = System.currentTimeMillis()
                    audioBuffer.addAll(frame.toList())
                } else if (isSpeechActive) {
                    // silence after speech -> end utterance
                    audioBuffer.addAll(frame.toList())
                    finalizeUtterance()
                } else {
                    // ignore leading silence
                }
            }
        }
    }

    private fun resetAudioBuffer() {
        audioBuffer.clear()
        isSpeechActive = false
        utteranceStartTs = 0L
    }

    private fun finalizeUtterance() {
        val vadInstance = vad ?: return
        val samples = audioBuffer.toShortArray()
        resetAudioBuffer()
        if (samples.isEmpty()) return
        currentAudioJob?.cancel()
        currentAudioJob = scope.launch {
            try {
                val file = mediaStorageManager.createVoiceFile("wav")
                writeWavFile(file, samples, vadInstance.sampleRateHz)
                val durationMs = samples.size * 1000L / vadInstance.sampleRateHz
                val voiceMessage = Message(
                    content = "Glasses voice command",
                    timestamp = Date(),
                    isFromUser = true,
                    contentType = ContentType.VOICE,
                    mediaUrl = file.absolutePath,
                    duration = durationMs,
                    mediaSize = file.length()
                )
                messageRepository.insertMessage(voiceMessage)
                val history = messageRepository.getRecentMessages(50).firstOrNull() ?: emptyList()
                llmService.sendMessage(
                    message = voiceMessage.content,
                    conversationHistory = history + voiceMessage,
                    mediaMessage = voiceMessage
                ).collect { event ->
                    when (event) {
                        is pro.sihao.jarvis.domain.service.LLMStreamEvent.Partial -> {
                            // ignore partials for now
                        }
                        is pro.sihao.jarvis.domain.service.LLMStreamEvent.Complete -> {
                            val aiMessage = Message(
                                content = event.content,
                                timestamp = Date(),
                                isFromUser = false
                            )
                            messageRepository.insertMessage(aiMessage)
                            if (_connectionState.value.connectionStatus == GlassesConnectionStatus.CONNECTED) {
                                runCatching { CxrApi.getInstance().sendTtsContent(event.content) }
                            }
                        }
                        is pro.sihao.jarvis.domain.service.LLMStreamEvent.Error -> {
                            _connectionState.update {
                                it.copy(errorMessage = "Voice handling failed: ${event.throwable.message}")
                            }
                        }
                        is pro.sihao.jarvis.domain.service.LLMStreamEvent.Canceled -> {
                            // no-op
                        }
                    }
                }
            } catch (e: Exception) {
                _connectionState.update { it.copy(errorMessage = "Failed to process glasses audio: ${e.message}") }
            }
        }
    }

    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        // Simple PCM 16-bit mono WAV writer
        val byteBuffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { byteBuffer.putShort(it) }
        val data = byteBuffer.array()
        val totalDataLen = 36 + data.size
        val byteRate = sampleRate * 2

        FileOutputStream(file).use { out ->
            out.write(byteArrayOf('R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte()))
            out.write(intToLittleEndian(totalDataLen))
            out.write(byteArrayOf('W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte()))
            out.write(byteArrayOf('f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte()))
            out.write(intToLittleEndian(16)) // Subchunk1Size for PCM
            out.write(shortToLittleEndian(1)) // AudioFormat PCM
            out.write(shortToLittleEndian(1)) // NumChannels mono
            out.write(intToLittleEndian(sampleRate))
            out.write(intToLittleEndian(byteRate))
            out.write(shortToLittleEndian(2)) // Block align
            out.write(shortToLittleEndian(16)) // Bits per sample
            out.write(byteArrayOf('d'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte()))
            out.write(intToLittleEndian(data.size))
            out.write(data)
        }
    }

    private fun intToLittleEndian(value: Int): ByteArray =
        byteArrayOf(
            (value and 0xff).toByte(),
            (value shr 8 and 0xff).toByte(),
            (value shr 16 and 0xff).toByte(),
            (value shr 24 and 0xff).toByte()
        )

    private fun shortToLittleEndian(value: Int): ByteArray =
        byteArrayOf(
            (value and 0xff).toByte(),
            (value shr 8 and 0xff).toByte()
        )

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasPermissions()) return
        updateBluetoothState()
        val adapter = bluetoothAdapter ?: return
        val leScanner = scanner ?: return
        if (!adapter.isEnabled) return
        val persistedAddress = _connectionState.value.persistedDevice?.macAddress

        discoveredDevices.clear()
        bondedDevices.clear()
        getConnectedDevices(persistedAddress).forEach { device -> bondedDevices[device.address] = device }
        adapter.bondedDevices?.forEach { device ->
            val include = device.name?.contains("Glasses", ignoreCase = true) == true ||
                (persistedAddress != null && device.address == persistedAddress)
            if (include) {
                bondedDevices[device.address] = device
            }
        }

        _connectionState.update {
            it.copy(
                isScanning = true,
                discoveredDevices = emptyList(),
                bondedDevices = bondedDevices.values.map { device -> device.toUiModel() },
                errorMessage = null
            )
        }

        leScanner.startScan(
            listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ROKID_SERVICE_UUID)
                    .build()
            ),
            ScanSettings.Builder().build(),
            scanCallback
        )

        scanJob?.cancel()
        scanJob = scope.launch {
            delay(SCAN_DURATION_MS)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanner?.stopScan(scanCallback)
        _connectionState.update { it.copy(isScanning = false) }
        scanJob?.cancel()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        if (!hasPermissions()) {
            _connectionState.update { it.copy(errorMessage = "Bluetooth permissions required before connecting") }
            return
        }
        val device = discoveredDevices[address] ?: bondedDevices[address]
        if (device == null) {
            _connectionState.update { it.copy(errorMessage = "Device not found for connection") }
            return
        }

        val persisted = _connectionState.value.persistedDevice
        if (persisted?.socketUuid?.isNotEmpty() == true && persisted.macAddress == address) {
            _connectionState.update {
                it.copy(
                    connectionStatus = GlassesConnectionStatus.CONNECTING,
                    selectedDevice = it.selectedDevice ?: persisted,
                    errorMessage = null
                )
            }
            if (_connectionState.value.connectionStatus == GlassesConnectionStatus.CONNECTED &&
                _connectionState.value.connectedDevice?.macAddress == address
            ) {
                return
            }
            connectWithIdentifiers(
                socketUuid = persisted.socketUuid,
                macAddress = persisted.macAddress ?: address,
                name = persisted.name,
                rokidAccount = persisted.rokidAccount,
                glassesType = persisted.glassesType
            )
            return
        }

        _connectionState.update {
            it.copy(
                connectionStatus = GlassesConnectionStatus.CONNECTING,
                selectedDevice = device.toUiModel(),
                errorMessage = null
            )
        }

        currentCallback = object : BluetoothStatusCallback {
            override fun onConnectionInfo(
                socketUuid: String?,
                macAddress: String?,
                rokidAccount: String?,
                glassesType: Int
            ) {
                if (socketUuid.isNullOrEmpty() || macAddress.isNullOrEmpty()) {
                    _connectionState.update {
                        it.copy(
                            connectionStatus = GlassesConnectionStatus.ERROR,
                            errorMessage = "Connection info missing"
                        )
                    }
                    return
                }
                connectWithIdentifiers(
                    socketUuid = socketUuid,
                    macAddress = macAddress,
                    name = device.name ?: macAddress,
                    rokidAccount = rokidAccount,
                    glassesType = glassesType
                )
            }

            override fun onConnected() {
                connectionTimeoutJob?.cancel()
                _connectionState.update {
                    it.copy(
                        connectionStatus = GlassesConnectionStatus.CONNECTED,
                        connectedDevice = it.selectedDevice ?: it.connectedDevice,
                        errorMessage = null
                    )
                }
            }

            override fun onDisconnected() {
                connectionTimeoutJob?.cancel()
                _connectionState.update {
                    it.copy(
                        connectionStatus = GlassesConnectionStatus.DISCONNECTED
                    )
                }
                scheduleReconnect()
            }

            override fun onFailed(errorCode: ValueUtil.CxrBluetoothErrorCode?) {
                connectionTimeoutJob?.cancel()
                _connectionState.update {
                    it.copy(
                        connectionStatus = GlassesConnectionStatus.ERROR,
                        errorMessage = "Bluetooth connect failed: ${errorCode?.name ?: "Unknown"}"
                    )
                }
                scheduleReconnect()
            }
        }
        CxrApi.getInstance().initBluetooth(context, device, currentCallback)
        startConnectionTimeout()
    }

    @SuppressLint("MissingPermission")
    private fun connectWithIdentifiers(
        socketUuid: String,
        macAddress: String,
        name: String,
        rokidAccount: String?,
        glassesType: Int?
    ) {
        // Ensure VAD is ready when connected
        if (vad == null) {
            vad = VadWrapper(context)
        }
        _connectionState.update {
            it.copy(
                connectionStatus = GlassesConnectionStatus.CONNECTING,
                selectedDevice = it.selectedDevice ?: it.connectedDevice ?: RokidGlassesDevice(
                    name = name,
                    macAddress = macAddress,
                    socketUuid = socketUuid,
                    rokidAccount = rokidAccount,
                    glassesType = glassesType
                ),
                errorMessage = null
            )
        }
        currentCallback = object : BluetoothStatusCallback {
            override fun onConnectionInfo(
                socketUuid: String?,
                macAddress: String?,
                rokidAccount: String?,
                glassesType: Int
            ) {
                val updatedDevice = RokidGlassesDevice(
                    name = name,
                    macAddress = macAddress,
                    socketUuid = socketUuid ?: _connectionState.value.connectedDevice?.socketUuid,
                    rokidAccount = rokidAccount,
                    glassesType = glassesType
                )
                _connectionState.update { state ->
                    state.copy(
                        connectedDevice = updatedDevice,
                        selectedDevice = updatedDevice
                    )
                }
                preferences.saveDevice(updatedDevice)
            }

            override fun onConnected() {
                connectionTimeoutJob?.cancel()
                val current = _connectionState.value.connectedDevice ?: RokidGlassesDevice(
                    name = name,
                    macAddress = macAddress,
                    socketUuid = socketUuid,
                    rokidAccount = rokidAccount,
                    glassesType = glassesType
                )
                preferences.saveDevice(current)
                _connectionState.update {
                    it.copy(
                        connectionStatus = GlassesConnectionStatus.CONNECTED,
                        connectedDevice = current,
                        persistedDevice = current
                    )
                }
            }

            override fun onDisconnected() {
                connectionTimeoutJob?.cancel()
                _connectionState.update {
                    it.copy(connectionStatus = GlassesConnectionStatus.DISCONNECTED)
                }
                scheduleReconnect()
            }

            override fun onFailed(errorCode: ValueUtil.CxrBluetoothErrorCode?) {
                connectionTimeoutJob?.cancel()
                _connectionState.update {
                    it.copy(
                        connectionStatus = GlassesConnectionStatus.ERROR,
                        errorMessage = "Bluetooth connect failed: ${errorCode?.name ?: "Unknown"}"
                    )
                }
                scheduleReconnect()
            }
        }
        CxrApi.getInstance().connectBluetooth(context, socketUuid, macAddress, currentCallback)
        CxrApi.getInstance().setAudioStreamListener(audioStreamListener)
        startConnectionTimeout()
    }

    fun removePersistedDevice() {
        preferences.clearDevice()
        _connectionState.update {
            it.copy(
                persistedDevice = null,
                connectedDevice = null,
                selectedDevice = null,
                connectionStatus = GlassesConnectionStatus.DISCONNECTED
            )
        }
        CxrApi.getInstance().deinitBluetooth()
        reconnectJob?.cancel()
    }

    private fun startConnectionTimeout() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = scope.launch {
            delay(10_000L)
            if (_connectionState.value.connectionStatus == GlassesConnectionStatus.CONNECTING) {
                _connectionState.update {
                    it.copy(
                        connectionStatus = GlassesConnectionStatus.ERROR,
                        errorMessage = "Connection timed out. Ensure glasses are powered on and in range."
                    )
                }
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        val persisted = _connectionState.value.persistedDevice ?: return
        if (persisted.socketUuid.isNullOrEmpty() || persisted.macAddress.isNullOrEmpty()) return
        if (!hasPermissions()) return
        reconnectJob = scope.launch {
            delay(2_000L)
            if (bluetoothAdapter?.isEnabled != true) return@launch
            connectWithIdentifiers(
                socketUuid = persisted.socketUuid!!,
                macAddress = persisted.macAddress!!,
                name = persisted.name,
                rokidAccount = persisted.rokidAccount,
                glassesType = persisted.glassesType
            )
        }
    }

    private fun BluetoothDevice.toUiModel(): RokidGlassesDevice {
        return RokidGlassesDevice(
            name = this.name ?: this.address ?: "Unknown",
            macAddress = this.address
        )
    }

    @SuppressLint("MissingPermission")
    private fun getConnectedDevices(persistedAddress: String? = null): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.filter { device ->
            try {
                val isConnected = device::class.java.getMethod("isConnected").invoke(device) as Boolean
                if (!isConnected) return@filter false
                val nameMatches = device.name?.contains("Glasses", ignoreCase = true) == true
                val persistedMatches = persistedAddress != null && device.address == persistedAddress
                nameMatches || persistedMatches
            } catch (_: Exception) {
                false
            }
        } ?: emptyList()
    }
}

data class GlassesConnectionState(
    val isBluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val discoveredDevices: List<RokidGlassesDevice> = emptyList(),
    val bondedDevices: List<RokidGlassesDevice> = emptyList(),
    val persistedDevice: RokidGlassesDevice? = null,
    val connectedDevice: RokidGlassesDevice? = null,
    val selectedDevice: RokidGlassesDevice? = null,
    val connectionStatus: GlassesConnectionStatus = GlassesConnectionStatus.DISCONNECTED,
    val errorMessage: String? = null
)
