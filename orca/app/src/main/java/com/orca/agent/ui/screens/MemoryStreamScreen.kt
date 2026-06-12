// ============================================================
// MemoryStreamScreen.kt - FULL FEATURES
// Path: app/src/main/java/com/orca/agent/ui/screens/MemoryStreamScreen.kt
// ============================================================

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
    var isMergeMode by remember { mutableStateOf(false) }
    var selectedForMerge by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    val filtered = if (searchQuery.isBlank()) {
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
                        "MEMORY STREAM",
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
                    if (isMergeMode) {
                        TextButton(
                            onClick = {
                                val selected = selectedForMerge.toList()
                                if (selected.size == 2) {
                                    val target = sessions.find { it.id == selected[0] }
                                    val source = sessions.find { it.id == selected[1] }
                                    if (target != null && source != null) {
                                        onMergeClick(target, source)
                                    }
                                    selectedForMerge = emptySet()
                                    isMergeMode = false
                                }
                            },
                            enabled = selectedForMerge.size == 2
                        ) {
                            Text(
                                "MERGE (${selectedForMerge.size}/2)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (selectedForMerge.size == 2) OrcaColors.NeonRed else OrcaColors.WarmGrey
                            )
                        }
                        IconButton(onClick = {
                            isMergeMode = false
                            selectedForMerge = emptySet()
                        }) {
                            Icon(Icons.Default.Close, "Cancel Merge", tint = OrcaColors.CoolGrey)
                        }
                    } else {
                        IconButton(onClick = { isMergeMode = true }) {
                            Icon(Icons.Default.CallMerge, "Merge Sessions", tint = OrcaColors.CyanIntelligence)
                        }
                    }
                    IconButton(onClick = { viewModel.createNewSession() }) {
                        Icon(Icons.Default.Add, "New Session", tint = OrcaColors.NeonRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = OrcaColors.VantaBlack
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search memory stream...", color = OrcaColors.WarmGrey) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = OrcaColors.CoolGrey) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear", tint = OrcaColors.CoolGrey)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OrcaColors.PureWhite,
                    unfocusedTextColor = OrcaColors.CoolGrey,
                    focusedBorderColor = OrcaColors.CyanIntelligence,
                    unfocusedBorderColor = OrcaColors.GlassBorder,
                    cursorColor = OrcaColors.NeonRed,
                    focusedContainerColor = OrcaColors.AbyssBlack,
                    unfocusedContainerColor = OrcaColors.AbyssBlack
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Stats row
            if (sessions.isNotEmpty()) {
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
                        "DONE",
                        OrcaColors.CyanIntelligence
                    )
                }
            }

            // Session list or empty state
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            tint = OrcaColors.WarmGrey.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No memories yet.\nThe timeline begins now."
                            else "No results for \"$searchQuery\"",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = OrcaColors.CoolGrey,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.createNewSession() },
                            colors = ButtonDefaults.buttonColors(containerColor = OrcaColors.NeonRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("NEW SESSION", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W600)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { session ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            MemoryCard(
                                session = session,
                                onClick = {
                                    if (isMergeMode) {
                                        selectedForMerge = if (session.id in selectedForMerge) {
                                            selectedForMerge - session.id
                                        } else if (selectedForMerge.size < 2) {
                                            selectedForMerge + session.id
                                        } else {
                                            selectedForMerge
                                        }
                                    } else {
                                        onSessionClick(session)
                                    }
                                },
                                onFork = { onForkClick(session) }
                            )

                            // Merge selection overlay
                            if (isMergeMode && session.id in selectedForMerge) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            OrcaColors.CyanIntelligence.copy(alpha = 0.15f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedForMerge = selectedForMerge - session.id
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected for merge",
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

    // Delete confirmation dialog
    showDeleteConfirm?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Session?", color = OrcaColors.PureWhite) },
            text = { Text("This cannot be undone.", color = OrcaColors.CoolGrey) },
            confirmButton = {
                TextButton(onClick = {
                    // Delete logic
                    showDeleteConfirm = null
                }) {
                    Text("Delete", color = OrcaColors.ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel", color = OrcaColors.CoolGrey)
                }
            },
            containerColor = OrcaColors.AbyssBlack
        )
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
