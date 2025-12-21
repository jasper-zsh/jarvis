package pro.sihao.jarvis.features.realtime.presentation.components.realtime

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState

@Composable
fun BotIndicator(
    connectionState: PipeCatConnectionState,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BotStatusIndicator(
            connectionState = connectionState,
            modifier = Modifier.size(64.dp)
        )

        if (showDetails) {
            BotStatusText(
                connectionState = connectionState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BotStatusIndicator(
    connectionState: PipeCatConnectionState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bot_status_animation")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_animation"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_animation"
    )

    val backgroundColor = when {
        connectionState.isConnecting -> MaterialTheme.colorScheme.surfaceVariant
        connectionState.isConnected && connectionState.botReady -> MaterialTheme.colorScheme.primaryContainer
        connectionState.isConnected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val icon = when {
        connectionState.isConnecting -> Icons.Default.Sync
        connectionState.isConnected && connectionState.botReady -> {
            if (connectionState.botIsSpeaking) Icons.Default.RecordVoiceOver else Icons.Default.SmartToy
        }
        connectionState.isConnected -> Icons.Default.Settings
        else -> Icons.Default.PowerOff
    }
    
    val iconColor = when {
        connectionState.isConnecting -> MaterialTheme.colorScheme.onSurfaceVariant
        connectionState.isConnected && connectionState.botReady -> MaterialTheme.colorScheme.onPrimaryContainer
        connectionState.isConnected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val showAnimation = when {
        connectionState.isConnecting -> true
        connectionState.isConnected && connectionState.botReady -> connectionState.botIsSpeaking
        connectionState.isConnected -> true
        else -> false
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (showAnimation && connectionState.isConnecting) {
            androidx.compose.material.icons.Icons.Default.Sync
        }
        
        Icon(
            imageVector = icon,
            contentDescription = "Bot status",
            tint = iconColor,
            modifier = Modifier
                .size(32.dp)
                .let { if (showAnimation && connectionState.isConnecting) it else it }
        )

        // Add speaking indicator
        if (connectionState.botIsSpeaking) {
            SpeakingIndicator(
                isSpeaking = true,
                isUser = false,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
            )
        }
    }
}

@Composable
fun BotStatusText(
    connectionState: PipeCatConnectionState,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when {
        connectionState.isConnecting -> {
            "Connecting..." to MaterialTheme.colorScheme.primary
        }
        connectionState.errorMessage != null -> {
            "Error: ${connectionState.errorMessage}" to MaterialTheme.colorScheme.error
        }
        !connectionState.isConnected -> {
            "Disconnected" to MaterialTheme.colorScheme.onSurfaceVariant
        }
        !connectionState.botReady -> {
            "Bot initializing..." to MaterialTheme.colorScheme.secondary
        }
        connectionState.botIsSpeaking -> {
            "Bot is speaking..." to MaterialTheme.colorScheme.primary
        }
        else -> {
            "Bot ready" to MaterialTheme.colorScheme.primary
        }
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.bodySmall,
        color = statusColor,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
fun CompactBotIndicator(
    connectionState: PipeCatConnectionState,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, icon, iconColor) = when {
        connectionState.isConnecting -> {
            Triple(
                MaterialTheme.colorScheme.surfaceVariant,
                Icons.Default.Sync,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        connectionState.isConnected && connectionState.botReady -> {
            Triple(
                MaterialTheme.colorScheme.primaryContainer,
                if (connectionState.botIsSpeaking) Icons.Default.RecordVoiceOver else Icons.Default.SmartToy,
                MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        else -> {
            Triple(
                MaterialTheme.colorScheme.surfaceVariant,
                Icons.Default.PowerOff,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Surface(
        modifier = modifier.size(40.dp),
        shape = CircleShape,
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "Bot status",
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BotCapabilities(
    capabilities: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Bot Capabilities",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        capabilities.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { capability ->
                    CapabilityChip(
                        capability = capability,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityChip(
    capability: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = capability,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ConnectionQualityIndicator(
    quality: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    val qualityColor = when {
        quality >= 0.8f -> MaterialTheme.colorScheme.primary
        quality >= 0.5f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    val qualityText = when {
        quality >= 0.8f -> "Excellent"
        quality >= 0.6f -> "Good"
        quality >= 0.4f -> "Fair"
        else -> "Poor"
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                quality >= 0.8f -> Icons.Default.Wifi
                quality >= 0.6f -> Icons.Default.Wifi
                quality >= 0.4f -> Icons.Default.Wifi
                quality >= 0.2f -> Icons.Default.Wifi
                else -> Icons.Default.SignalWifiOff
            },
            contentDescription = "Connection quality",
            tint = qualityColor,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = qualityText,
            style = MaterialTheme.typography.labelSmall,
            color = qualityColor,
            fontWeight = FontWeight.Medium
        )
    }
}