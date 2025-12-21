package pro.sihao.jarvis.features.realtime.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pro.sihao.jarvis.features.realtime.presentation.viewmodel.PipeCatViewModel
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.platform.android.audio.AudioRoutingStatus
import pro.sihao.jarvis.features.realtime.presentation.components.realtime.ChatModeSelector
import pro.sihao.jarvis.features.realtime.presentation.components.realtime.BotIndicator
import pro.sihao.jarvis.features.realtime.presentation.components.realtime.AudioIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealTimeCallScreen(
    pipeCatViewModel: PipeCatViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by pipeCatViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with mode selector
        CallHeader(
            currentMode = uiState.currentMode,
            onModeSelected = { mode ->
                when (mode) {
                    pro.sihao.jarvis.core.domain.model.ChatMode.TEXT -> {
                        // Text mode navigation now handled by bottom tab navigation
                        // Users can access settings through the navigation system
                    }
                    pro.sihao.jarvis.core.domain.model.ChatMode.REALTIME -> pipeCatViewModel.switchToRealtimeMode()
                    pro.sihao.jarvis.core.domain.model.ChatMode.GLASSES -> {
                        if (uiState.glassesConnected) {
                            pipeCatViewModel.switchToGlassesMode()
                            // Navigation to Glasses tab handled by NavigationManager in ViewModel
                        }
                    }
                }
            },
            glassesConnected = uiState.glassesConnected,
            modifier = Modifier.fillMaxWidth()
        )

        // Main call interface
        CallInterface(
            connectionState = uiState.connectionState,
            isConnected = uiState.isConnected,
            isConnecting = uiState.isConnecting,
            microphoneEnabled = uiState.microphoneEnabled,
            cameraEnabled = uiState.cameraEnabled,
            onToggleMicrophone = pipeCatViewModel::toggleMicrophone,
            onToggleCamera = pipeCatViewModel::toggleCamera,
            onConnect = pipeCatViewModel::connectWithDefaultConfig,
            onDisconnect = pipeCatViewModel::disconnect,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Error message if present
        uiState.errorMessage?.let { errorMessage ->
            ErrorMessage(
                message = errorMessage,
                onDismiss = pipeCatViewModel::clearError,
                modifier = Modifier.fillMaxWidth()
            )
        }

        
        // Bottom controls
        CallControls(
            isConnected = uiState.isConnected,
            isConnecting = uiState.isConnecting,
            microphoneEnabled = uiState.microphoneEnabled,
            cameraEnabled = uiState.cameraEnabled,
            onToggleMicrophone = pipeCatViewModel::toggleMicrophone,
            onToggleCamera = pipeCatViewModel::toggleCamera,
            onEndCall = pipeCatViewModel::disconnect,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CallHeader(
    currentMode: pro.sihao.jarvis.core.domain.model.ChatMode,
    onModeSelected: (pro.sihao.jarvis.core.domain.model.ChatMode) -> Unit,
    glassesConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Real-time Voice Chat",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            ChatModeSelector(
                currentMode = currentMode,
                onModeSelected = onModeSelected,
                glassesConnected = glassesConnected,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}

@Composable
private fun CallInterface(
    connectionState: PipeCatConnectionState,
    isConnected: Boolean,
    isConnecting: Boolean,
    microphoneEnabled: Boolean,
    cameraEnabled: Boolean,
    onToggleMicrophone: (Boolean) -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Bot status and audio indicators
            BotIndicator(
                connectionState = connectionState,
                showDetails = true
            )

            // Audio level indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioIndicator(
                    audioLevel = connectionState.userAudioLevel,
                    isActive = connectionState.userIsSpeaking,
                    isUser = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(32.dp))

                AudioIndicator(
                    audioLevel = connectionState.botAudioLevel,
                    isActive = connectionState.botIsSpeaking,
                    isUser = false,
                    modifier = Modifier.weight(1f)
                )
            }

            // Connection status
            ConnectionStatusCard(
                connectionState = connectionState,
                isConnected = isConnected,
                isConnecting = isConnecting,
                onConnect = onConnect,
                onDisconnect = onDisconnect
            )

            // Media controls
            if (isConnected) {
                MediaControls(
                    microphoneEnabled = microphoneEnabled,
                    cameraEnabled = cameraEnabled,
                    onToggleMicrophone = onToggleMicrophone,
                    onToggleCamera = onToggleCamera
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: PipeCatConnectionState,
    isConnected: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = when {
            isConnecting -> MaterialTheme.colorScheme.primaryContainer
            isConnected -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isConnecting -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Connecting to bot...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                isConnected -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Connected to bot",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                else -> {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Connect",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect to Bot")
                    }
                }
            }

            // Connection quality indicator (when connected)
            if (isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            connectionState.botReady -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                connectionState.botReady -> "Bot Ready - Excellent Connection"
                                else -> "Connecting..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Audio levels
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("User:", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = connectionState.userAudioLevel,
                                modifier = Modifier.width(60.dp)
                            )
                            Text("Bot:", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = connectionState.botAudioLevel,
                                modifier = Modifier.width(60.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaControls(
    microphoneEnabled: Boolean,
    cameraEnabled: Boolean,
    onToggleMicrophone: (Boolean) -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Media Controls",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaControlButton(
                    icon = if (microphoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    label = if (microphoneEnabled) "Mic On" else "Mic Off",
                    isActive = microphoneEnabled,
                    onClick = { onToggleMicrophone(!microphoneEnabled) }
                )

                MediaControlButton(
                    icon = if (cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = if (cameraEnabled) "Camera On" else "Camera Off",
                    isActive = cameraEnabled,
                    onClick = { onToggleCamera(!cameraEnabled) }
                )
            }
        }
    }
}

@Composable
private fun MediaControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CallControls(
    isConnected: Boolean,
    isConnecting: Boolean,
    microphoneEnabled: Boolean,
    cameraEnabled: Boolean,
    onToggleMicrophone: (Boolean) -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isConnected) {
                // Microphone toggle
                IconButton(
                    onClick = { onToggleMicrophone(!microphoneEnabled) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (microphoneEnabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (microphoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (microphoneEnabled) "Mute" else "Unmute",
                        tint = if (microphoneEnabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Camera toggle
                IconButton(
                    onClick = { onToggleCamera(!cameraEnabled) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (cameraEnabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = if (cameraEnabled) "Camera Off" else "Camera On",
                        tint = if (cameraEnabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                // End call
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                // Connect button (when not connected)
                Button(
                    onClick = { /* Handle connect */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isConnecting
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Connect",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect to Bot")
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}