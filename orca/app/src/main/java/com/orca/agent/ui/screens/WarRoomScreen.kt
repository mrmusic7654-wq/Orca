package com.orca.agent.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orca.agent.core.TaskNode
import com.orca.agent.ui.theme.OrcaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarRoomScreen(sessionId: String, onNavigateBack: () -> Unit) {
    val taskNodes = remember { mutableStateListOf<TaskNode>() }
    Scaffold(
        topBar = { TopAppBar(title = { Text("WAR ROOM", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W300, fontSize = 20.sp, color = OrcaColors.PureWhite, letterSpacing = 6.sp) }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = OrcaColors.CoolGrey) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.9f))) },
        containerColor = OrcaColors.VantaBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.8f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TASK CHAIN STATUS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W600, fontSize = 12.sp, color = OrcaColors.NeonRed, letterSpacing = 3.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = 0.6f, modifier = Modifier.fillMaxWidth().height(4.dp), color = OrcaColors.NeonRed, trackColor = OrcaColors.NeonRedFaint)
                    }
                }
            }
        }
    }
}
