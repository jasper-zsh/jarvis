package pro.sihao.jarvis.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import pro.sihao.jarvis.domain.model.GlassesConnectionStatus
import pro.sihao.jarvis.domain.model.RokidGlassesDevice
import pro.sihao.jarvis.ui.viewmodel.GlassesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassesScreen(
    onBackClick: () -> Unit,
    viewModel: GlassesViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.onPermissionsResult(result)
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.updateBluetoothState()
        viewModel.ensureAutoReconnect()
    }

    LaunchedEffect(Unit) {
        viewModel.updateBluetoothState()
        viewModel.ensureAutoReconnect()
        viewModel.refreshPersistedDevice()
        if (viewModel.hasRequiredPermissions() && viewModel.uiState.value.isBluetoothEnabled) {
            viewModel.startScan()
        }
    }

    val devices = (uiState.bondedDevices + uiState.discoveredDevices + listOfNotNull(uiState.persistedDevice)).distinctBy {
        it.macAddress ?: it.name
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rokid Glasses", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ConnectionStatusCard(
                    isBluetoothEnabled = uiState.isBluetoothEnabled,
                    isScanning = uiState.isScanning,
                    status = uiState.connectionStatus,
                    onRequestPermissions = {
                        permissionLauncher.launch(GlassesViewModel.requiredPermissions)
                    },
                    onEnableBluetooth = {
                        enableBluetoothLauncher.launch(
                            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        )
                    },
                    onScan = {
                        if (viewModel.hasRequiredPermissions()) {
                            viewModel.startScan()
                        } else {
                            permissionLauncher.launch(GlassesViewModel.requiredPermissions)
                        }
                    },
                    onStopScan = { viewModel.stopScan() }
                )
            }

            uiState.errorMessage?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            uiState.connectedDevice?.let { device ->
                item {
                    ConnectedInfoCard(device = device)
                }
            }

            item {
                DeviceListCard(
                    devices = devices,
                    connectedMac = uiState.connectedDevice?.macAddress,
                    onConnect = { address ->
                        if (!viewModel.hasRequiredPermissions()) {
                            permissionLauncher.launch(GlassesViewModel.requiredPermissions)
                            return@DeviceListCard
                        }
                        if (!uiState.isBluetoothEnabled) {
                            enableBluetoothLauncher.launch(
                                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            )
                            return@DeviceListCard
                        }
                        viewModel.connectToDevice(address)
                    }
                )
            }

            uiState.persistedDevice?.let { saved ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Saved device", fontWeight = FontWeight.Bold)
                            Text(saved.name)
                            Button(onClick = { viewModel.removePersistedDevice() }) {
                                Text("Remove saved device")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    isBluetoothEnabled: Boolean,
    isScanning: Boolean,
    status: GlassesConnectionStatus,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onScan: () -> Unit,
    onStopScan: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Connection", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when {
                    status == GlassesConnectionStatus.CONNECTED -> Icons.Filled.BluetoothConnected
                    isBluetoothEnabled -> Icons.Filled.Bluetooth
                    else -> Icons.Filled.BluetoothDisabled
                }
                Icon(icon, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (status) {
                        GlassesConnectionStatus.CONNECTED -> "Connected"
                        GlassesConnectionStatus.CONNECTING -> "Connecting..."
                        GlassesConnectionStatus.ERROR -> "Error"
                        GlassesConnectionStatus.DISCONNECTED -> if (isBluetoothEnabled) "Disconnected" else "Bluetooth Off"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRequestPermissions) {
                    Text("Permissions")
                }
                Button(onClick = onEnableBluetooth) {
                    Text("Enable Bluetooth")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onScan, enabled = !isScanning) {
                    Text(if (isScanning) "Scanning..." else "Scan for glasses")
                }
                if (isScanning) {
                    Button(onClick = onStopScan) {
                        Text("Stop scan")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceListCard(
    devices: List<RokidGlassesDevice>,
    connectedMac: String?,
    onConnect: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Available glasses", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (devices.isEmpty()) {
                Text(
                    "No glasses found. Ensure Bluetooth is enabled and scanning.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    devices.forEach { device ->
                        DeviceRow(
                            device = device,
                            isConnected = connectedMac != null && device.macAddress == connectedMac,
                            onConnect = onConnect
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: RokidGlassesDevice,
    isConnected: Boolean,
    onConnect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(device.name, fontWeight = FontWeight.SemiBold)
            device.macAddress?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = { device.macAddress?.let(onConnect) }, enabled = device.macAddress != null) {
            Text(if (isConnected) "Connected" else "Connect")
        }
    }
}

@Composable
private fun ConnectedInfoCard(device: RokidGlassesDevice) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Connected glasses", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Name: ${device.name}")
            device.macAddress?.let { Text("MAC: $it") }
            device.glassesType?.let { Text("Glasses Type: $it") }
        }
    }
}
