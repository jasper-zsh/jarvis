package pro.sihao.jarvis.ui.components.realtime

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.sihao.jarvis.domain.model.ChatMode

@Composable
fun ChatModeSelector(
    currentMode: ChatMode,
    onModeSelected: (ChatMode) -> Unit,
    modifier: Modifier = Modifier,
    glassesConnected: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Chat Mode",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ChatMode.values().forEach { mode ->
                ModeTab(
                    mode = mode,
                    isSelected = mode == currentMode,
                    isAvailable = mode != ChatMode.GLASSES || glassesConnected,
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModeTab(
    mode: ChatMode,
    isSelected: Boolean,
    isAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = tween(
            durationMillis = 200,
            easing = EaseInOutCubic
        ),
        label = "background_color_animation"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            if (isAvailable) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
        },
        animationSpec = tween(
            durationMillis = 200,
            easing = EaseInOutCubic
        ),
        label = "content_color_animation"
    )

    val icon = when (mode) {
        ChatMode.TEXT -> Icons.Default.Chat
        ChatMode.REALTIME -> Icons.Default.Phone
        ChatMode.GLASSES -> Icons.Default.Headset
    }
    
    val label = when (mode) {
        ChatMode.TEXT -> "Text"
        ChatMode.REALTIME -> "Real-time"
        ChatMode.GLASSES -> "Glasses"
    }

    Surface(
        onClick = if (isAvailable) onClick else { {} },
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .let { if (isAvailable) it.clickable { onClick() } else it },
        color = backgroundColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$label mode",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )

            if (!isAvailable) {
                Text(
                    text = "Unavailable",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun CompactChatModeSelector(
    currentMode: ChatMode,
    onModeSelected: (ChatMode) -> Unit,
    modifier: Modifier = Modifier,
    glassesConnected: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Current mode button
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (currentMode) {
                        ChatMode.TEXT -> Icons.Default.Chat
                        ChatMode.REALTIME -> Icons.Default.Phone
                        ChatMode.GLASSES -> Icons.Default.Headset
                    },
                    contentDescription = "Current mode",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = when (currentMode) {
                        ChatMode.TEXT -> "Text"
                        ChatMode.REALTIME -> "Real-time"
                        ChatMode.GLASSES -> "Glasses"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ChatMode.values().forEach { mode ->
                val isAvailable = mode != ChatMode.GLASSES || glassesConnected
                
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    ChatMode.TEXT -> Icons.Default.Chat
                                    ChatMode.REALTIME -> Icons.Default.Phone
                                    ChatMode.GLASSES -> Icons.Default.Headset
                                },
                                contentDescription = null,
                                tint = if (isAvailable) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )

                            Text(
                                text = when (mode) {
                                    ChatMode.TEXT -> "Text Chat"
                                    ChatMode.REALTIME -> "Real-time Voice"
                                    ChatMode.GLASSES -> "Glasses Mode"
                                }
                            )

                            if (mode == currentMode) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        if (isAvailable) {
                            onModeSelected(mode)
                            expanded = false
                        }
                    },
                    enabled = isAvailable
                )
            }
        }
    }
}

@Composable
fun ModeTransitionIndicator(
    fromMode: ChatMode,
    toMode: ChatMode,
    isTransitioning: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isTransitioning) return

    val infiniteTransition = rememberInfiniteTransition(label = "mode_transition_animation")
    
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "transition_progress"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // From mode icon (fading out)
        Icon(
            imageVector = when (fromMode) {
                ChatMode.TEXT -> Icons.Default.Chat
                ChatMode.REALTIME -> Icons.Default.Phone
                ChatMode.GLASSES -> Icons.Default.Headset
            },
            contentDescription = "Previous mode",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 1f - progress),
            modifier = Modifier.size(20.dp)
        )

        // Loading animation
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )

        // To mode icon (fading in)
        Icon(
            imageVector = when (toMode) {
                ChatMode.TEXT -> Icons.Default.Chat
                ChatMode.REALTIME -> Icons.Default.Phone
                ChatMode.GLASSES -> Icons.Default.Headset
            },
            contentDescription = "New mode",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = progress),
            modifier = Modifier.size(20.dp)
        )
    }
}