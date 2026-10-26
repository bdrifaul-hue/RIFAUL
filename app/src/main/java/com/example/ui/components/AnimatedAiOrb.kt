package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldListening
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.PurpleGlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class OrbVisualState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

@Composable
fun AnimatedAiOrb(
    modifier: Modifier = Modifier,
    state: OrbVisualState = OrbVisualState.IDLE,
    audioRmsDb: Float = 0f,
    size: Dp = 180.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    // Slow continuous rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbVisualState.THINKING -> 3000
                    OrbVisualState.LISTENING -> 5000
                    OrbVisualState.SPEAKING -> 4000
                    else -> 10000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Breathing pulse
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbVisualState.THINKING -> 700
                    OrbVisualState.LISTENING -> 400
                    OrbVisualState.SPEAKING -> 600
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbPulse"
    )

    // Wave ripple phase
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbWavePhase"
    )

    // Dynamic scale expansion when receiving mic audio amplitude
    val micBoost = if (state == OrbVisualState.LISTENING) {
        (audioRmsDb.coerceIn(0f, 15f) / 15f) * 0.25f
    } else {
        0f
    }
    val effectiveScale = pulse + micBoost

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.65f

            // Color scheme based on state
            val (coreColor, outerColor, glowColor) = when (state) {
                OrbVisualState.LISTENING -> Triple(
                    EmeraldListening,
                    NeonCyan,
                    Color(0xFF10B981)
                )
                OrbVisualState.THINKING -> Triple(
                    PurpleGlow,
                    NeonCyan,
                    ElectricViolet
                )
                OrbVisualState.SPEAKING -> Triple(
                    NeonCyan,
                    ElectricViolet,
                    PurpleGlow
                )
                OrbVisualState.ERROR -> Triple(
                    NeonPink,
                    Color(0xFFE11D48),
                    Color(0xFF9F1239)
                )
                OrbVisualState.IDLE -> Triple(
                    NeonCyan,
                    ElectricViolet,
                    CyanGlow
                )
            }

            // Outer atmospheric aura glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.45f * effectiveScale),
                        coreColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.55f * effectiveScale
                ),
                radius = baseRadius * 1.55f * effectiveScale,
                center = center
            )

            // Dynamic harmonic waveform rings when speaking or listening
            if (state == OrbVisualState.SPEAKING || state == OrbVisualState.LISTENING) {
                val path = Path()
                val wavePoints = 36
                for (i in 0..wavePoints) {
                    val angle = (i.toFloat() / wavePoints) * (2 * PI).toFloat()
                    val waveAmp = if (state == OrbVisualState.SPEAKING) 12f else 8f + micBoost * 20f
                    val r = (baseRadius * 1.15f) + sin(angle * 5f + wavePhase) * waveAmp
                    val x = center.x + r * cos(angle)
                    val y = center.y + r * sin(angle)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.sweepGradient(
                        colors = listOf(NeonCyan, ElectricViolet, EmeraldListening, NeonCyan),
                        center = center
                    ),
                    style = Stroke(width = 3.5f)
                )
            }

            // Orbital Ring 1 (Rotates clockwise)
            rotate(rotation, pivot = center) {
                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(coreColor, outerColor, Color.Transparent, coreColor),
                        center = center
                    ),
                    topLeft = Offset(center.x - baseRadius * 1.25f, center.y - baseRadius * 0.45f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 2.5f, baseRadius * 0.9f),
                    style = Stroke(width = 2.5f)
                )
            }

            // Orbital Ring 2 (Rotates counter-clockwise)
            rotate(-rotation * 0.8f + 45f, pivot = center) {
                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(outerColor, glowColor, Color.Transparent, outerColor),
                        center = center
                    ),
                    topLeft = Offset(center.x - baseRadius * 0.45f, center.y - baseRadius * 1.25f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 0.9f, baseRadius * 2.5f),
                    style = Stroke(width = 2f)
                )
            }

            // Core Energy Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        coreColor.copy(alpha = 0.9f),
                        outerColor.copy(alpha = 0.75f),
                        Color(0xFF070A13).copy(alpha = 0.9f)
                    ),
                    center = center,
                    radius = baseRadius * effectiveScale
                ),
                radius = baseRadius * effectiveScale,
                center = center
            )

            // Inner high-tech specular reflection
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    center = Offset(center.x - baseRadius * 0.35f, center.y - baseRadius * 0.35f),
                    radius = baseRadius * 0.45f
                ),
                radius = baseRadius * 0.45f,
                center = Offset(center.x - baseRadius * 0.35f, center.y - baseRadius * 0.35f)
            )
        }
    }
}
