package com.orca.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orca.agent.memory.ChatMessage
import com.orca.agent.memory.MessageRole
import com.orca.agent.ui.components.CommandDeck
import com.orca.agent.ui.theme.OrcaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val session by viewModel.activeSession.collectAsState()
    val messages = session?.messages ?: emptyList()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.title ?: "Chat", fontWeight = FontWeight.W600, fontSize = 18.sp, color = OrcaColors.PureWhite) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = OrcaColors.CoolGrey) } },
                actions = {
                    IconButton(onClick = { viewModel.forkSession() }) { Icon(Icons.Default.Add, "Fork", tint = OrcaColors.CyanIntelligence) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.9f))
            )
        },
        containerColor = OrcaColors.VantaBlack
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    val isUser = message.role == MessageRole.USER
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                        Card(
                            modifier = Modifier.widthIn(max = 320.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isUser) OrcaColors.AbyssBlack else OrcaColors.NeonRed.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp)
                        ) {
                            Text(message.content, modifier = Modifier.padding(12.dp), fontSize = 14.sp, color = if (isUser) OrcaColors.PureWhite else OrcaColors.CoolGrey)
                        }
                    }
                }
            }
            CommandDeck(onTextSubmit = { viewModel.sendMessage(it) }, onVoiceClick = {}, onCameraClick = {}, onImageClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
