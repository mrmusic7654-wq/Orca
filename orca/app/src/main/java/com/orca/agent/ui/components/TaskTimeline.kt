package com.orca.agent.ui.components

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orca.agent.core.TaskNode
import com.orca.agent.core.NodeStatus
import com.orca.agent.ui.theme.OrcaColors

@Composable
fun TaskTimeline(nodes: List<TaskNode>, currentNodeId: String?, modifier: Modifier = Modifier, onNodeClick: (TaskNode) -> Unit = {}) {
    if (nodes.isEmpty()) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No active tasks", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = OrcaColors.WarmGrey) }; return }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) { items(nodes) { node -> TimelineNodeRow(node = node, isActive = node.id == currentNodeId, isLast = nodes.lastOrNull()?.id == node.id, onClick = { onNodeClick(node) }) } }
}

@Composable
fun TimelineNodeRow(node: TaskNode, isActive: Boolean, isLast: Boolean, onClick: () -> Unit) {
    val statusColor = when (node.status) { NodeStatus.VERIFIED -> OrcaColors.SuccessGreen; NodeStatus.IN_PROGRESS -> OrcaColors.CyanIntelligence; NodeStatus.FAILED -> OrcaColors.ErrorRed; NodeStatus.NEEDS_RECOVERY -> OrcaColors.WarningOrange; else -> OrcaColors.WarmGrey }
    val statusIcon = when (node.status) { NodeStatus.VERIFIED -> Icons.Default.CheckCircle; NodeStatus.IN_PROGRESS -> Icons.Default.PlayArrow; NodeStatus.FAILED -> Icons.Default.Close; NodeStatus.NEEDS_RECOVERY -> Icons.Default.Warning; else -> Icons.Default.Star }
    Row(modifier = Modifier.fillMaxWidth().height(if (isLast) 60.dp else 80.dp).clickable(onClick = onClick).padding(horizontal = 8.dp), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(modifier = Modifier.size(if (isActive) 16.dp else 12.dp).clip(CircleShape).background(if (isActive) statusColor else statusColor.copy(alpha = 0.6f)))
            if (!isLast) Box(modifier = Modifier.width(2.dp).height(70.dp).background(if (node.status == NodeStatus.VERIFIED) OrcaColors.SuccessGreen.copy(alpha = 0.3f) else OrcaColors.GlassDark))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Card(modifier = Modifier.weight(1f).padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = if (isActive) OrcaColors.NeonRed.copy(alpha = 0.1f) else OrcaColors.AbyssBlack.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(statusIcon, node.status.name, tint = statusColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(node.description, fontWeight = if (isActive) FontWeight.W600 else FontWeight.Normal, fontSize = 13.sp, color = if (isActive) OrcaColors.PureWhite else OrcaColors.CoolGrey, textDecoration = if (node.status == NodeStatus.VERIFIED) TextDecoration.LineThrough else TextDecoration.None, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
