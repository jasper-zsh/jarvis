package pro.sihao.jarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import pro.sihao.jarvis.connection.GlassesConnectionManager
import pro.sihao.jarvis.connection.GlassesConnectionState

@HiltViewModel
class GlassesViewModel @Inject constructor(
    private val connectionManager: GlassesConnectionManager,
    private val glassesPipeCatBridge: pro.sihao.jarvis.data.bridge.GlassesPipeCatBridge? = null
) : ViewModel() {

    companion object {
        val requiredPermissions: Array<String> =
            GlassesConnectionManager.REQUIRED_PERMISSIONS.toTypedArray()
    }

    val uiState: StateFlow<GlassesConnectionState> = connectionManager.connectionState

    fun hasRequiredPermissions() = connectionManager.hasPermissions()

    fun onPermissionsResult(results: Map<String, Boolean>) {
        if (results.values.all { it }) {
            connectionManager.updateBluetoothState()
            connectionManager.refreshPersistedDevice()
            connectionManager.ensureAutoReconnect()
        }
    }

    fun updateBluetoothState() {
        connectionManager.updateBluetoothState()
        connectionManager.refreshPersistedDevice()
    }

    fun startScan() = connectionManager.startScan()
    fun stopScan() = connectionManager.stopScan()
    fun refreshPersistedDevice() = connectionManager.refreshPersistedDevice()
    fun clearError() {
        // No-op: errors are held in connection state; screen can filter them if needed.
    }

    fun connectToDevice(address: String) = connectionManager.connectToDevice(address)
    fun removePersistedDevice() = connectionManager.removePersistedDevice()
    fun ensureAutoReconnect() = connectionManager.ensureAutoReconnect()
    
    /**
     * Enable or disable PipeCat integration for glasses
     */
    fun setPipeCatIntegrationEnabled(enabled: Boolean) {
        glassesPipeCatBridge?.setGlassesIntegrationEnabled(enabled)
    }
    
    /**
     * Check if PipeCat integration is available
     */
    fun isPipeCatIntegrationAvailable(): Boolean {
        return glassesPipeCatBridge != null
    }
}
