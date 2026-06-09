// app/src/main/java/com/orca/agent/ui/screens/SettingsScreen.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.orca.agent.ui.theme.OrcaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var autoPilotEnabled by remember { mutableStateOf(false) }
    var deepThinkMode by remember { mutableStateOf(false) }
    var silentTaskEnabled by remember { mutableStateOf(true) }
    var voiceEnabled by remember { mutableStateOf(true) }
    var threatDetection by remember { mutableStateOf(true) }
    var autoReplyEnabled by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Agent Configuration
            SettingsSection(title = "AGENT CONFIGURATION") {
                SettingsToggle(
                    title = "AutoPilot Mode",
                    subtitle = "Enable full autonomous agent behavior",
                    icon = Icons.Default.AutoMode,
                    checked = autoPilotEnabled,
                    onCheckedChange = { autoPilotEnabled = it }
                )
                SettingsToggle(
                    title = "Deep Think Mode",
                    subtitle = "Use more compute for complex task planning",
                    icon = Icons.Default.Psychology,
                    checked = deepThinkMode,
                    onCheckedChange = { deepThinkMode = it }
                )
                SettingsToggle(
                    title = "Silent Background Tasks",
                    subtitle = "Optimize device during idle time",
                    icon = Icons.Default.NightsStay,
                    checked = silentTaskEnabled,
                    onCheckedChange = { silentTaskEnabled = it }
                )
            }
            
            // Security Settings
            SettingsSection(title = "SECURITY") {
                SettingsToggle(
                    title = "Threat Detection",
                    subtitle = "Scan for phishing and malicious content",
                    icon = Icons.Default.Shield,
                    checked = threatDetection,
                    onCheckedChange = { threatDetection = it }
                )
                SettingsToggle(
                    title = "Biometric Checkpoints",
                    subtitle = "Require fingerprint for sensitive actions",
                    icon = Icons.Default.Fingerprint,
                    checked = true,
                    onCheckedChange = { }
                )
                SettingsItem(
                    title = "Manage Auto-Reply Whitelist",
                    subtitle = "Contacts allowed for automatic replies",
                    icon = Icons.Default.Contacts,
                    onClick = { }
                )
            }
            
            // Communication
            SettingsSection(title = "COMMUNICATION") {
                SettingsToggle(
                    title = "Voice Output",
                    subtitle = "Orca speaks responses aloud",
                    icon = Icons.Default.VolumeUp,
                    checked = voiceEnabled,
                    onCheckedChange = { voiceEnabled = it }
                )
                SettingsToggle(
                    title = "Auto-Reply",
                    subtitle = "Automatically respond to trusted contacts",
                    icon = Icons.Default.Reply,
                    checked = autoReplyEnabled,
                    onCheckedChange = { autoReplyEnabled = it }
                )
            }
            
            // Data & Storage
            SettingsSection(title = "DATA & STORAGE") {
                SettingsItem(
                    title = "Memory Usage",
                    subtitle = "1.2 GB of 5 GB allocated",
                    icon = Icons.Default.Storage,
                    onClick = { }
                )
                SettingsItem(
                    title = "Export All Data",
                    subtitle = "Download your Orca data archive",
                    icon = Icons.Default.Download,
                    onClick = { }
                )
                SettingsItem(
                    title = "Clear Memory Stream",
                    subtitle = "Delete all chat sessions and memories",
                    icon = Icons.Default.DeleteForever,
                    onClick = { },
                    isDestructive = true
                )
            }
            
            // About
            SettingsSection(title = "ABOUT") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            OrcaColors.AbyssBlack.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ORCA v2.0.0",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.W600,
                        fontSize = 16.sp,
                        color = OrcaColors.PureWhite
                    )
                    Text(
                        text = "Codename: Abyssal Neon",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = OrcaColors.NeonRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Autonomous Agent for Android",
                        fontFamily = FontFamily.Default,
                        fontSize = 13.sp,
                        color = OrcaColors.CoolGrey
                    )
                    Text(
                        text = "Powered by Gemini 2.5 Flash",
                        fontFamily = FontFamily.Default,
                        fontSize = 13.sp,
                        color = OrcaColors.CoolGrey
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.W600,
            fontSize = 11.sp,
            color = OrcaColors.NeonRed,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = OrcaColors.CoolGrey,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.W500,
                fontSize = 14.sp,
                color = OrcaColors.PureWhite
            )
            Text(
                text = subtitle,
                fontFamily = FontFamily.Default,
                fontSize = 11.sp,
                color = OrcaColors.WarmGrey
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OrcaColors.NeonRed,
                checkedTrackColor = OrcaColors.NeonRedFaint,
                uncheckedThumbColor = OrcaColors.WarmGrey,
                uncheckedTrackColor = OrcaColors.GlassDark
            )
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDestructive) OrcaColors.ErrorRed else OrcaColors.CoolGrey,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.W500,
                fontSize = 14.sp,
                color = if (isDestructive) OrcaColors.ErrorRed else OrcaColors.PureWhite
            )
            Text(
                text = subtitle,
                fontFamily = FontFamily.Default,
                fontSize = 11.sp,
                color = OrcaColors.WarmGrey
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = OrcaColors.WarmGrey,
            modifier = Modifier.size(20.dp)
        )
    }
}
