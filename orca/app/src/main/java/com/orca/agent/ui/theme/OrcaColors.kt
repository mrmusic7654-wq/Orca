// app/src/main/java/com/orca/agent/ui/theme/OrcaColors.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.theme

import androidx.compose.ui.graphics.Color

// Abyssal Neon Color System
object OrcaColors {
    // Primary Backgrounds
    val AbyssBlack = Color(0xFF0A0B0F)
    val VantaBlack = Color(0xFF000100)
    val DeepSpace = Color(0xFF111318)
    val VoidBlack = Color(0xFF08090C)
    
    // Neon Accents
    val NeonRed = Color(0xFFFF3A2D)
    val NeonRedPulse = Color(0xFFFF6B4A)
    val NeonRedDim = Color(0xCCFF3A2D)
    val NeonRedFaint = Color(0x33FF3A2D)
    
    // Cyan Intelligence
    val CyanIntelligence = Color(0xFF00E5FF)
    val CyanThinking = Color(0xFF00B8D4)
    val CyanFaint = Color(0x3300E5FF)
    
    // Text
    val PureWhite = Color(0xFFFFFFFF)
    val CoolGrey = Color(0xFFAEB5C0)
    val WarmGrey = Color(0xFF8A8F98)
    val DimWhite = Color(0x99FFFFFF)
    
    // Status
    val SuccessGreen = Color(0xFF00E676)
    val WarningOrange = Color(0xFFFF9100)
    val ErrorRed = Color(0xFFFF1744)
    
    // Glass Morphism
    val GlassDark = Color(0x1AFFFFFF)
    val GlassBorder = Color(0x33FFFFFF)
    val GlassHover = Color(0x22FFFFFF)
    
    // Orb States
    val OrbIdle = NeonRed.copy(alpha = 0.6f)
    val OrbActive = NeonRed
    val OrbThinking = CyanIntelligence
    val OrbAlert = NeonRedPulse
    val OrbInput = Color(0xFFFF6B4A)
}
