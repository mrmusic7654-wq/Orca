// app/src/main/java/com/orca/agent/ui/screens/MainOrbScreen.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
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
    
    // State
    val cognitiveState by viewModel.cognitiveState.collectAsState()
    val thoughtStream by viewModel.thoughtStream.collectAsState()
    val autoPilotEnabled by viewModel.autoPilotEnabled.collectAsState()
    val threats by viewModel.threats.collectAsState()
    val activeTask by viewModel.activeTask.collectAsState()
    
    // Permissions
    var hasAccessibilityPermission by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }
    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    
    // Media Projection for screen capture
    val mediaProjectionManager = context.getSystemService(MediaProjectionManager::class.java)
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.let { data ->
                // Start screen capture service
                val intent = android.content.Intent(context, ScreenCaptureService::class.java)
                context.startService(intent)
            }
        }
    }
    
    // Voice input launcher
    val voiceInputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Start voice input
            viewModel.startVoiceInput()
        }
    }
    
    // Camera launcher for SeeAndAct
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.activateSeeAndAct()
        }
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
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu button
                IconButton(onClick = onNavigateToMemory) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Memory Stream",
                        tint = OrcaColors.CyanIntelligence
                    )
                }
                
                // Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ORCA",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.W300,
                        fontSize = 28.sp,
                        color = OrcaColors.PureWhite,
                        letterSpacing = 8.sp
                    )
                    Text(
                        text = "ABYSSAL NEON v2.0",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = OrcaColors.NeonRed.copy(alpha = 0.6f),
                        letterSpacing = 4.sp
                    )
                }
                
                // Settings button
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = OrcaColors.CoolGrey
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(0.3f))
            
            // ORCA ORB - Central element
            OrcaOrb(
                cognitiveState = cognitiveState,
                modifier = Modifier.size(220.dp),
                onOrbClick = {
                    if (cognitiveState == CognitiveState.AWAITING_INPUT) {
                        onNavigateToChat()
                    }
                }
            )
            
            // Status text under orb
            AnimatedContent(
                targetState = cognitiveState,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith 
                    fadeOut() + slideOutVertically()
                }
            ) { state ->
                Text(
                    text = getStateMessage(state),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = OrcaColors.CoolGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(0.3f))
            
            // Active task indicator
            activeTask?.let { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(OrcaColors.NeonRed.copy(alpha = 0.5f), OrcaColors.GlassBorder)
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timeline,
                            contentDescription = null,
                            tint = OrcaColors.NeonRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.name,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.W600,
                                fontSize = 14.sp,
                                color = OrcaColors.PureWhite
                            )
                            Text(
                                text = "${task.completedSteps}/${task.totalSteps} steps complete",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = OrcaColors.CoolGrey
                            )
                        }
                        // Progress indicator
                        CircularProgressIndicator(
                            progress = { task.completedSteps.toFloat() / task.totalSteps },
                            modifier = Modifier.size(24.dp),
                            color = OrcaColors.NeonRed,
                            trackColor = OrcaColors.NeonRedFaint,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            // Threat alerts
            threats.take(1).forEach { threat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = OrcaColors.ErrorRed.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = OrcaColors.ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = threat.description,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = OrcaColors.PureWhite
                        )
                    }
                }
            }
            
            // AutoPilot Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AUTOPILOT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.W600,
                        fontSize = 14.sp,
                        color = if (autoPilotEnabled) OrcaColors.CyanIntelligence else OrcaColors.CoolGrey,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = if (autoPilotEnabled) "Orca is fully autonomous" else "Manual control",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = OrcaColors.WarmGrey
                    )
                }
                
                Switch(
                    checked = autoPilotEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (!hasAccessibilityPermission || !hasOverlayPermission) {
                                // Show permission dialog
                            } else {
                                viewModel.toggleAutoPilot(true)
                                // Start screen capture
                                screenCaptureLauncher.launch(
                                    mediaProjectionManager.createScreenCaptureIntent()
                                )
                            }
                        } else {
                            viewModel.toggleAutoPilot(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OrcaColors.CyanIntelligence,
                        checkedTrackColor = OrcaColors.CyanFaint,
                        uncheckedThumbColor = OrcaColors.WarmGrey,
                        uncheckedTrackColor = OrcaColors.GlassDark
                    )
                )
            }
            
            // Thought Stream (collapsed)
            if (thoughtStream.isNotEmpty()) {
                ThoughtStream(
                    thoughtStream = viewModel.thoughtStreamFlow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Command Deck (bottom input)
            CommandDeck(
                onTextSubmit = { text ->
                    viewModel.processTextInput(text)
                    onNavigateToChat()
                },
                onVoiceClick = {
                    voiceInputLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onCameraClick = {
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                },
                onImageClick = {
                    viewModel.openImagePicker()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Permission setup overlay
        if (!hasAccessibilityPermission) {
            PermissionOverlay(
                title = "Accessibility Required",
                message = "Orca needs Accessibility Service to control your phone like a human.",
                onEnable = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
        }
    }
}

@Composable
fun PermissionOverlay(
    title: String,
    message: String,
    onEnable: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrcaColors.VantaBlack.copy(alpha = 0.95f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(OrcaColors.NeonRed, OrcaColors.CyanIntelligence)
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = OrcaColors.NeonRed,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.W600,
                    fontSize = 20.sp,
                    color = OrcaColors.PureWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    fontFamily = FontFamily.Default,
                    fontSize = 14.sp,
                    color = OrcaColors.CoolGrey,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onEnable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrcaColors.NeonRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "ENABLE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.W600
                    )
                }
            }
        }
    }
}

fun getStateMessage(state: CognitiveState): String = when (state) {
    CognitiveState.IDLE -> "Awaiting command"
    CognitiveState.PLANNING -> "Processing request..."
    CognitiveState.EXECUTING -> "Executing task chain"
    CognitiveState.AUTONOMOUS -> "AutoPilot engaged"
    CognitiveState.AWAITING_INPUT -> "Input required"
    CognitiveState.ERROR -> "Error encountered"
    CognitiveState.SHUTDOWN -> "System offline"
}

fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val service = "${context.packageName}/com.orca.agent.execution.AccessibilityBridge"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(service) == true
}
