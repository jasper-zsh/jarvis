package pro.sihao.jarvis.core.initialization

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pro.sihao.jarvis.core.data.database.initializer.DatabaseInitializer
import pro.sihao.jarvis.platform.android.connection.GlassesConnectionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseInitializer: DatabaseInitializer,
    private val glassesConnectionManager: GlassesConnectionManager
) {
    companion object {
        private const val TAG = "AppInitializationCoordinator"
        private const val INITIALIZATION_TIMEOUT_MS = 10000L
        private const val RETRY_DELAY_MS = 1000L
    }

    private val _initializationState = MutableStateFlow(InitializationState.NOT_STARTED)
    val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var initializationJob: kotlinx.coroutines.Job? = null

    enum class InitializationState {
        NOT_STARTED,
        INITIALIZING_DATABASE,
        INITIALIZING_SERVICES,
        READY,
        FAILED
    }

    fun initialize() {
        Log.d(TAG, "Starting app initialization")

        initializationJob?.cancel()
        initializationJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                _initializationState.value = InitializationState.INITIALIZING_DATABASE
                Log.d(TAG, "Step 1: Initializing database")

                // Initialize database first
                CoroutineScope(Dispatchers.IO).launch {
                    databaseInitializer.initializeIfNeeded()
                }.join()

                _initializationState.value = InitializationState.INITIALIZING_SERVICES
                Log.d(TAG, "Step 2: Initializing services and triggering glasses connection")

                // Wait a bit for system services to be ready
                delay(500)

                // Initialize glasses connection manager
                glassesConnectionManager.updateBluetoothState()

                // Trigger auto-connection with retries
                triggerGlassesAutoConnectionWithRetry()

                _initializationState.value = InitializationState.READY
                _isReady.value = true
                Log.d(TAG, "App initialization completed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "App initialization failed", e)
                _initializationState.value = InitializationState.FAILED
                _isReady.value = false
            }
        }
    }

    private suspend fun triggerGlassesAutoConnectionWithRetry() {
        var retryCount = 0
        val maxRetries = 5

        while (retryCount < maxRetries) {
            try {
                Log.d(TAG, "Attempting glasses auto-connection (attempt ${retryCount + 1}/$maxRetries)")

                // Check if all conditions are met before attempting connection
                if (glassesConnectionManager.hasPermissions() &&
                    glassesConnectionManager.isBluetoothEnabled() &&
                    glassesConnectionManager.hasPersistedDevice()) {

                    glassesConnectionManager.ensureAutoReconnect()
                    Log.d(TAG, "Glasses auto-connection triggered successfully")
                    return
                } else {
                    Log.w(TAG, "Connection conditions not met: " +
                        "hasPermissions=${glassesConnectionManager.hasPermissions()}, " +
                        "isBluetoothEnabled=${glassesConnectionManager.isBluetoothEnabled()}, " +
                        "hasPersistedDevice=${glassesConnectionManager.hasPersistedDevice()}")
                }

                retryCount++
                if (retryCount < maxRetries) {
                    delay(RETRY_DELAY_MS)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during glasses auto-connection attempt ${retryCount + 1}", e)
                retryCount++
                if (retryCount < maxRetries) {
                    delay(RETRY_DELAY_MS * 2) // Longer delay on errors
                }
            }
        }

        Log.w(TAG, "Glasses auto-connection failed after $maxRetries attempts")
    }

    fun isInitializationComplete(): Boolean {
        return _isReady.value
    }

    suspend fun waitForInitialization(timeoutMs: Long = INITIALIZATION_TIMEOUT_MS): Boolean {
        return try {
            var completed = false
            val startTime = System.currentTimeMillis()

            while (!completed && (System.currentTimeMillis() - startTime) < timeoutMs) {
                if (_isReady.value) {
                    completed = true
                } else {
                    delay(100) // Small delay between checks
                }
            }

            completed
        } catch (e: Exception) {
            Log.w(TAG, "Initialization timeout after ${timeoutMs}ms", e)
            false
        }
    }

    fun cleanup() {
        initializationJob?.cancel()
        _initializationState.value = InitializationState.NOT_STARTED
        _isReady.value = false
    }
}