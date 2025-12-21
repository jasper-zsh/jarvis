package pro.sihao.jarvis

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.sihao.jarvis.core.initialization.AppInitializationCoordinator
import pro.sihao.jarvis.core.presentation.navigation.BottomTabNavigation
import pro.sihao.jarvis.core.presentation.navigation.NavigationManager
import pro.sihao.jarvis.core.presentation.theme.JarvisTheme
import pro.sihao.jarvis.platform.android.connection.GlassesConnectionManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigationManager: NavigationManager

    @Inject
    lateinit var glassesConnectionManager: GlassesConnectionManager

    @Inject
    lateinit var initializationCoordinator: AppInitializationCoordinator

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "MainActivity onCreate")

        setContent {
            JarvisTheme {
                // Auto-connection trigger when MainActivity becomes visible
                LaunchedEffect(Unit) {
                    Log.d(TAG, "MainActivity LaunchedEffect triggered - ensuring glasses connection")
                    triggerGlassesConnectionFromUI()
                }

                BottomTabNavigation(
                    navigationManager = navigationManager,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity onResume - triggering additional connection attempt")

        // Additional connection attempt when app comes to foreground
        CoroutineScope(Dispatchers.Main).launch {
            delay(500) // Small delay to ensure services are ready
            triggerGlassesConnectionFromUI()
        }
    }

    private fun triggerGlassesConnectionFromUI() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Wait for initialization if not ready yet
                if (!initializationCoordinator.isInitializationComplete()) {
                    Log.d(TAG, "Waiting for app initialization before connection attempt")
                    val isReady = initializationCoordinator.waitForInitialization(3000L)
                    if (!isReady) {
                        Log.w(TAG, "Initialization timeout, proceeding with connection attempt anyway")
                    }
                }

                // Update Bluetooth state first
                glassesConnectionManager.updateBluetoothState()

                // Check connection prerequisites
                val hasPermissions = glassesConnectionManager.hasPermissions()
                val isBluetoothEnabled = glassesConnectionManager.isBluetoothEnabled()
                val hasPersistedDevice = glassesConnectionManager.hasPersistedDevice()

                Log.d(TAG, "UI Connection prerequisites - Permissions: $hasPermissions, Bluetooth: $isBluetoothEnabled, Saved Device: $hasPersistedDevice")

                // Only attempt connection if not already connected and prerequisites are met
                if (!glassesConnectionManager.connectionState.value.isConnected &&
                    hasPermissions && isBluetoothEnabled && hasPersistedDevice) {

                    Log.d(TAG, "Triggering glasses connection from MainActivity")
                    glassesConnectionManager.ensureAutoReconnect()

                } else if (glassesConnectionManager.connectionState.value.isConnected) {
                    Log.d(TAG, "Already connected to glasses, no action needed")
                } else {
                    Log.w(TAG, "Cannot trigger connection - missing prerequisites: " +
                        "Permissions=$hasPermissions, Bluetooth=$isBluetoothEnabled, Saved Device=$hasPersistedDevice")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error triggering glasses connection from MainActivity", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity onDestroy")
    }
}