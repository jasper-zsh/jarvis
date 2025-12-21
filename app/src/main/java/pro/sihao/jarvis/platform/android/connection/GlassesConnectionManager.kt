package pro.sihao.jarvis.platform.android.connection

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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pro.sihao.jarvis.core.data.storage.GlassesPreferences
import pro.sihao.jarvis.core.domain.model.GlassesConnectionStatus
import pro.sihao.jarvis.core.domain.model.RokidGlassesDevice

/**
 * Simplified manager that handles glasses Bluetooth connection as a standard audio device.
 *
 * After refactoring, glasses now act as a standard Bluetooth SCO headset rather than requiring
 * custom audio processing. This manager handles device discovery, connection, and basic state management.
 * Audio routing is handled automatically by Android AudioManager when glasses are connected.
 */
@Singleton
class GlassesConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: GlassesPreferences
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
        android.util.Log.d("GlassesConnectionManager", "ensureAutoReconnect() called")

        updateBluetoothState()
        refreshPersistedDevice()

        val currentState = _connectionState.value
        val persisted = currentState.persistedDevice

        android.util.Log.d("GlassesConnectionManager", "Auto-reconnect checks: " +
            "currentStatus=${currentState.connectionStatus}, " +
            "hasPersistedDevice=${persisted != null}, " +
            "hasPermissions=${hasPermissions()}, " +
            "isBluetoothEnabled=${bluetoothAdapter?.isEnabled == true}")

        when (currentState.connectionStatus) {
            GlassesConnectionStatus.CONNECTED -> {
                android.util.Log.d("GlassesConnectionManager", "Already connected, skipping auto-reconnect")
                return
            }
            GlassesConnectionStatus.CONNECTING -> {
                android.util.Log.d("GlassesConnectionManager", "Already connecting, skipping auto-reconnect")
                return
            }
            else -> { /* Continue with connection attempt */ }
        }

        if (persisted?.socketUuid?.isNullOrEmpty() != false || persisted?.macAddress?.isNullOrEmpty() != false) {
            android.util.Log.w("GlassesConnectionManager", "No saved glasses device found for auto-reconnect: " +
                "socketUuid=${persisted?.socketUuid}, macAddress=${persisted?.macAddress}")
            return
        }

        if (!hasPermissions()) {
            android.util.Log.w("GlassesConnectionManager", "Missing required Bluetooth permissions for auto-reconnect")
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            android.util.Log.w("GlassesConnectionManager", "Bluetooth is not enabled for auto-reconnect")
            return
        }

        android.util.Log.i("GlassesConnectionManager", "Initiating auto-reconnect to saved glasses: " +
            "name=${persisted.name}, mac=${persisted.macAddress}, socket=${persisted.socketUuid}")

        try {
            connectWithIdentifiers(
                socketUuid = persisted.socketUuid!!,
                macAddress = persisted.macAddress!!,
                name = persisted.name,
                rokidAccount = persisted.rokidAccount,
                glassesType = persisted.glassesType
            )
        } catch (e: Exception) {
            android.util.Log.e("GlassesConnectionManager", "Exception during auto-reconnect initiation", e)
            _connectionState.update {
                it.copy(errorMessage = "Auto-reconnect failed: ${e.message}")
            }
        }
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

    fun hasPersistedDevice(): Boolean {
        val persisted = preferences.loadDevice()
        return persisted?.socketUuid?.isNotEmpty() == true && persisted?.macAddress?.isNotEmpty() == true
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun updateBluetoothState() {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = manager?.adapter
        scanner = bluetoothAdapter?.bluetoothLeScanner
        _connectionState.update { state ->
            state.copy(isBluetoothEnabled = bluetoothAdapter?.isEnabled == true)
        }
    }

  
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
) {
    val isConnected: Boolean
        get() = connectedDevice != null && connectionStatus == GlassesConnectionStatus.CONNECTED
}
