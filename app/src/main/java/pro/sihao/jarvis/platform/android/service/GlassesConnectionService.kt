package pro.sihao.jarvis.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.sihao.jarvis.core.initialization.AppInitializationCoordinator
import pro.sihao.jarvis.platform.android.connection.GlassesConnectionManager
import javax.inject.Inject

@AndroidEntryPoint
class GlassesConnectionService : Service() {

    @Inject
    lateinit var manager: GlassesConnectionManager

    @Inject
    lateinit var initializationCoordinator: AppInitializationCoordinator

    companion object {
        private const val TAG = "GlassesConnectionService"
        private const val MAX_INIT_WAIT_MS = 5000L
        private const val RETRY_INTERVAL_MS = 30000L // Retry every 30 seconds
    }

    private var connectionRetryJob: kotlinx.coroutines.Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "GlassesConnectionService started")

        // Start the connection process with proper initialization timing
        startConnectionWithInitializationCheck()

        return START_STICKY
    }

    private fun startConnectionWithInitializationCheck() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Waiting for app initialization to complete...")

                // Wait for initialization to complete or timeout
                val isReady = initializationCoordinator.waitForInitialization(MAX_INIT_WAIT_MS)

                if (isReady) {
                    Log.d(TAG, "App initialization completed, attempting glasses connection")
                    attemptGlassesConnection()
                } else {
                    Log.w(TAG, "App initialization timeout, proceeding with connection attempt anyway")
                    // Still try to connect even if initialization didn't complete
                    attemptGlassesConnection()
                }

                // Start periodic retry attempts
                startPeriodicConnectionAttempts()

            } catch (e: Exception) {
                Log.e(TAG, "Error in connection startup sequence", e)
                // Still try basic connection as fallback
                attemptGlassesConnection()
            }
        }
    }

    private suspend fun attemptGlassesConnection() {
        try {
            Log.d(TAG, "Attempting glasses auto-connection...")

            // Update Bluetooth state first
            manager.updateBluetoothState()

            // Check connection prerequisites
            val hasPermissions = manager.hasPermissions()
            val isBluetoothEnabled = manager.isBluetoothEnabled()
            val hasPersistedDevice = manager.hasPersistedDevice()

            Log.d(TAG, "Connection prerequisites - Permissions: $hasPermissions, Bluetooth: $isBluetoothEnabled, Saved Device: $hasPersistedDevice")

            if (hasPermissions && isBluetoothEnabled && hasPersistedDevice) {
                manager.ensureAutoReconnect()
                Log.d(TAG, "Glasses auto-connection initiated successfully")
            } else {
                Log.w(TAG, "Cannot initiate connection - missing prerequisites: " +
                    "Permissions=$hasPermissions, Bluetooth=$isBluetoothEnabled, Saved Device=$hasPersistedDevice")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error attempting glasses connection", e)
        }
    }

    private fun startPeriodicConnectionAttempts() {
        connectionRetryJob?.cancel()

        connectionRetryJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    delay(RETRY_INTERVAL_MS)
                    Log.d(TAG, "Periodic connection attempt...")

                    // Only attempt if we're not already connected
                    if (!manager.connectionState.value.isConnected) {
                        attemptGlassesConnection()
                    } else {
                        Log.d(TAG, "Already connected to glasses, skipping periodic attempt")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic connection attempt", e)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        connectionRetryJob?.cancel()
        Log.d(TAG, "GlassesConnectionService destroyed")
    }
}
