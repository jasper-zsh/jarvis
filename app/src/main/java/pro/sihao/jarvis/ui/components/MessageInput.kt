package pro.sihao.jarvis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.sihao.jarvis.permission.PermissionManager

@Composable
fun MessageInput(
    message: String,
    isLoading: Boolean,
    isRecording: Boolean,
    recordingDuration: Long,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onVoiceRecordStart: () -> Unit = {},
    onVoiceRecordStop: () -> Unit = {},
    onVoiceRecordCancel: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    permissionStatus: PermissionManager.MediaPermissionsSummary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Media buttons row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    VoiceRecordButton(
                        isRecording = isRecording,
                        recordingDuration = recordingDuration,
                        onRecordingStart = onVoiceRecordStart,
                        onRecordingStop = onVoiceRecordStop,
                        onRecordingCancel = onVoiceRecordCancel,
                        enabled = permissionStatus.canRecordVoice && !isLoading,
                        modifier = Modifier
                            .height(48.dp)
                            .widthIn(min = 48.dp)
                    )

                    // Camera button
                    MediaButton(
                        icon = Icons.Filled.CameraAlt,
                        contentDescription = "Take photo",
                        onClick = onCameraClick,
                        enabled = permissionStatus.canTakePhoto && !isLoading,
                        modifier = Modifier.size(40.dp)
                    )

                    // Gallery button
                    MediaButton(
                        icon = Icons.Filled.PhotoLibrary,
                        contentDescription = "Choose photo",
                        onClick = onGalleryClick,
                        enabled = permissionStatus.canSelectFromGallery && !isLoading,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Text input field
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Type your message...",
                            fontWeight = FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    readOnly = isLoading
                )

                // Send button
                if (message.isNotBlank() && !isLoading) {
                    FloatingActionButton(
                        onClick = onSendClick,
                        modifier = Modifier.size(56.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun VoiceRecordButton(
    isRecording: Boolean,
    recordingDuration: Long = 0,
    onRecordingStart: () -> Unit,
    onRecordingStop: () -> Unit,
    onRecordingCancel: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // This is a press-and-hold button for voice recording
    // Implementation would require handling touch events for press and hold
    // For now, this is a placeholder that could be expanded with drag detection
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            // Recording state - show stop and duration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Recording indicator
                IconButton(
                    onClick = onRecordingStop,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = "Stop recording",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Duration display
                Text(
                    text = "${(recordingDuration / 1000 / 60).toString().padStart(2, '0')}:${((recordingDuration / 1000) % 60).toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            // Idle state - show microphone button
            IconButton(
                onClick = onRecordingStart,
                enabled = enabled,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = "Start recording",
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
