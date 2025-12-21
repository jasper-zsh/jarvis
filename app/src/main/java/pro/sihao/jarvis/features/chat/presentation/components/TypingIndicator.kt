package pro.sihao.jarvis.features.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val alphaDot1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0, StartOffsetType.FastForward)
        ),
        label = "alphaDot1"
    )

    val alphaDot2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(200, StartOffsetType.FastForward)
        ),
        label = "alphaDot2"
    )

    val alphaDot3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(400, StartOffsetType.FastForward)
        ),
        label = "alphaDot3"
    )

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(alpha = alphaDot1, color = color)
        Dot(alpha = alphaDot2, color = color)
        Dot(alpha = alphaDot3, color = color)
    }
}

@Composable
private fun Dot(
    alpha: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}