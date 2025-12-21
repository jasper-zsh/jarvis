package pro.sihao.jarvis.features.realtime.presentation.components.realtime

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun AudioIndicator(
    audioLevel: Float,
    isActive: Boolean,
    isUser: Boolean = true,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val animatedAudioLevel by animateFloatAsState(
        targetValue = if (isActive) audioLevel else 0f,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        ),
        label = "audio_level_animation"
    )

    val primaryColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                drawAudioWaveform(
                    audioLevel = animatedAudioLevel,
                    isActive = isActive,
                    color = primaryColor
                )
            }
        }

        if (showLabel) {
            Text(
                text = if (isUser) "You" else "Bot",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompactAudioIndicator(
    audioLevel: Float,
    isActive: Boolean,
    isUser: Boolean = true,
    modifier: Modifier = Modifier
) {
    val animatedAudioLevel by animateFloatAsState(
        targetValue = if (isActive) audioLevel else 0f,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        ),
        label = "compact_audio_level_animation"
    )

    val primaryColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (isActive) {
                    primaryColor.copy(alpha = 0.2f + animatedAudioLevel * 0.8f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(
                    color = primaryColor.copy(alpha = animatedAudioLevel),
                    radius = animatedAudioLevel * 6.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun AudioLevelBars(
    audioLevel: Float,
    isActive: Boolean,
    isUser: Boolean = true,
    modifier: Modifier = Modifier,
    barCount: Int = 5
) {
    val animatedAudioLevel by animateFloatAsState(
        targetValue = if (isActive) audioLevel else 0f,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        ),
        label = "audio_bars_animation"
    )

    val primaryColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(barCount) { index ->
            val barHeight = (index + 1) * (animatedAudioLevel / barCount)
            val alpha = if (barHeight > 0.1f) 1f else 0.3f
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp * barHeight)
                    .background(
                        color = primaryColor.copy(alpha = alpha),
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}

private fun DrawScope.drawAudioWaveform(
    audioLevel: Float,
    isActive: Boolean,
    color: Color
) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val maxRadius = min(centerX, centerY) * 0.8f

    if (isActive && audioLevel > 0.1f) {
        // Draw expanding circles based on audio level
        val circles = (1..3).map { index ->
            val radius = maxRadius * (audioLevel * (index / 3f))
            val alpha = (1f - index / 3f) * audioLevel
            color.copy(alpha = alpha)
        }

        circles.forEach { circleColor ->
            drawCircle(
                color = circleColor,
                radius = maxRadius * audioLevel,
                center = Offset(centerX, centerY)
            )
        }
    } else {
        // Draw a simple circle when not active
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = maxRadius * 0.3f,
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
fun SpeakingIndicator(
    isSpeaking: Boolean,
    isUser: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speaking_animation")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_alpha"
    )

    val primaryColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(
                if (isSpeaking) {
                    primaryColor.copy(alpha = alpha)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
    )
}