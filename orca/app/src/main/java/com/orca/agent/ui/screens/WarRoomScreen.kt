// ============================================================
// WarRoomScreen.kt - FULL FEATURES
// Path: app/src/main/java/com/orca/agent/ui/screens/WarRoomScreen.kt
// ============================================================

package com.orca.agent.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orca.agent.core.NodeStatus
import com.orca.agent.core.TaskNode
import com.orca.agent.ui.theme.OrcaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarRoomScreen(sessionId: String, onNavigateBack: () -> Unit) {
    val taskNodes = remember { mutableStateListOf<TaskNode>() }
    var selectedNode by remember { mutableStateOf<TaskNode?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "WAR ROOM",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.W300,
                        fontSize = 20.sp,
                        color = OrcaColors.PureWhite,
                        letterSpacing = 6.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = OrcaColors.CoolGrey)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelp = !showHelp }) {
                        Icon(Icons.Default.Help, "Help", tint = OrcaColors.CyanIntelligence)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = OrcaColors.VantaBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Background: 3D Topological Map
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2

                // Central goal sun
                drawCircle(
                    Brush.radialGradient(
                        colors = listOf(
                            OrcaColors.NeonRed.copy(alpha = 0.6f),
                            OrcaColors.NeonRed.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    radius = 80.dp.toPx(),
                    center = Offset(cx, cy)
                )

                // Orbit rings
                for (i in 1..3) {
                    drawCircle(
                        color = OrcaColors.CyanIntelligence.copy(alpha = 0.05f * (4 - i)),
                        radius = (80 + i * 60).dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = (1.5f - i * 0.3f).dp.toPx())
                    )
                }
            }

            // Task timeline
            Column(modifier = Modifier.fillMaxSize()) {
                // Stats header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "TASK CHAIN STATUS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.W600,
                            fontSize = 12.sp,
                            color = OrcaColors.NeonRed,
                            letterSpacing = 3.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val completed = taskNodes.count { it.status == NodeStatus.VERIFIED }
                        val failed = taskNodes.count { it.status == NodeStatus.FAILED }
                        val inProgress = taskNodes.count { it.status == NodeStatus.IN_PROGRESS }
                        val pending = taskNodes.count { it.status == NodeStatus.PENDING }
                        val total = taskNodes.size

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBadge("$completed/$total", "DONE", OrcaColors.SuccessGreen)
                            StatBadge("$inProgress", "ACTIVE", OrcaColors.CyanIntelligence)
                            StatBadge("$pending", "PENDING", OrcaColors.WarmGrey)
                            StatBadge("$failed", "FAILED", OrcaColors.ErrorRed)
                        }

                        if (total > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { completed.toFloat() / total.toFloat() },
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

                // Node list
                if (taskNodes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AccountTree,
                                null,
                                tint = OrcaColors.WarmGrey.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No active task chain",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = OrcaColors.CoolGrey
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(taskNodes) { node ->
                            TimelineNode(
                                node = node,
                                isSelected = selectedNode?.id == node.id,
                                isActive = node.status == NodeStatus.IN_PROGRESS,
                                onClick = { selectedNode = node }
                            )
                        }
                    }
                }
            }

            // Help overlay
            AnimatedVisibility(
                visible = showHelp,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OrcaColors.VantaBlack.copy(alpha = 0.9f))
                        .clickable { showHelp = false }
                ) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                "WAR ROOM GUIDE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.W600,
                                fontSize = 18.sp,
                                color = OrcaColors.NeonRed,
                                letterSpacing = 3.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HelpRow("🟢 Green", "Completed successfully")
                            HelpRow("🔵 Cyan", "Currently executing")
                            HelpRow("⚪ Grey", "Pending execution")
                            HelpRow("🔴 Red", "Failed - needs attention")
                            HelpRow("💫 Central orb", "Overall task goal")
                            HelpRow("🔄 Orbit rings", "Task complexity layers")
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Tap any node for details. Drag to reorder. Failed nodes can be retried or skipped.",
                                fontSize = 12.sp,
                                color = OrcaColors.CoolGrey
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineNode(
    node: TaskNode,
    isSelected: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (node.status) {
        NodeStatus.VERIFIED -> OrcaColors.SuccessGreen
        NodeStatus.IN_PROGRESS -> OrcaColors.CyanIntelligence
        NodeStatus.FAILED -> OrcaColors.ErrorRed
        NodeStatus.NEEDS_RECOVERY -> OrcaColors.WarningOrange
        else -> OrcaColors.WarmGrey
    }

    val statusIcon = when (node.status) {
        NodeStatus.VERIFIED -> Icons.Default.CheckCircle
        NodeStatus.IN_PROGRESS -> Icons.Default.Timelapse
        NodeStatus.FAILED -> Icons.Default.Cancel
        NodeStatus.NEEDS_RECOVERY -> Icons.Default.Warning
        NodeStatus.SKIPPED -> Icons.Default.SkipNext
        else -> Icons.Default.RadioButtonUnchecked
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .background(
                if (isSelected) OrcaColors.NeonRed.copy(alpha = 0.1f)
                else OrcaColors.AbyssBlack.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Status dot with icon
        Box(
            modifier = Modifier
                .size(if (isActive) 28.dp else 24.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                statusIcon,
                contentDescription = node.status.name,
                tint = statusColor,
                modifier = Modifier.size(if (isActive) 16.dp else 14.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.description,
                fontFamily = FontFamily.Default,
                fontWeight = if (isActive) FontWeight.W600 else FontWeight.Normal,
                fontSize = 13.sp,
                color = if (isActive) OrcaColors.PureWhite else OrcaColors.CoolGrey,
                textDecoration = if (node.status == NodeStatus.VERIFIED) TextDecoration.LineThrough
                else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (node.requiresConfirmation) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        tint = OrcaColors.NeonRedPulse,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "HUMAN CHECKPOINT REQUIRED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = OrcaColors.NeonRedPulse,
                        letterSpacing = 1.sp
                    )
                }
            }

            if (node.retryCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Retries: ${node.retryCount}/${node.maxRetries}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = OrcaColors.WarningOrange
                )
            }
        }

        // Action type badge
        val actionLabel = when {
            node.action is com.orca.agent.core.AgentAction.Tap -> "TAP"
            node.action is com.orca.agent.core.AgentAction.Swipe -> "SWIPE"
            node.action is com.orca.agent.core.AgentAction.Type -> "TYPE"
            node.action is com.orca.agent.core.AgentAction.LongPress -> "HOLD"
            node.action is com.orca.agent.core.AgentAction.Back -> "BACK"
            node.action is com.orca.agent.core.AgentAction.Home -> "HOME"
            node.action is com.orca.agent.core.AgentAction.AppAction -> "APP"
            node.action is com.orca.agent.core.AgentAction.ScreenshotToGemini -> "AI"
            node.action is com.orca.agent.core.AgentAction.WaitForUserConfirmation -> "WAIT"
            else -> "ACT"
        }

        Box(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = actionLabel,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = statusColor,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun StatBadge(count: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.W600,
            fontSize = 18.sp,
            color = color
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = OrcaColors.WarmGrey,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun HelpRow(icon: String, description: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(description, fontSize = 12.sp, color = OrcaColors.CoolGrey)
    }
}
