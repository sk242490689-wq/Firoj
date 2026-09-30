package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = CyberCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
            val barCount = 28
            val totalWidth = size.width
            val barWidth = 4.dp.toPx()
            val spacing = (totalWidth - (barCount * barWidth)) / (barCount + 1)
            val centerY = size.height / 2f

            for (i in 0 until barCount) {
                val x = spacing + i * (barWidth + spacing)
                val waveFactor = sin(phase + (i * 0.45f)).toFloat()
                val minHeight = 4.dp.toPx()
                val dynamicHeight = if (isActive) {
                    val ampMultiplier = (amplitude * 24.dp.toPx()).coerceAtLeast(6.dp.toPx())
                    (minHeight + (waveFactor * 0.5f + 0.5f) * ampMultiplier).coerceIn(4.dp.toPx(), 30.dp.toPx())
                } else {
                    minHeight
                }

                val top = centerY - (dynamicHeight / 2f)
                val barAlpha = if (isActive) 0.9f else 0.25f

                drawRoundRect(
                    color = if (i % 2 == 0) barColor.copy(alpha = barAlpha) else ElectricBlue.copy(alpha = barAlpha),
                    topLeft = Offset(x, top),
                    size = Size(barWidth, dynamicHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}
