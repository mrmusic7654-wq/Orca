// app/src/main/java/com/orca/agent/ui/components/MemoryStreamView.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orca.agent.memory.ChatSession
import com.orca.agent.memory.SessionStatus
import com.orca.agent.ui.theme.OrcaColors

@Composable
fun MemoryStreamView(
    sessions: List<ChatSession>,
    onSessionClick: (ChatSession) -> Unit,
    onForkClick: (ChatSession) -> Unit,
    onMergeClick: (ChatSession, ChatSession) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OrcaColors.VantaBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Search bar
            SearchBar()
        }
        
        items(sessions) { session ->
            MemoryCard(
                session = session,
                onClick = { onSessionClick(session) },
                onFork = { onForkClick(session) }
            )
        }
    }
}

@Composable
fun MemoryCard(
    session: ChatSession,
    onClick: () -> Unit,
    onFork: () -> Unit
) {
    val statusColor = when (session.status) {
        SessionStatus.ACTIVE -> OrcaColors.NeonRed
        SessionStatus.COMPLETE -> OrcaColors.CyanIntelligence
        SessionStatus.PAUSED -> OrcaColors.NeonRedPulse
        SessionStatus.ARCHIVED -> OrcaColors.WarmGrey
    }
    
    val statusText = when (session.status) {
        SessionStatus.ACTIVE -> "ACTIVE"
        SessionStatus.COMPLETE -> "COMPLETE"
        SessionStatus.PAUSED -> "PAUSED"
        SessionStatus.ARCHIVED -> "ARCHIVED"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(
                    statusColor.copy(alpha = 0.3f),
                    OrcaColors.GlassBorder
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Left status line
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(statusColor)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title
                Text(
                    text = session.title,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.W600,
                    fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
                    color = OrcaColors.PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Subtitle with session summary
                Text(
                    text = "${session.messages.size} messages • ${session.artifacts.size} artifacts",
                    fontFamily = FontFamily.Monospace,
                    fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                    color = OrcaColors.CoolGrey
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tags
                if (session.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        session.tags.take(3).forEach { tag ->
                            TagChip(tag)
                        }
                    }
                }
            }
            
            // Status and time
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = statusText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                    color = statusColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatRelativeTime(session.updatedAt),
                    fontFamily = FontFamily.Monospace,
                    fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                    color = OrcaColors.WarmGrey
                )
                
                if (session.status == SessionStatus.PAUSED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onFork,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "RESUME",
                            fontFamily = FontFamily.Monospace,
                            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                            color = OrcaColors.CyanIntelligence
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                OrcaColors.NeonRedFaint,
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
            color = OrcaColors.NeonRed
        )
    }
}

@Composable
fun SearchBar() {
    OutlinedTextField(
        value = "",
        onValueChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        placeholder = {
            Text(
                text = "Search memory stream...",
                color = OrcaColors.WarmGrey
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OrcaColors.PureWhite,
            unfocusedTextColor = OrcaColors.CoolGrey,
            focusedBorderColor = OrcaColors.CyanIntelligence,
            unfocusedBorderColor = OrcaColors.GlassBorder,
            cursorColor = OrcaColors.NeonRed
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}
