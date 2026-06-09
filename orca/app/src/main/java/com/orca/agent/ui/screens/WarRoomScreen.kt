// app/src/main/java/com/orca/agent/ui/screens/WarRoomScreen.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orca.agent.core.TaskNode
import com.orca.agent.core.NodeStatus
import com.orca.agent.ui.theme.OrcaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarRoomScreen(
    sessionId: String,
    onNavigateBack: () -> Unit
) {
    // In production, this would be populated from the ViewModel
    val taskNodes = remember { mutableStateListOf<TaskNode>() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "WAR ROOM",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.W300,
                        fontSize = 20.sp,
                        color = OrcaColors.PureWhite,
                        letterSpacing = 6.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = OrcaColors.CoolGrey
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = OrcaColors.VantaBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 3D Topological Map Visualization
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                // Central goal sun
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            OrcaColors.NeonRed,
                            OrcaColors.NeonRed.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    radius = 60.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                
                // Task node orbits
                drawCircle(
                    color = OrcaColors.CyanIntelligence.copy(alpha = 0.1f),
                    radius = 120.dp.toPx(),
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx())
                )
                
                drawCircle(
                    color = OrcaColors.CyanIntelligence.copy(alpha = 0.05f),
                    radius = 200.dp.toPx(),
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            
            // Overlay information
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Stats
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(OrcaColors.NeonRed.copy(alpha = 0.3f), OrcaColors.GlassBorder)
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "TASK CHAIN STATUS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.W600,
                            fontSize = 12.sp,
                            color = OrcaColors.NeonRed,
                            letterSpacing = 3.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem("COMPLETED", "12", OrcaColors.SuccessGreen)
                            StatItem("ACTIVE", "3", OrcaColors.CyanIntelligence)
                            StatItem("PENDING", "5", OrcaColors.WarmGrey)
                            StatItem("FAILED", "0", OrcaColors.ErrorRed)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { 0.6f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = OrcaColors.NeonRed,
                            trackColor = OrcaColors.NeonRedFaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.W600,
            fontSize = 24.sp,
            color = color
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = OrcaColors.WarmGrey,
            letterSpacing = 2.sp
        )
    }
}
