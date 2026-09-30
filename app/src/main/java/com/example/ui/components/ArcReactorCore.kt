package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.JarvisStatus
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.StarkGold
import com.example.ui.theme.StarkRedAlert
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorCore(
    status: JarvisStatus,
    audioAmplitude: Float,
    isSpeaking: Boolean,
    isListening: Boolean,
    onCoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorRotation")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )

    val coreGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreGlow"
    )

    val primaryColor = when (status) {
        JarvisStatus.LISTENING -> CyberCyan
        JarvisStatus.THINKING -> StarkGold
        JarvisStatus.SPEAKING -> ElectricBlue
        JarvisStatus.EXECUTING -> Color(0xFF00E5FF)
        JarvisStatus.IDLE -> CyberCyan
    }

    val dynamicScale = when {
        isSpeaking || isListening -> (1.0f + (audioAmplitude * 0.28f)).coerceIn(0.95f, 1.35f)
        else -> idlePulse
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCoreClick
                )
                .testTag("arc_reactor_core"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2f) - 12.dp.toPx()

                // 1. Ambient outer glow circle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = coreGlowAlpha * 0.45f),
                            primaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.15f
                    ),
                    center = center,
                    radius = baseRadius * 1.15f
                )

                // 2. Outer Rotating Tech Ring with Ticks
                rotate(outerRotation, pivot = center) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.35f),
                        center = center,
                        radius = baseRadius,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Draw 24 tick marks on outer ring
                    val tickCount = 24
                    for (i in 0 until tickCount) {
                        val angleDeg = (i * 360f / tickCount)
                        val angleRad = (angleDeg * PI / 180f).toFloat()
                        val isMajor = i % 4 == 0
                        val tickLen = if (isMajor) 9.dp.toPx() else 4.dp.toPx()
                        val r1 = baseRadius - tickLen
                        val r2 = baseRadius

                        val start = Offset(
                            center.x + (r1 * cos(angleRad)),
                            center.y + (r1 * sin(angleRad))
                        )
                        val end = Offset(
                            center.x + (r2 * cos(angleRad)),
                            center.y + (r2 * sin(angleRad))
                        )

                        drawLine(
                            color = if (isMajor) primaryColor else primaryColor.copy(alpha = 0.4f),
                            start = start,
                            end = end,
                            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // 3. Middle Counter-Rotating Segmented Ring
                rotate(innerRotation, pivot = center) {
                    val midRadius = baseRadius * 0.78f
                    val segmentCount = 6
                    val arcSweep = 44f
                    for (i in 0 until segmentCount) {
                        val startAngle = (i * 60f) + 8f
                        drawArc(
                            color = primaryColor.copy(alpha = 0.75f),
                            startAngle = startAngle,
                            sweepAngle = arcSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - midRadius, center.y - midRadius),
                            size = androidx.compose.ui.geometry.Size(midRadius * 2, midRadius * 2),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // 4. Inner Concentric Pulsing Reactor Core
                val coreRadius = baseRadius * 0.52f * dynamicScale

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            primaryColor.copy(alpha = 0.85f),
                            primaryColor.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = coreRadius
                    ),
                    center = center,
                    radius = coreRadius
                )

                // 5. High-Tech Hexagonal Coils inside Arc Core
                val coilRadius = coreRadius * 0.65f
                val hexPath = Path()
                for (i in 0 until 6) {
                    val angle = (i * 60.0 * PI / 180.0)
                    val x = center.x + (coilRadius * cos(angle)).toFloat()
                    val y = center.y + (coilRadius * sin(angle)).toFloat()
                    if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                }
                hexPath.close()

                drawPath(
                    path = hexPath,
                    color = primaryColor,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 6. Central Triangular Crystal
                val triRadius = coilRadius * 0.55f
                val triPath = Path()
                for (i in 0 until 3) {
                    val angle = ((i * 120.0 - 90.0) * PI / 180.0)
                    val x = center.x + (triRadius * cos(angle)).toFloat()
                    val y = center.y + (triRadius * sin(angle)).toFloat()
                    if (i == 0) triPath.moveTo(x, y) else triPath.lineTo(x, y)
                }
                triPath.close()

                drawPath(
                    path = triPath,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status badge
        val statusLabel = when (status) {
            JarvisStatus.LISTENING -> "JARVIS // LISTENING (SPEAK NOW)"
            JarvisStatus.THINKING -> "JARVIS // NEURAL PROCESSING"
            JarvisStatus.SPEAKING -> "JARVIS // VOCAL TRANSMISSION"
            JarvisStatus.EXECUTING -> "JARVIS // PROTOCOL EXECUTING"
            JarvisStatus.IDLE -> "JARVIS CORE // STANDING BY (TAP TO TALK)"
        }

        Text(
            text = statusLabel,
            color = primaryColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )
    }
}
