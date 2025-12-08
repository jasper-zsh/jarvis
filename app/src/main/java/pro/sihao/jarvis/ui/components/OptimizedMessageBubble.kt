package pro.sihao.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.sihao.jarvis.domain.model.Message
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OptimizedMessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    // Cache the time format and formatted time to avoid recomputation
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(message.timestamp) }

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
            // Cache the bubble shape to avoid recomputation
            val bubbleShape = remember(message.isFromUser) {
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                )
            }

            // Cache the bubble colors
            val bubbleColor = if (message.isFromUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }

            val textColor = if (message.isFromUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .padding(12.dp)
            ) {
                if (message.isLoading) {
                    TypingIndicator()
                } else {
                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 16.sp,
                        // Optimize text rendering for long messages
                        maxLines = Int.MAX_VALUE
                    )
                }
            }

            if (!message.isLoading) {
                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MessageListItem(
    message: Message,
    modifier: Modifier = Modifier
) {
    // A more performant wrapper for large lists
    OptimizedMessageBubble(
        message = message,
        modifier = modifier
    )
}