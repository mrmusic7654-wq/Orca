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
import androidx.compose.ui.unit.sp
import com.orca.agent.core.CognitiveState
import com.orca.agent.ui.theme.OrcaColors

@Composable
fun OrcaOrb(cognitiveState: CognitiveState, modifier: Modifier = Modifier, onOrbClick: () -> Unit = {}) {
    val inf = rememberInfiniteTransition(label = "orb")
    val pulse by inf.animateFloat(1f, 1.08f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "pulse")
    val glow by inf.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow")

    val orbColor = when (cognitiveState) {
        CognitiveState.IDLE -> OrcaColors.NeonRed.copy(alpha = 0.5f)
        CognitiveState.PLANNING, CognitiveState.LEARNING -> OrcaColors.CyanIntelligence
        CognitiveState.EXECUTING, CognitiveState.AUTONOMOUS -> OrcaColors.NeonRed
        CognitiveState.AWAITING_INPUT, CognitiveState.AWAITING_CONFIRMATION -> OrcaColors.NeonRedPulse
        CognitiveState.ERROR, CognitiveState.RECOVERING -> OrcaColors.ErrorRed
        else -> OrcaColors.NeonRed.copy(alpha = 0.5f)
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(200.dp).scale(pulse)) {
        Canvas(modifier = Modifier.size(220.dp).blur(30.dp)) { drawCircle(orbColor.copy(alpha = glow), size.minDimension / 2) }
        Canvas(modifier = Modifier.size(190.dp)) { drawCircle(Brush.sweepGradient(listOf(orbColor.copy(alpha = 0f), orbColor, orbColor.copy(alpha = 0f))), size.minDimension / 2 - 2.dp.toPx(), style = Stroke(3.dp.toPx())) }
        Canvas(modifier = Modifier.size(160.dp).shadow(20.dp, CircleShape).clip(CircleShape)) { drawCircle(Color.Black); drawCircle(Brush.radialGradient(listOf(orbColor.copy(alpha = 0.3f), Color.Transparent), Offset(size.width * 0.35f, size.height * 0.35f), size.minDimension * 0.55f)); drawCircle(orbColor.copy(alpha = 0.5f), size.minDimension / 2, style = Stroke(1.5.dp.toPx())) }
    }
}
