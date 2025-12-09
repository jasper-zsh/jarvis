package pro.sihao.jarvis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import pro.sihao.jarvis.domain.model.ContentType
import pro.sihao.jarvis.domain.model.Message
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
    onPhotoClick: ((String) -> Unit)? = null,
    onVoicePlay: ((String) -> Unit)? = null,
    onVoicePause: (() -> Unit)? = null,
    onCancelLoading: (() -> Unit)? = null
) {
    // Streaming/loading placeholder
    if (message.isLoading) {
        LoadingMessageBubble(
            isFromUser = message.isFromUser,
            modifier = modifier,
            onCancel = onCancelLoading
        )
        return
    }

    // Use MediaMessageBubble for media content types
    if (message.contentType != ContentType.TEXT) {
        MediaMessageBubble(
            message = message,
            onPhotoClick = onPhotoClick,
            onVoicePlay = onVoicePlay,
            onVoicePause = onVoicePause,
            modifier = modifier
        )
    } else {
        // Legacy text message bubble for backward compatibility
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier.widthIn(max = 300.dp),
                horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                            )
                        )
                        .background(
                            if (message.isFromUser) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = if (message.isFromUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = timeFormat.format(message.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingMessageBubble(
    isFromUser: Boolean,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null
) {
    val alignment = if (isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val shimmerAlpha by rememberInfiniteTransition(label = "loading_transition")
        .animateFloat(
            initialValue = 0.2f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "loading_alpha"
        )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isFromUser) 16.dp else 4.dp,
                        bottomEnd = if (isFromUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isFromUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    repeat(3) { idx ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (idx == 0) 0.7f else if (idx == 1) 0.9f else 0.5f)
                                .height(10.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.5f)
                                )
                        )
                    }
                }
                if (onCancel != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
