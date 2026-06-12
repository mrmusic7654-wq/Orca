package com.orca.agent.ui.screens

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WAR ROOM", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W300, fontSize = 20.sp, color = OrcaColors.PureWhite, letterSpacing = 6.sp) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = OrcaColors.CoolGrey) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.9f))
            )
        },
        containerColor = OrcaColors.VantaBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2; val cy = size.height / 2
                drawCircle(Brush.radialGradient(listOf(OrcaColors.NeonRed.copy(alpha = 0.6f), Color.Transparent)), 80.dp.toPx(), Offset(cx, cy))
                for (i in 1..3) drawCircle(OrcaColors.CyanIntelligence.copy(alpha = 0.05f * (4 - i)), (80 + i * 60).dp.toPx(), Offset(cx, cy), style = Stroke((1.5f - i * 0.3f).dp.toPx()))
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.8f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TASK CHAIN STATUS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W600, fontSize = 12.sp, color = OrcaColors.NeonRed, letterSpacing = 3.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        val total = taskNodes.size
                        if (total > 0) {
                            val completed = taskNodes.count { it.status == NodeStatus.VERIFIED }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                                StatBadge("$completed/$total", "DONE", OrcaColors.SuccessGreen)
                                StatBadge("${taskNodes.count { it.status == NodeStatus.IN_PROGRESS }}", "ACTIVE", OrcaColors.CyanIntelligence)
                                StatBadge("${taskNodes.count { it.status == NodeStatus.PENDING }}", "PENDING", OrcaColors.WarmGrey)
                                StatBadge("${taskNodes.count { it.status == NodeStatus.FAILED }}", "FAILED", OrcaColors.ErrorRed)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(progress = { completed.toFloat() / total }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = OrcaColors.NeonRed, trackColor = OrcaColors.NeonRedFaint)
                        }
                    }
                }
                if (taskNodes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No active task chain", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = OrcaColors.CoolGrey) }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(taskNodes) { node -> TimelineNode(node, selectedNode?.id == node.id, node.status == NodeStatus.IN_PROGRESS) { selectedNode = node } }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineNode(node: TaskNode, isSelected: Boolean, isActive: Boolean, onClick: () -> Unit) {
    val statusColor = when (node.status) {
        NodeStatus.VERIFIED -> OrcaColors.SuccessGreen; NodeStatus.IN_PROGRESS -> OrcaColors.CyanIntelligence
        NodeStatus.FAILED -> OrcaColors.ErrorRed; NodeStatus.NEEDS_RECOVERY -> OrcaColors.WarningOrange; else -> OrcaColors.WarmGrey
    }
    val statusIcon = when (node.status) {
        NodeStatus.VERIFIED -> Icons.Default.CheckCircle; NodeStatus.IN_PROGRESS -> Icons.Default.PlayArrow
        NodeStatus.FAILED -> Icons.Default.Close; NodeStatus.NEEDS_RECOVERY -> Icons.Default.Warning; else -> Icons.Default.Circle
    }
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp).background(if (isSelected) OrcaColors.NeonRed.copy(alpha = 0.1f) else OrcaColors.AbyssBlack.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(if (isActive) 28.dp else 24.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.2f)), Alignment.Center) { Icon(statusIcon, node.status.name, tint = statusColor, modifier = Modifier.size(if (isActive) 16.dp else 14.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(node.description, fontWeight = if (isActive) FontWeight.W600 else FontWeight.Normal, fontSize = 13.sp, color = if (isActive) OrcaColors.PureWhite else OrcaColors.CoolGrey, textDecoration = if (node.status == NodeStatus.VERIFIED) TextDecoration.LineThrough else TextDecoration.None, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (node.retryCount > 0) Text("Retries: ${node.retryCount}/${node.maxRetries}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = OrcaColors.WarningOrange)
        }
    }
}

@Composable
fun StatBadge(count: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W600, fontSize = 18.sp, color = color)
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = OrcaColors.WarmGrey, letterSpacing = 2.sp)
    }
}
