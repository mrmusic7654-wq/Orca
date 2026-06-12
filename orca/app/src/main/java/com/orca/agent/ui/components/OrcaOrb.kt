package com.orca.agent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orca.agent.core.CognitiveState
import com.orca.agent.ui.theme.OrcaColors

@Composable
fun OrcaOrb(
    cognitiveState: CognitiveState,
    modifier: Modifier = Modifier,
    onOrbClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val pulseScale by infiniteTransition.animateFloat(1f, 1.08f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(0.4f, 0.8f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow")

    val orbColor = when (cognitiveState) {
        CognitiveState.IDLE -> OrcaColors.OrbIdle
        CognitiveState.PLANNING, CognitiveState.LEARNING -> OrcaColors.OrbThinking
        CognitiveState.EXECUTING, CognitiveState.AUTONOMOUS -> OrcaColors.OrbActive
        CognitiveState.AWAITING_CONFIRMATION -> OrcaColors.OrbInput
        CognitiveState.ERROR, CognitiveState.RECOVERING -> OrcaColors.ErrorRed
        CognitiveState.SHUTDOWN -> OrcaColors.DimWhite
        else -> OrcaColors.OrbIdle
    }

    val stateText = when (cognitiveState) {
        CognitiveState.IDLE -> "IDLE"
        CognitiveState.PLANNING -> "THINK"
        CognitiveState.EXECUTING -> "EXEC"
        CognitiveState.AUTONOMOUS -> "AUTO"
        CognitiveState.AWAITING_CONFIRMATION -> "INPUT"
        CognitiveState.ERROR -> "ERR"
        else -> ""
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.scale(pulseScale)) {
        Canvas(modifier = Modifier.size(220.dp).blur(30.dp)) {
            drawCircle(orbColor.copy(alpha = glowAlpha), size.minDimension / 2)
        }
        Canvas(modifier = Modifier.size(190.dp)) {
            drawCircle(Brush.sweepGradient(listOf(orbColor.copy(alpha = 0f), orbColor, orbColor.copy(alpha = 0f))), size.minDimension / 2 - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
        }
        Canvas(modifier = Modifier.size(160.dp).shadow(20.dp, CircleShape).clip(CircleShape)) {
            drawCircle(Color.Black)
            drawCircle(Brush.radialGradient(listOf(orbColor.copy(alpha = 0.3f), Color.Transparent), Offset(size.width * 0.3f, size.height * 0.3f), size.minDimension * 0.6f))
            drawCircle(orbColor.copy(alpha = 0.5f), size.minDimension / 2, style = Stroke(2.dp.toPx()))
        }
        if (cognitiveState != CognitiveState.IDLE) {
            Text(stateText, fontFamily = FontFamily.Monospace, color = orbColor, textAlign = TextAlign.Center)
        }
    }
}
