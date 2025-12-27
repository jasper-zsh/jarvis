package pro.sihao.jarvis.features.realtime.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import pro.sihao.jarvis.features.realtime.presentation.components.realtime.BotIndicator
import pro.sihao.jarvis.features.realtime.presentation.components.realtime.CompactBotIndicator
import pro.sihao.jarvis.features.realtime.presentation.components.realtime.ConversationDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealTimeCallScreen(
    pipeCatViewModel: PipeCatViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by pipeCatViewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        CallHeader(
            connectionState = uiState.connectionState,
            modifier = Modifier.fillMaxWidth(),
            onClear = { pipeCatViewModel.clearTranscripts() }
        )

        // Transcript display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            ConversationDisplay(
                transcripts = uiState.transcripts,
                connectionState = uiState.connectionState,
                modifier = Modifier.fillMaxSize()
            )
        }


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
            onConnect = pipeCatViewModel::connectWithDefaultConfig,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CallHeader(
    connectionState: PipeCatConnectionState,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Bot indicator + Title
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                CompactBotIndicator(connectionState = connectionState)
                Text(
                    text = "Real-time Voice Chat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Right: Clear button
            onClear?.let { clearCallback ->
                TextButton(onClick = clearCallback) {
                    Text("Clear")
                }
            }
        }
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
    onConnect: () -> Unit,
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
                    onClick = onConnect,
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