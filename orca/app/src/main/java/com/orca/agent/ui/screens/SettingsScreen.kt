// ============================================================
// SettingsScreen.kt - FULL FEATURES
// Path: app/src/main/java/com/orca/agent/ui/screens/SettingsScreen.kt
// ============================================================

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
fun SettingsScreen(onNavigateBack: () -> Unit) {
    var autoPilot by remember { mutableStateOf(false) }
    var deepThink by remember { mutableStateOf(false) }
    var silentTasks by remember { mutableStateOf(true) }
    var voiceOutput by remember { mutableStateOf(true) }
    var threatDetection by remember { mutableStateOf(true) }
    var biometricCheckpoints by remember { mutableStateOf(true) }
    var autoReply by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SETTINGS",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = OrcaColors.VantaBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AGENT CONFIGURATION
            SettingsSectionHeader("AGENT CONFIGURATION")
            SettingsCard {
                SettingsToggleRow(
                    title = "AutoPilot Mode",
                    subtitle = "Enable full autonomous agent behavior. Orca will act without confirmation for routine tasks.",
                    icon = Icons.Default.AutoAwesome,
                    checked = autoPilot,
                    onCheckedChange = { autoPilot = it }
                )
                SettingsToggleRow(
                    title = "Deep Think Mode",
                    subtitle = "Use extended reasoning for complex task planning. Increases API usage.",
                    icon = Icons.Default.Psychology,
                    checked = deepThink,
                    onCheckedChange = { deepThink = it }
                )
                SettingsToggleRow(
                    title = "Silent Background Tasks",
                    subtitle = "Optimize device during idle time. Clean cache, update apps, backup data.",
                    icon = Icons.Default.Nightlight,
                    checked = silentTasks,
                    onCheckedChange = { silentTasks = it }
                )
            }

            // SECURITY
            SettingsSectionHeader("SECURITY")
            SettingsCard {
                SettingsToggleRow(
                    title = "Threat Detection",
                    subtitle = "Scan for phishing attempts, malicious links, and suspicious UI patterns.",
                    icon = Icons.Default.Security,
                    checked = threatDetection,
                    onCheckedChange = { threatDetection = it }
                )
                SettingsToggleRow(
                    title = "Biometric Checkpoints",
                    subtitle = "Require fingerprint verification for payments, passwords, and sensitive actions.",
                    icon = Icons.Default.Fingerprint,
                    checked = biometricCheckpoints,
                    onCheckedChange = { biometricCheckpoints = it }
                )
                SettingsClickRow(
                    title = "Auto-Reply Whitelist",
                    subtitle = "Manage contacts allowed for automatic message replies.",
                    icon = Icons.Default.ContactPhone,
                    onClick = { }
                )
                SettingsClickRow(
                    title = "Stored Credentials",
                    subtitle = "Manage saved passwords and login information.",
                    icon = Icons.Default.Password,
                    onClick = { }
                )
            }

            // COMMUNICATION
            SettingsSectionHeader("COMMUNICATION")
            SettingsCard {
                SettingsToggleRow(
                    title = "Voice Output",
                    subtitle = "Orca speaks responses aloud. Respects Do Not Disturb and silent mode.",
                    icon = Icons.Default.VolumeUp,
                    checked = voiceOutput,
                    onCheckedChange = { voiceOutput = it }
                )
                SettingsToggleRow(
                    title = "Auto-Reply",
                    subtitle = "Automatically respond to messages from trusted contacts when you're busy.",
                    icon = Icons.Default.Reply,
                    checked = autoReply,
                    onCheckedChange = { autoReply = it }
                )
            }

            // DATA & STORAGE
            SettingsSectionHeader("DATA & STORAGE")
            SettingsCard {
                SettingsClickRow(
                    title = "Memory Usage",
                    subtitle = "1.2 GB of 5 GB allocated • 24% used",
                    icon = Icons.Default.Storage,
                    onClick = { }
                )
                SettingsClickRow(
                    title = "Export All Data",
                    subtitle = "Download your complete Orca data archive as JSON.",
                    icon = Icons.Default.Download,
                    onClick = { }
                )
                SettingsClickRow(
                    title = "Clear Memory Stream",
                    subtitle = "Permanently delete all chat sessions and learned patterns.",
                    icon = Icons.Default.DeleteForever,
                    onClick = { showClearConfirm = true },
                    isDestructive = true
                )
            }

            // ABOUT
            SettingsSectionHeader("ABOUT")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "ORCA",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.W600,
                            fontSize = 24.sp,
                            color = OrcaColors.NeonRed,
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "v2.0.0",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = OrcaColors.CoolGrey
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Codename: Abyssal Neon",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = OrcaColors.CyanIntelligence
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Autonomous Agent for Android",
                        fontFamily = FontFamily.Default,
                        fontSize = 13.sp,
                        color = OrcaColors.CoolGrey
                    )
                    Text(
                        "Powered by Gemini 2.5 Flash • 1M Context Window",
                        fontFamily = FontFamily.Default,
                        fontSize = 13.sp,
                        color = OrcaColors.CoolGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Clear confirmation dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text(
                    "Clear All Memory?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.W600,
                    color = OrcaColors.PureWhite
                )
            },
            text = {
                Text(
                    "This will permanently delete all chat sessions, learned patterns, and cached data. This action cannot be undone.",
                    color = OrcaColors.CoolGrey
                )
            },
            confirmButton = {
                Button(
                    onClick = { showClearConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OrcaColors.ErrorRed)
                ) {
                    Text("DELETE EVERYTHING", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = OrcaColors.CoolGrey)
                }
            },
            containerColor = OrcaColors.AbyssBlack
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        color = OrcaColors.NeonRed,
        letterSpacing = 3.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
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

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = OrcaColors.CoolGrey, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.W500,
                fontSize = 14.sp,
                color = OrcaColors.PureWhite
            )
            Text(
                subtitle,
                fontFamily = FontFamily.Default,
                fontSize = 11.sp,
                color = OrcaColors.WarmGrey,
                lineHeight = 14.sp
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
fun SettingsClickRow(
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
            null,
            tint = if (isDestructive) OrcaColors.ErrorRed else OrcaColors.CoolGrey,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.W500,
                fontSize = 14.sp,
                color = if (isDestructive) OrcaColors.ErrorRed else OrcaColors.PureWhite
            )
            Text(
                subtitle,
                fontFamily = FontFamily.Default,
                fontSize = 11.sp,
                color = OrcaColors.WarmGrey
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = OrcaColors.WarmGrey.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
