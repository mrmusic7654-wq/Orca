package com.orca.agent.ui.components

import androidx.compose.animation.*
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
fun TaskTimeline(
    nodes: List<TaskNode>,
    currentNodeId: String?,
    modifier: Modifier = Modifier,
    onNodeClick: (TaskNode) -> Unit = {}
) {
    if (nodes.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No active task chain",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = OrcaColors.WarmGrey
            )
        }
        return
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(nodes) { node ->
            TimelineNode(
                node = node,
                isActive = node.id == currentNodeId,
                isLast = nodes.lastOrNull()?.id == node.id,
                onNodeClick = { onNodeClick(node) }
            )
        }
    }
}

@Composable
fun TimelineNode(
    node: TaskNode,
    isActive: Boolean,
    isLast: Boolean,
    onNodeClick: () -> Unit
) {
    val statusColor = when (node.status) {
        NodeStatus.COMPLETED -> OrcaColors.SuccessGreen
        NodeStatus.IN_PROGRESS -> OrcaColors.CyanIntelligence
        NodeStatus.FAILED -> OrcaColors.ErrorRed
        NodeStatus.SKIPPED -> OrcaColors.WarmGrey
        NodeStatus.PENDING -> OrcaColors.WarmGrey
    }
    
    val statusIcon = when (node.status) {
        NodeStatus.COMPLETED -> Icons.Default.CheckCircle
        NodeStatus.IN_PROGRESS -> Icons.Default.Timelapse
        NodeStatus.FAILED -> Icons.Default.Cancel
        NodeStatus.SKIPPED -> Icons.Default.SkipNext
        NodeStatus.PENDING -> Icons.Default.RadioButtonUnchecked
    }
    
    val textDecoration = if (node.status == NodeStatus.COMPLETED) {
        TextDecoration.LineThrough
    } else {
        TextDecoration.None
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLast) 60.dp else 80.dp)
            .clickable(onClick = onNodeClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline line and dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Dot
            Box(
                modifier = Modifier
                    .size(if (isActive) 16.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) statusColor else statusColor.copy(alpha = 0.6f)
                    )
                    .then(
                        if (isActive) Modifier.animateContentSize() else Modifier
                    )
            )
            
            // Connecting line (if not last)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(70.dp)
                        .background(
                            if (node.status == NodeStatus.COMPLETED) 
                                OrcaColors.SuccessGreen.copy(alpha = 0.3f) 
                            else 
                                OrcaColors.GlassDark
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Node content
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive) 
                    OrcaColors.NeonRed.copy(alpha = 0.1f) 
                else 
                    OrcaColors.AbyssBlack.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp),
            border = if (isActive) {
                CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(statusColor.copy(alpha = 0.5f), OrcaColors.GlassBorder)
                    )
                )
            } else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                Icon(
                    statusIcon,
                    contentDescription = node.status.name,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.description,
                        fontFamily = FontFamily.Default,
                        fontWeight = if (isActive) FontWeight.W600 else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (isActive) OrcaColors.PureWhite else OrcaColors.CoolGrey,
                        textDecoration = textDecoration,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (node.retryCount > 0) {
                        Text(
                            text = "Retry ${node.retryCount}/${node.maxRetries}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = OrcaColors.WarningOrange
                        )
                    }
                    
                    if (node.requiresConfirmation) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = OrcaColors.NeonRedPulse,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "HUMAN CHECKPOINT",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                color = OrcaColors.NeonRedPulse,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
                
                // Action type badge
                val actionLabel = when (node.action) {
                    is com.orca.agent.core.AgentAction.Tap -> "TAP"
                    is com.orca.agent.core.AgentAction.Swipe -> "SWIPE"
                    is com.orca.agent.core.AgentAction.Type -> "TYPE"
                    is com.orca.agent.core.AgentAction.LongPress -> "HOLD"
                    is com.orca.agent.core.AgentAction.Back -> "BACK"
                    is com.orca.agent.core.AgentAction.Home -> "HOME"
                    is com.orca.agent.core.AgentAction.AppAction -> "APP"
                    else -> "ACT"
                }
                
                Box(
                    modifier = Modifier
                        .background(
                            statusColor.copy(alpha = 0.15f),
                            RoundedCornerShape(4.dp)
                        )
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
    }
}
