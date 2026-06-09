// app/src/main/java/com/orca/agent/ui/screens/MemoryStreamScreen.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orca.agent.memory.ChatSession
import com.orca.agent.memory.SessionStatus
import com.orca.agent.ui.components.MemoryCard
import com.orca.agent.ui.components.SearchBar
import com.orca.agent.ui.theme.OrcaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryStreamScreen(
    sessions: List<ChatSession>,
    onSessionClick: (ChatSession) -> Unit,
    onForkClick: (ChatSession) -> Unit,
    onMergeClick: (ChatSession, ChatSession) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MemoryStreamViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSessions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isMergeMode by remember { mutableStateOf(false) }
    
    val filteredSessions = if (searchQuery.isBlank()) {
        sessions
    } else {
        sessions.filter { session ->
            session.title.contains(searchQuery, ignoreCase = true) ||
            session.messages.any { it.content.contains(searchQuery, ignoreCase = true) } ||
            session.tags.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MEMORY STREAM",
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
                actions = {
                    if (isMergeMode) {
                        TextButton(
                            onClick = {
                                if (selectedSessions.size == 2) {
                                    val sessionsList = selectedSessions.toList()
                                    val target = sessions.find { it.id == sessionsList[0] }!!
                                    val source = sessions.find { it.id == sessionsList[1] }!!
                                    onMergeClick(target, source)
                                    selectedSessions = emptySet()
                                    isMergeMode = false
                                }
                            },
                            enabled = selectedSessions.size == 2
                        ) {
                            Text(
                                text = "MERGE",
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedSessions.size == 2) OrcaColors.NeonRed else OrcaColors.WarmGrey
                            )
                        }
                    } else {
                        IconButton(onClick = { isMergeMode = true }) {
                            Icon(
                                Icons.Default.MergeType,
                                contentDescription = "Merge Sessions",
                                tint = OrcaColors.CyanIntelligence
                            )
                        }
                    }
                    
                    IconButton(onClick = { viewModel.createNewSession() }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Session",
                            tint = OrcaColors.NeonRed
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Session stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip("${sessions.size}", "TOTAL", OrcaColors.PureWhite)
                StatChip(
                    "${sessions.count { it.status == SessionStatus.ACTIVE }}",
                    "ACTIVE",
                    OrcaColors.NeonRed
                )
                StatChip(
                    "${sessions.count { it.status == SessionStatus.PAUSED }}",
                    "PAUSED",
                    OrcaColors.NeonRedPulse
                )
                StatChip(
                    "${sessions.count { it.status == SessionStatus.COMPLETE }}",
                    "COMPLETE",
                    OrcaColors.CyanIntelligence
                )
            }
            
            // Sessions list
            if (filteredSessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = OrcaColors.WarmGrey,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isBlank()) 
                                "No memories yet.\nThe timeline begins now." 
                            else 
                                "No results for \"$searchQuery\"",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = OrcaColors.CoolGrey,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.createNewSession() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrcaColors.NeonRed
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "NEW SESSION",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.W600
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSessions) { session ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            MemoryCard(
                                session = session,
                                onClick = {
                                    if (isMergeMode) {
                                        selectedSessions = if (session.id in selectedSessions) {
                                            selectedSessions - session.id
                                        } else {
                                            selectedSessions + session.id
                                        }
                                    } else {
                                        onSessionClick(session)
                                    }
                                },
                                onFork = { onForkClick(session) }
                            )
                            
                            // Merge selection overlay
                            if (isMergeMode && session.id in selectedSessions) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            OrcaColors.CyanIntelligence.copy(alpha = 0.1f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedSessions = selectedSessions - session.id
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = OrcaColors.CyanIntelligence,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(count: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.W600,
            fontSize = 20.sp,
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
