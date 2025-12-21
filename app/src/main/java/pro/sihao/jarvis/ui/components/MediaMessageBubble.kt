package pro.sihao.jarvis.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import pro.sihao.jarvis.domain.model.ContentType
import pro.sihao.jarvis.domain.model.Message
import java.io.File

@Composable
fun MediaMessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
    onPhotoClick: ((String) -> Unit)? = null,
    onVoicePlay: ((String) -> Unit)? = null,
    onVoicePause: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (message.isFromUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shadowElevation = 2.dp
        ) {
            when (message.contentType) {
                ContentType.TEXT -> TextMessageContent(message)
                ContentType.VOICE -> VoiceMessageContent(
                    message = message,
                    onPlayClick = onVoicePlay,
                    onPauseClick = onVoicePause,
                    textColor = if (message.isFromUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                ContentType.PHOTO -> PhotoMessageContent(
                    message = message,
                    onPhotoClick = onPhotoClick
                )
                ContentType.REALTIME_TRANSCRIPT -> TextMessageContent(
                    message = message.copy(content = "[Transcript] ${message.content}")
                )
                ContentType.REALTIME_RESPONSE -> TextMessageContent(
                    message = message.copy(content = "[Response] ${message.content}")
                )
            }
        }

        // Timestamp for non-text messages
        if (message.contentType != ContentType.TEXT) {
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
            )
        }
    }
}

@Composable
private fun TextMessageContent(message: Message) {
    Text(
        text = message.content,
        modifier = Modifier.padding(16.dp),
        color = if (message.isFromUser) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun VoiceMessageContent(
    message: Message,
    onPlayClick: ((String) -> Unit)?,
    onPauseClick: (() -> Unit)?,
    textColor: Color
) {
    val mediaUrl = message.mediaUrl
    val duration = message.duration ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Play/Pause button
        IconButton(
            onClick = {
                mediaUrl?.let { url ->
                    onPlayClick?.invoke(url)
                } ?: onPauseClick?.invoke()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play voice message",
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
        }

        // Voice waveform visualization (placeholder)
        Row(
            modifier = Modifier
                .weight(1f)
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(20) { index ->
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height((8 + (index % 3) * 8).dp)
                        .background(
                            textColor.copy(alpha = 0.6f),
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        // Duration text
        Text(
            text = formatDuration(duration),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PhotoMessageContent(
    message: Message,
    onPhotoClick: ((String) -> Unit)?
) {
    val thumbnailUrl = message.thumbnailUrl ?: message.mediaUrl

    Column {
        // Photo thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clickable { onPhotoClick?.invoke(message.mediaUrl ?: "") }
        ) {
            if (thumbnailUrl != null) {
                if (thumbnailUrl.startsWith("http")) {
                    // Load from URL
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Photo message",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Load from local file
                    val file = File(thumbnailUrl)
                    if (file.exists()) {
                        // Use Coil to load local image
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(file)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Photo message",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Placeholder if file doesn't exist
                        PhotoPlaceholder()
                    }
                }
            } else {
                PhotoPlaceholder()
            }

            // Photo overlay icon
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ZoomIn,
                    contentDescription = "View full size",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Optional caption text
        if (message.content.isNotBlank()) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (message.isFromUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PhotoPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Image,
                contentDescription = "Photo",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "Photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun formatTime(timestamp: java.util.Date): String {
    val now = java.util.Date()
    val diff = now.time - timestamp.time

    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        else -> {
            val format = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
            format.format(timestamp)
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    return if (minutes > 0) {
        String.format("%d:%02d", minutes, seconds)
    } else {
        String.format("0:%02d", seconds)
    }
}

// Full-screen image viewer component
@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() }
        )

        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Image
        if (imageUrl.startsWith("http")) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Full size image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            val file = File(imageUrl)
            if (file.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(file)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Full size image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}