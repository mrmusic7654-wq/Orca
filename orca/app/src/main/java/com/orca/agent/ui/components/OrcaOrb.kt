// app/src/main/java/com/orca/agent/ui/components/OrcaOrb.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
    // Animation values
    val infiniteTransition = rememberInfiniteTransition()
    
    // Pulse animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Rotation for accretion disk
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        )
    )
    
    // Glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val orbColor = when (cognitiveState) {
        CognitiveState.IDLE -> OrcaColors.OrbIdle
        CognitiveState.PLANNING -> OrcaColors.OrbThinking
        CognitiveState.EXECUTING -> OrcaColors.OrbActive
        CognitiveState.AUTONOMOUS -> OrcaColors.OrbActive
        CognitiveState.AWAITING_INPUT -> OrcaColors.OrbInput
        CognitiveState.ERROR -> OrcaColors.ErrorRed
        CognitiveState.SHUTDOWN -> OrcaColors.DimWhite
    }
    
    val stateText = when (cognitiveState) {
        CognitiveState.IDLE -> "IDLE"
        CognitiveState.PLANNING -> "THINKING"
        CognitiveState.EXECUTING -> "EXECUTING"
        CognitiveState.AUTONOMOUS -> "AUTO"
        CognitiveState.AWAITING_INPUT -> "INPUT"
        CognitiveState.ERROR -> "ERROR"
        CognitiveState.SHUTDOWN -> "OFF"
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(200.dp)
            .scale(pulseScale)
    ) {
        // Outer glow
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .blur(30.dp)
        ) {
            drawCircle(
                color = orbColor.copy(alpha = glowAlpha),
                radius = size.minDimension / 2
            )
        }
        
        // Accretion disk ring
        Canvas(
            modifier = Modifier
                .size(190.dp)
                .graphicsLayer { rotationZ = rotation }
        ) {
            val ringWidth = 4.dp.toPx()
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        orbColor.copy(alpha = 0f),
                        orbColor,
                        orbColor.copy(alpha = 0f),
                        orbColor.copy(alpha = 0f)
                    )
                ),
                radius = size.minDimension / 2 - ringWidth / 2,
                style = Stroke(width = ringWidth)
            )
        }
        
        // Main orb sphere
        Canvas(
            modifier = Modifier
                .size(160.dp)
                .shadow(20.dp, CircleShape)
                .clip(CircleShape)
        ) {
            // Deep space background
            drawCircle(color = OrcaColors.VantaBlack)
            
            // Radial gradient for depth
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        orbColor.copy(alpha = 0.3f),
                        orbColor.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.3f, size.height * 0.3f),
                    radius = size.minDimension * 0.6f
                )
            )
            
            // Surface highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.35f, size.height * 0.35f),
                    radius = size.minDimension * 0.4f
                )
            )
            
            // Edge glow
            drawCircle(
                color = orbColor.copy(alpha = 0.5f),
                radius = size.minDimension / 2,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        // State text
        if (cognitiveState != CognitiveState.IDLE) {
            Text(
                text = stateText,
                fontFamily = FontFamily.Monospace,
                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = orbColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 180.dp)
            )
        }
        
        // Crackling energy lines when executing
        if (cognitiveState == CognitiveState.EXECUTING) {
            EnergyLines(orbColor)
        }
    }
}

@Composable
fun EnergyLines(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animated energy lines
    Canvas(modifier = Modifier.size(200.dp)) {
        val lineCount = 5
        for (i in 0 until lineCount) {
            val angle = (i * 72f + infiniteTransition.currentValue * 360f) % 360f
            val radian = Math.toRadians(angle.toDouble())
            
            val startX = size.width / 2 + (80.dp.toPx() * kotlin.math.cos(radian)).toFloat()
            val startY = size.height / 2 + (80.dp.toPx() * kotlin.math.sin(radian)).toFloat()
            val endX = size.width / 2 + (110.dp.toPx() * kotlin.math.cos(radian)).toFloat()
            val endY = size.height / 2 + (110.dp.toPx() * kotlin.math.sin(radian)).toFloat()
            
            drawLine(
                color = color.copy(alpha = 0.6f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1.5f
            )
        }
    }
}
