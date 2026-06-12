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
import com.orca.agent.ui.components.ThoughtStream
import com.orca.agent.ui.theme.OrcaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainOrbScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainOrbViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val cognitiveState by viewModel.cognitiveState.collectAsState()
    val autoPilotEnabled by viewModel.autoPilotEnabled.collectAsState()
    val threats by viewModel.threats.collectAsState()
    val activeTask by viewModel.activeTask.collectAsState()

    val mpManager = remember { context.getSystemService(MediaProjectionManager::class.java) }
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.let { data ->
                context.startService(Intent(context, ScreenCaptureService::class.java))
            }
        }
    }

    var hasAccessibilityPermission by remember {
        mutableStateOf(
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )?.contains(context.packageName) == true
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrcaColors.VantaBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToMemory) {
                    Icon(Icons.Default.DateRange, contentDescription = "Memory Stream", tint = OrcaColors.CyanIntelligence)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ORCA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W300, fontSize = 28.sp, color = OrcaColors.PureWhite, letterSpacing = 8.sp)
                    Text("ABYSSAL NEON v2.0", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = OrcaColors.NeonRed.copy(alpha = 0.6f), letterSpacing = 4.sp)
                }

                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = OrcaColors.CoolGrey)
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            OrcaOrb(
                cognitiveState = cognitiveState,
                modifier = Modifier.size(220.dp),
                onOrbClick = {
                    if (cognitiveState == CognitiveState.AWAITING_CONFIRMATION) onNavigateToChat()
                }
            )

            AnimatedContent(
                targetState = cognitiveState,
                transitionSpec = { fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically() },
                label = "state"
            ) { state ->
                Text(
                    text = when (state) {
                        CognitiveState.IDLE -> "Awaiting command"
                        CognitiveState.OBSERVING -> "Watching for patterns..."
                        CognitiveState.PLANNING -> "Processing request..."
                        CognitiveState.EXECUTING -> "Executing task chain"
                        CognitiveState.VERIFYING -> "Verifying action..."
                        CognitiveState.RECOVERING -> "Recovering from error..."
                        CognitiveState.AWAITING_CONFIRMATION -> "Input required"
                        CognitiveState.LEARNING -> "Learning from experience..."
                        CognitiveState.AUTONOMOUS -> "AutoPilot engaged"
                        CognitiveState.ERROR -> "Error encountered"
                        CognitiveState.SHUTDOWN -> "System offline"
                    },
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = OrcaColors.CoolGrey,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.3f))

            activeTask?.let { task ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timeline, null, tint = OrcaColors.NeonRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.name, fontFamily = FontFamily.Default, fontWeight = FontWeight.W600, fontSize = 14.sp, color = OrcaColors.PureWhite)
                            Text("${task.completedSteps}/${task.totalSteps} steps", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = OrcaColors.CoolGrey)
                        }
                        CircularProgressIndicator(
                            progress = { if (task.totalSteps > 0) task.completedSteps.toFloat() / task.totalSteps else 0f },
                            modifier = Modifier.size(24.dp), color = OrcaColors.NeonRed, trackColor = OrcaColors.NeonRedFaint, strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Threat alerts - simplified to avoid enum import issues
            threats.take(3).forEach { threat ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = OrcaColors.ErrorRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = OrcaColors.ErrorRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(threat.description, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = OrcaColors.PureWhite)
                            Text("Source: ${threat.source}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = OrcaColors.CoolGrey)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("AUTOPILOT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W600, fontSize = 14.sp, color = if (autoPilotEnabled) OrcaColors.CyanIntelligence else OrcaColors.CoolGrey, letterSpacing = 3.sp)
                    Text(if (autoPilotEnabled) "Orca is fully autonomous" else "Manual control mode", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = OrcaColors.WarmGrey)
                }
                Switch(
                    checked = autoPilotEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasAccessibilityPermission) {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        } else {
                            viewModel.toggleAutoPilot(enabled)
                            if (enabled) screenCaptureLauncher.launch(mpManager.createScreenCaptureIntent())
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = OrcaColors.CyanIntelligence, checkedTrackColor = OrcaColors.CyanFaint, uncheckedThumbColor = OrcaColors.WarmGrey, uncheckedTrackColor = OrcaColors.GlassDark)
                )
            }

            ThoughtStream(thoughtStream = viewModel.thoughtStreamFlow, modifier = Modifier.fillMaxWidth().height(120.dp))
            Spacer(modifier = Modifier.height(8.dp))

            CommandDeck(
                onTextSubmit = { text -> viewModel.processTextInput(text); onNavigateToChat() },
                onVoiceClick = {},
                onCameraClick = {},
                onImageClick = {},
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!hasAccessibilityPermission) {
            Box(modifier = Modifier.fillMaxSize().background(OrcaColors.VantaBlack.copy(alpha = 0.95f)).clickable(enabled = false) { }, contentAlignment = Alignment.Center) {
                Card(modifier = Modifier.padding(32.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, null, tint = OrcaColors.NeonRed, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Accessibility Required", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W600, fontSize = 20.sp, color = OrcaColors.PureWhite)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Orca needs Accessibility Service to control your phone like a human.", fontFamily = FontFamily.Default, fontSize = 14.sp, color = OrcaColors.CoolGrey, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, colors = ButtonDefaults.buttonColors(containerColor = OrcaColors.NeonRed), shape = RoundedCornerShape(12.dp)) {
                            Text("ENABLE ACCESSIBILITY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W600)
                        }
                    }
                }
            }
        }
    }
}
