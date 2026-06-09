// app/src/main/java/com/orca/agent/ui/theme/AbyssalNeonTheme.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = OrcaColors.NeonRed,
    secondary = OrcaColors.CyanIntelligence,
    tertiary = OrcaColors.NeonRedPulse,
    background = OrcaColors.VantaBlack,
    surface = OrcaColors.AbyssBlack,
    onPrimary = OrcaColors.PureWhite,
    onSecondary = OrcaColors.VantaBlack,
    onTertiary = OrcaColors.PureWhite,
    onBackground = OrcaColors.PureWhite,
    onSurface = OrcaColors.CoolGrey,
    error = OrcaColors.ErrorRed,
    outline = OrcaColors.GlassBorder
)

@Composable
fun AbyssalNeonTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = OrcaTypography,
        content = content
    )
}

val OrcaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.W300,
        fontSize = 57.sp,
        color = OrcaColors.PureWhite
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 32.sp,
        color = OrcaColors.PureWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.W500,
        fontSize = 28.sp,
        color = OrcaColors.CyanIntelligence
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 22.sp,
        color = OrcaColors.PureWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = OrcaColors.CoolGrey
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = OrcaColors.CyanIntelligence
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        color = OrcaColors.NeonRed
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        color = OrcaColors.WarmGrey
    )
)
