package pro.sihao.jarvis.platform.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(false) // Default to false, will be updated in init
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    companion object {
        private const val TAG = "NetworkMonitor"
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available: $network")
            _isConnected.value = true
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost: $network")
            _isConnected.value = false
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val connected = hasInternet && isValidated
            Log.d(TAG, "Network capabilities changed - Internet: $hasInternet, Validated: $isValidated, Connected: $connected")
            _isConnected.value = connected
        }
    }

    init {
        // Start monitoring automatically when created
        startMonitoring()
    }

    fun startMonitoring() {
        Log.d(TAG, "Starting network monitoring")
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Check initial state
        val initialConnected = isCurrentlyConnected()
        Log.d(TAG, "Initial network state: $initialConnected")
        _isConnected.value = initialConnected
    }

    fun stopMonitoring() {
        Log.d(TAG, "Stopping network monitoring")
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun isCurrentlyConnected(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork == null) {
            Log.d(TAG, "No active network")
            return false
        }
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val connected = hasInternet && isValidated
        Log.d(TAG, "Current connection check - HasInternet: $hasInternet, IsValidated: $isValidated, Connected: $connected")
        return connected
    }
}