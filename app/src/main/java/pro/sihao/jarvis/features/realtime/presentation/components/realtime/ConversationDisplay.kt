package pro.sihao.jarvis.features.realtime.presentation.components.realtime

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.sihao.jarvis.core.domain.model.TranscriptMessage
import pro.sihao.jarvis.core.domain.model.MessageRole
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension function to check if LazyListState is scrolled to bottom
 */
private fun androidx.compose.foundation.lazy.LazyListState.isScrolledToBottom(threshold: Float = 100f): Boolean {
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
    val totalItems = layoutInfo.totalItemsCount

    return if (lastVisibleItem == null) {
        true // At top/bottom by default
    } else {
        // Check if last item is visible and near bottom
        lastVisibleItem.index == totalItems - 1 &&
                (layoutInfo.viewportEndOffset - (lastVisibleItem.offset + lastVisibleItem.size)) <= threshold
    }
}

@Composable
fun ConversationDisplay(
    transcripts: List<TranscriptMessage>,
    connectionState: PipeCatConnectionState,
    modifier: Modifier = Modifier,
    userLabel: String = "You",
    assistantLabel: String = "Assistant"
) {
    // Phase 1: Smart auto-scroll
    val listState = rememberLazyListState()
    val isScrolledToBottom by remember {
        derivedStateOf { listState.isScrolledToBottom() }
    }

    LaunchedEffect(transcripts.size) {
        // Only auto-scroll if user is already at bottom
        if (isScrolledToBottom && transcripts.isNotEmpty()) {
            listState.animateScrollToItem(transcripts.size - 1)
        }
    }

    // Phase 2: Connection state UI - prioritize messages over connection states
    if (transcripts.isNotEmpty()) {
        MessageList(
            transcripts = transcripts,
            connectionState = connectionState,
            listState = listState,
            modifier = modifier,
            userLabel = userLabel,
            assistantLabel = assistantLabel
        )
    } else {
        ConnectionStateDisplay(
            connectionState = connectionState,
            modifier = modifier
        )
    }
}

@Composable
private fun ConnectionStateDisplay(
    connectionState: PipeCatConnectionState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            connectionState.errorMessage != null -> ErrorState(connectionState.errorMessage)
            connectionState.isConnecting -> ConnectingState()
            !connectionState.isConnected -> DisconnectedState()
            connectionState.isConnected && !connectionState.botReady -> BotInitializingState()
            else -> WaitingForMessagesState()
        }
    }
}

@Composable
private fun ConnectingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Connecting to agent...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DisconnectedState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Disconnected",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Not connected to agent",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Connect to an agent to see conversation messages in real-time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(errorMessage: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = "Connection Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BotInitializingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Bot is initializing...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WaitingForMessagesState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Connected",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Waiting for messages...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageList(
    transcripts: List<TranscriptMessage>,
    connectionState: PipeCatConnectionState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
    userLabel: String,
    assistantLabel: String
) {
    // Phase 5: Enhanced animations
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = transcripts,
            key = { it.id }
        ) { message ->
            // Phase 5: Animated entrance for messages
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            ) {
                TranscriptMessageBubble(
                    message = message,
                    connectionState = connectionState,
                    userLabel = userLabel,
                    assistantLabel = assistantLabel,
                    modifier = Modifier.animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun TranscriptMessageBubble(
    message: TranscriptMessage,
    connectionState: PipeCatConnectionState,
    userLabel: String,
    assistantLabel: String,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER

    // Phase 3: Thinking indicator logic
    val showThinking = !isUser &&
            connectionState.isConnected &&
            !connectionState.botIsSpeaking &&
            !message.isFinal &&
            message.text.isBlank()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Phase 4: Role labels
            MessageRoleLabel(
                role = message.role,
                isUser = isUser,
                userLabel = userLabel,
                assistantLabel = assistantLabel
            )

            // Message bubble
            Column(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Message text or thinking indicator
                if (showThinking) {
                    ThinkingIndicator()
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Timestamp
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (isUser) androidx.compose.ui.text.style.TextAlign.End
                              else androidx.compose.ui.text.style.TextAlign.Start
                )
            }

            // Speaking indicator for bot
            if (!isUser && connectionState.botIsSpeaking) {
                SpeakingIndicator(
                    isSpeaking = true,
                    isUser = false,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
        }
    }
}

// Phase 4: Role label component
@Composable
private fun MessageRoleLabel(
    role: MessageRole,
    isUser: Boolean,
    userLabel: String,
    assistantLabel: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = if (isUser) userLabel else assistantLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(
            start = if (isUser) 0.dp else 4.dp,
            end = if (isUser) 4.dp else 0.dp
        )
    )
}

// Phase 3: Thinking indicator with animated dots
@Composable
private fun ThinkingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_animation")

    // Animate through values 0, 1, 2, and repeat
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinking_progress"
    )

    // Convert to int for dot count (1, 2, 3)
    val dotCount = (animationProgress.coerceIn(0f, 2.99f).toInt() + 1).coerceIn(1, 3)

    Text(
        text = ".".repeat(dotCount),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
