package pro.sihao.jarvis.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AnimatedTypingIndicator(
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_animation")

    // Create animated values for each dot with different delays
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutBack),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0)
        ),
        label = "dot1_scale"
    )

    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutBack),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(200)
        ),
        label = "dot2_scale"
    )

    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutBack),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(400)
        ),
        label = "dot3_scale"
    )

    Row(
        modifier = modifier
            .height(32.dp)
            .width(48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TypingDot(
            scale = dot1Scale,
            baseColor = dotColor,
            activeColor = activeColor
        )
        TypingDot(
            scale = dot2Scale,
            baseColor = dotColor,
            activeColor = activeColor
        )
        TypingDot(
            scale = dot3Scale,
            baseColor = dotColor,
            activeColor = activeColor
        )
    }
}

@Composable
private fun TypingDot(
    scale: Float,
    baseColor: Color,
    activeColor: Color
) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        val animatedColor = remember(scale) {
            // Interpolate color based on scale
            if (scale > 1f) {
                activeColor.copy(alpha = (scale - 1f) * 2f)
            } else {
                baseColor
            }
        }

        Canvas(modifier = Modifier.size(4.dp)) {
            drawCircle(
                color = animatedColor,
                radius = (2.dp.toPx() * scale).coerceAtMost(3.dp.toPx())
            )
        }
    }
}

@Composable
fun EnhancedTypingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "enhanced_typing")
    val primaryColor = MaterialTheme.colorScheme.primary

    val waveAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_animation"
    )

    Row(
        modifier = modifier
            .height(32.dp)
            .width(60.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..2) {
            val delay = i * 0.1f
            val scale = 0.5f + 0.5f * sin((waveAnimation + delay) * 2 * Math.PI.toFloat()).coerceAtLeast(0f)

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(
                        color = primaryColor.copy(
                            alpha = scale.coerceIn(0.3f, 1f)
                        ),
                        radius = (3.dp.toPx() * scale).coerceAtMost(4.dp.toPx())
                    )
                }
            }
        }
    }
}

// Original TypingIndicator for backward compatibility
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    AnimatedTypingIndicator(modifier = modifier)
}