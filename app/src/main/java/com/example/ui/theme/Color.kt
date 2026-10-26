package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Futuristic Cyber Theme Palette
val CyberDarkBg = Color(0xFF070A13)
val CyberSurface = Color(0xFF0E1626)
val CyberSurfaceElevated = Color(0xFF162238)
val CyberCardBorder = Color(0xFF1E2F4D)

// Neon & Energy Colors
val NeonCyan = Color(0xFF00F2FE)
val CyanGlow = Color(0xFF38BDF8)
val ElectricViolet = Color(0xFF8B5CF6)
val PurpleGlow = Color(0xFFA855F7)
val NeonPink = Color(0xFFF43F5E)
val EmeraldListening = Color(0xFF10B981)

// Neutral Text
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Gradients
val OrbGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFF00F2FE),
        Color(0xFF8B5CF6),
        Color(0xFF4338CA),
        Color(0x00070A13)
    )
)

val CardGlowBorder = Brush.linearGradient(
    colors = listOf(
        Color(0xFF00F2FE).copy(alpha = 0.6f),
        Color(0xFF8B5CF6).copy(alpha = 0.6f)
    )
)

val HeaderGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF00F2FE),
        Color(0xFFA855F7)
    )
)
