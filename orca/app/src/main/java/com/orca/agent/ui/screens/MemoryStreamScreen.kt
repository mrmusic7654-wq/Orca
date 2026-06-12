package com.orca.agent.ui.screens

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
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = if (searchQuery.isBlank()) sessions else sessions.filter { it.title.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("MEMORY STREAM", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W300, fontSize = 20.sp, color = OrcaColors.PureWhite, letterSpacing = 6.sp) }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = OrcaColors.CoolGrey) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.9f)))
        },
        containerColor = OrcaColors.VantaBlack
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth().padding(16.dp), placeholder = { Text("Search...", color = OrcaColors.WarmGrey) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OrcaColors.PureWhite, focusedBorderColor = OrcaColors.CyanIntelligence, unfocusedBorderColor = OrcaColors.GlassBorder), shape = RoundedCornerShape(12.dp))
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { session -> MemoryCard(session = session, onClick = { onSessionClick(session) }, onFork = { onForkClick(session) }) }
            }
        }
    }
}
