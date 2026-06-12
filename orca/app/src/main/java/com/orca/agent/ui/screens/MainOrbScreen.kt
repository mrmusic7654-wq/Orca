package com.orca.agent.ui.screens

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orca.agent.core.CognitiveState
import com.orca.agent.execution.ScreenCaptureService
import com.orca.agent.ui.components.CommandDeck
import com.orca.agent.ui.components.OrcaOrb
import com.orca.agent.ui.theme.OrcaColors
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainOrbScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainOrbViewModel = viewModel()
) {
    val context = LocalContext.current
    val cognitiveState by viewModel.cognitiveState.collectAsState()
    val autoPilotEnabled by viewModel.autoPilotEnabled.collectAsState()
    val activeTask by viewModel.activeTask.collectAsState()

    val mpManager = remember { context.getSystemService(MediaProjectionManager::class.java) }
    val screenCaptureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) context.startService(Intent(context, ScreenCaptureService::class.java))
    }

    Box(modifier = Modifier.fillMaxSize().background(OrcaColors.VantaBlack)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateToMemory) { Icon(Icons.Default.DateRange, "Memory", tint = OrcaColors.CyanIntelligence) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ORCA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W300, fontSize = 28.sp, color = OrcaColors.PureWhite, letterSpacing = 8.sp)
                    Text("ABYSSAL NEON v2.0", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = OrcaColors.NeonRed.copy(alpha = 0.6f), letterSpacing = 4.sp)
                }
                IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings", tint = OrcaColors.CoolGrey) }
            }
            Spacer(modifier = Modifier.weight(0.3f))
            OrcaOrb(cognitiveState = cognitiveState, modifier = Modifier.size(220.dp))
            Spacer(modifier = Modifier.weight(0.3f))

            activeTask?.let { task ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.8f)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = OrcaColors.NeonRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.name, fontWeight = FontWeight.W600, fontSize = 14.sp, color = OrcaColors.PureWhite)
                            Text("${task.completedSteps}/${task.totalSteps} steps", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = OrcaColors.CoolGrey)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("AUTOPILOT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W600, fontSize = 14.sp, color = if (autoPilotEnabled) OrcaColors.CyanIntelligence else OrcaColors.CoolGrey, letterSpacing = 3.sp) }
                Switch(checked = autoPilotEnabled, onCheckedChange = { e -> viewModel.toggleAutoPilot(e); if (e) screenCaptureLauncher.launch(mpManager.createScreenCaptureIntent()) }, colors = SwitchDefaults.colors(checkedThumbColor = OrcaColors.CyanIntelligence, checkedTrackColor = OrcaColors.CyanFaint))
            }
            CommandDeck(onTextSubmit = { viewModel.processTextInput(it); onNavigateToChat() }, onVoiceClick = {}, onCameraClick = {}, onImageClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
