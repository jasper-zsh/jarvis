package pro.sihao.jarvis.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.sihao.jarvis.permission.PermissionManager
import pro.sihao.jarvis.ui.model.InputMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessageInput(
    message: String,
    inputMode: InputMode,
    isLoading: Boolean,
    isRecording: Boolean,
    recordingDuration: Long,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onInputModeChange: (InputMode) -> Unit,
    onEmojiSelected: (String) -> Unit,
    onVoiceRecordStart: () -> Unit = {},
    onVoiceRecordStop: () -> Unit = {},
    onVoiceRecordCancel: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    permissionStatus: PermissionManager.MediaPermissionsSummary,
    modifier: Modifier = Modifier
) {
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRecording) {
                RecordingStatus(recordingDuration = recordingDuration)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModeToggleButton(
                    mode = inputMode,
                    enabled = !isLoading,
                    onToggle = {
                        val next = if (inputMode == InputMode.TEXT) InputMode.VOICE else InputMode.TEXT
                        onInputModeChange(next)
                    }
                )

                if (inputMode == InputMode.TEXT) {
                    TextModeField(
                        value = message,
                        onValueChange = onMessageChange,
                        onSend = onSendClick,
                        onEmojiSelected = onEmojiSelected,
                        onShowEmoji = { showEmojiPicker = it },
                        onShowAttach = { showAttachMenu = it },
                        showEmojiPicker = showEmojiPicker,
                        showAttachMenu = showAttachMenu,
                        onCameraClick = onCameraClick,
                        onGalleryClick = onGalleryClick,
                        isLoading = isLoading,
                        permissionStatus = permissionStatus
                    )
                } else {
                    VoiceModeField(
                        isRecording = isRecording,
                        recordingDuration = recordingDuration,
                        enabled = permissionStatus.canRecordVoice && !isLoading,
                        onStart = onVoiceRecordStart,
                        onStop = onVoiceRecordStop,
                        onCancel = onVoiceRecordCancel
                    )

                    AttachControl(
                        expanded = showAttachMenu,
                        onExpandedChange = { showAttachMenu = it },
                        canTakePhoto = permissionStatus.canTakePhoto,
                        canSelectFromGallery = permissionStatus.canSelectFromGallery,
                        onCameraClick = onCameraClick,
                        onGalleryClick = onGalleryClick,
                        enabled = !isLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeToggleButton(
    mode: InputMode,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    IconButton(
        onClick = onToggle,
        enabled = enabled,
        modifier = Modifier
            .size(44.dp)
            .testTag("input_mode_toggle")
    ) {
        val (icon, description) = if (mode == InputMode.TEXT) {
            Icons.Filled.KeyboardVoice to "Switch to voice input"
        } else {
            Icons.Filled.Textsms to "Switch to text input"
        }
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RowScope.TextModeField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onEmojiSelected: (String) -> Unit,
    onShowEmoji: (Boolean) -> Unit,
    onShowAttach: (Boolean) -> Unit,
    showEmojiPicker: Boolean,
    showAttachMenu: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    isLoading: Boolean,
    permissionStatus: PermissionManager.MediaPermissionsSummary
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .wrapContentHeight(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 8.dp)
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("message_text_field"),
                placeholder = { Text("Hold to talk or type...") },
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
                enabled = !isLoading,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            EmojiControl(
                expanded = showEmojiPicker,
                onExpandedChange = onShowEmoji,
                onEmojiSelected = onEmojiSelected,
                enabled = !isLoading
            )

            AttachControl(
                expanded = showAttachMenu,
                onExpandedChange = onShowAttach,
                canTakePhoto = permissionStatus.canTakePhoto,
                canSelectFromGallery = permissionStatus.canSelectFromGallery,
                onCameraClick = onCameraClick,
                onGalleryClick = onGalleryClick,
                enabled = !isLoading
            )

            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = if (value.isNotBlank() && !isLoading) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.VoiceModeField(
    isRecording: Boolean,
    recordingDuration: Long,
    enabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    HoldToTalkButton(
        isRecording = isRecording,
        recordingDuration = recordingDuration,
        enabled = enabled,
        onStart = onStart,
        onStop = onStop,
        onCancel = onCancel,
        modifier = Modifier
            .weight(1f)
            .testTag("hold_to_talk")
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HoldToTalkButton(
    isRecording: Boolean,
    recordingDuration: Long,
    enabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cancelThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    var isCancelPreview by remember { mutableStateOf(false) }
    val textColor by rememberUpdatedState(
        when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            isCancelPreview -> MaterialTheme.colorScheme.onError
            isRecording -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
        }
    )
    val backgroundColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        isRecording && isCancelPreview -> MaterialTheme.colorScheme.errorContainer
        isRecording -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        tonalElevation = if (isRecording) 4.dp else 0.dp,
        shadowElevation = if (isRecording) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        onStart()
                        isCancelPreview = false
                        var totalDx = 0f

                        val dragSucceeded = drag(down.id) { change ->
                            val deltaX = change.positionChange().x
                            if (deltaX != 0f) {
                                totalDx += deltaX
                                isCancelPreview = totalDx < -cancelThresholdPx
                                change.consume()
                            }
                        }

                        if (dragSucceeded) {
                            if (isCancelPreview) onCancel() else onStop()
                            isCancelPreview = false
                        } else {
                            onCancel()
                            isCancelPreview = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val text = when {
                !enabled -> "Mic disabled"
                !isRecording -> "Hold to talk"
                isCancelPreview -> "Release to cancel"
                else -> "Release to send"
            }
            val durationLabel = if (isRecording && !isCancelPreview) {
                " • ${formatDuration(recordingDuration)}"
            } else ""
            Text(
                text = text + durationLabel,
                color = if (enabled) textColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmojiControl(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEmojiSelected: (String) -> Unit,
    enabled: Boolean
) {
    Box {
        EmojiButton(
            onClick = { onExpandedChange(true) },
            enabled = enabled,
            isActive = expanded
        )
        EmojiMenu(
            expanded = expanded,
            onDismiss = { onExpandedChange(false) },
            onEmojiSelected = {
                onEmojiSelected(it)
                onExpandedChange(false)
            }
        )
    }
}

@Composable
private fun AttachControl(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    canTakePhoto: Boolean,
    canSelectFromGallery: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    enabled: Boolean
) {
    Box {
        AttachButton(
            onClick = { onExpandedChange(true) },
            enabled = enabled && (canTakePhoto || canSelectFromGallery),
            modifier = Modifier
        )
        AttachMenu(
            expanded = expanded,
            onDismiss = { onExpandedChange(false) },
            onCameraClick = {
                onExpandedChange(false)
                onCameraClick()
            },
            onGalleryClick = {
                onExpandedChange(false)
                onGalleryClick()
            },
            canTakePhoto = canTakePhoto,
            canSelectFromGallery = canSelectFromGallery
        )
    }
}

@Composable
private fun EmojiButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(40.dp)
            .testTag("emoji_button")
    ) {
        Icon(
            imageVector = Icons.Filled.EmojiEmotions,
            contentDescription = "Emoji",
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AttachButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(40.dp)
            .testTag("attach_button")
    ) {
        Icon(
            imageVector = Icons.Filled.AttachFile,
            contentDescription = "Attach",
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmojiMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    val emojis = listOf("😀", "😂", "😍", "👍", "🙏", "🎉")
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        emojis.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { emoji ->
                    TextButton(onClick = { onEmojiSelected(emoji) }) {
                        Text(emoji, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    canTakePhoto: Boolean,
    canSelectFromGallery: Boolean
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Camera") },
            onClick = onCameraClick,
            enabled = canTakePhoto,
            leadingIcon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
            modifier = Modifier.testTag("attach_camera")
        )
        DropdownMenuItem(
            text = { Text("Gallery") },
            onClick = onGalleryClick,
            enabled = canSelectFromGallery,
            leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
            modifier = Modifier.testTag("attach_gallery")
        )
    }
}

@Composable
private fun RecordingStatus(recordingDuration: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.error)
        )
        Text(
            text = "Recording… ${formatDuration(recordingDuration)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = (totalSeconds / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
