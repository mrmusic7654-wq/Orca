// app/src/main/java/com/orca/agent/ui/screens/ChatScreen.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.orca.agent.memory.ChatMessage
import com.orca.agent.memory.MessageRole
import com.orca.agent.ui.components.CommandDeck
import com.orca.agent.ui.components.ThoughtStream
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
    val thoughtStream by viewModel.thoughtStream.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = session?.title ?: "Chat",
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.W600,
                            fontSize = 18.sp,
                            color = OrcaColors.PureWhite
                        )
                        if (session?.isPaused == true) {
                            Text(
                                text = "PAUSED - Awaiting input",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = OrcaColors.NeonRedPulse
                            )
                        }
                    }
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
                    // Fork session button
                    IconButton(onClick = { viewModel.forkSession() }) {
                        Icon(
                            Icons.Default.CallSplit,
                            contentDescription = "Fork Session",
                            tint = OrcaColors.CyanIntelligence
                        )
                    }
                    // View task timeline
                    IconButton(onClick = { viewModel.showTaskTimeline() }) {
                        Icon(
                            Icons.Default.Timeline,
                            contentDescription = "Task Timeline",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages) { message ->
                        ChatBubble(message = message)
                    }
                    
                    // Thinking indicator
                    if (isThinking) {
                        item {
                            ThinkingIndicator()
                        }
                    }
                }
                
                // Thought Stream (if active tasks)
                if (thoughtStream.isNotEmpty()) {
                    ThoughtStream(
                        thoughtStream = viewModel.thoughtStreamFlow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
                
                // Command input
                CommandDeck(
                    onTextSubmit = { text -> viewModel.sendMessage(text) },
                    onVoiceClick = { viewModel.startVoiceInput() },
                    onCameraClick = { viewModel.activateSeeAndAct() },
                    onImageClick = { viewModel.openImagePicker() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val isThought = message.role == MessageRole.THOUGHT_STREAM
    
    val bubbleColor = when (message.role) {
        MessageRole.USER -> OrcaColors.AbyssBlack
        MessageRole.ORCA -> OrcaColors.NeonRed.copy(alpha = 0.1f)
        MessageRole.SYSTEM -> OrcaColors.CyanIntelligence.copy(alpha = 0.1f)
        MessageRole.THOUGHT_STREAM -> OrcaColors.GlassDark
    }
    
    val borderColor = when (message.role) {
        MessageRole.USER -> OrcaColors.NeonRed.copy(alpha = 0.3f)
        MessageRole.ORCA -> OrcaColors.NeonRed.copy(alpha = 0.2f)
        MessageRole.SYSTEM -> OrcaColors.CyanIntelligence.copy(alpha = 0.3f)
        MessageRole.THOUGHT_STREAM -> OrcaColors.GlassBorder
    }
    
    val alignment = if (isUser) Alignment.End else Alignment.Start
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // Role label
        if (!isUser && !isThought) {
            Text(
                text = when (message.role) {
                    MessageRole.ORCA -> "ORCA"
                    MessageRole.SYSTEM -> "SYSTEM"
                    else -> ""
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = if (message.role == MessageRole.ORCA) OrcaColors.NeonRed else OrcaColors.CyanIntelligence,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        
        // Message content
        Card(
            modifier = Modifier
                .widthIn(max = 320.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(borderColor, OrcaColors.GlassBorder)
                )
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Attachments
                message.attachments.forEach { attachment ->
                    if (attachment.mimeType.startsWith("image/")) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(attachment.data)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Image attachment",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Text content
                Text(
                    text = message.content,
                    fontFamily = if (isThought) FontFamily.Monospace else FontFamily.Default,
                    fontSize = if (isThought) 11.sp else 14.sp,
                    color = when (message.role) {
                        MessageRole.USER -> OrcaColors.PureWhite
                        MessageRole.ORCA -> OrcaColors.CoolGrey
                        MessageRole.SYSTEM -> OrcaColors.CyanIntelligence
                        MessageRole.THOUGHT_STREAM -> OrcaColors.CyanIntelligence.copy(alpha = 0.6f)
                    },
                    lineHeight = if (isThought) 14.sp else 20.sp
                )
                
                // Actions taken (for Orca messages)
                if (message.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    message.actions.forEach { action ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = OrcaColors.SuccessGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = action.description,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = OrcaColors.SuccessGreen
                            )
                        }
                    }
                }
            }
        }
        
        // Timestamp
        Text(
            text = formatTimestamp(message.timestamp),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = OrcaColors.WarmGrey,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
fun ThinkingIndicator() {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OrcaColors.NeonRed.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = OrcaColors.NeonRed,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Orca is thinking...",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = OrcaColors.NeonRed.copy(alpha = 0.7f)
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
