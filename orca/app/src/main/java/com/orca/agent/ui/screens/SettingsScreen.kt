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
    var autoReply by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W300, fontSize = 20.sp, color = OrcaColors.PureWhite, letterSpacing = 6.sp) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = OrcaColors.CoolGrey) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.9f))
            )
        },
        containerColor = OrcaColors.VantaBlack
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            SettingsSection("AGENT CONFIGURATION")
            SettingsCard {
                SettingsToggle("AutoPilot Mode", "Full autonomous agent behavior", Icons.Default.Star, autoPilot) { autoPilot = it }
                SettingsToggle("Deep Think Mode", "Extended reasoning for complex tasks", Icons.Default.Build, deepThink) { deepThink = it }
                SettingsToggle("Silent Background Tasks", "Optimize during idle time", Icons.Default.Home, silentTasks) { silentTasks = it }
            }

            SettingsSection("SECURITY")
            SettingsCard {
                SettingsToggle("Threat Detection", "Scan for phishing and malicious content", Icons.Default.Warning, threatDetection) { threatDetection = it }
                SettingsToggle("Biometric Checkpoints", "Fingerprint for sensitive actions", Icons.Default.Lock, true) { }
                SettingsClickRow("Auto-Reply Whitelist", "Manage trusted contacts", Icons.Default.Person) { }
                SettingsClickRow("Stored Credentials", "Manage saved passwords", Icons.Default.KeyboardArrowRight) { }
            }

            SettingsSection("COMMUNICATION")
            SettingsCard {
                SettingsToggle("Voice Output", "Orca speaks responses aloud", Icons.Default.PlayArrow, voiceOutput) { voiceOutput = it }
                SettingsToggle("Auto-Reply", "Respond to trusted contacts", Icons.Default.Email, autoReply) { autoReply = it }
            }

            SettingsSection("DATA & STORAGE")
            SettingsCard {
                SettingsClickRow("Memory Usage", "1.2 GB of 5 GB allocated", Icons.Default.Info) { }
                SettingsClickRow("Export All Data", "Download your data archive", Icons.Default.Share) { }
                SettingsClickRow("Clear Memory Stream", "Delete all sessions and patterns", Icons.Default.Delete) { showClearConfirm = true }
            }

            SettingsSection("ABOUT")
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ORCA v2.0.0", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W600, fontSize = 16.sp, color = OrcaColors.PureWhite)
                    Text("Codename: Abyssal Neon", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = OrcaColors.NeonRed)
                    Text("Powered by Gemini 2.5 Flash", fontFamily = FontFamily.Default, fontSize = 13.sp, color = OrcaColors.CoolGrey)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Memory?", fontWeight = FontWeight.W600, color = OrcaColors.PureWhite) },
            text = { Text("This permanently deletes all sessions and data.", color = OrcaColors.CoolGrey) },
            confirmButton = { Button(onClick = { showClearConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = OrcaColors.ErrorRed)) { Text("DELETE") } },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } },
            containerColor = OrcaColors.AbyssBlack
        )
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.W600, fontSize = 11.sp, color = OrcaColors.NeonRed, letterSpacing = 3.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(4.dp)) { content() }
    }
}

@Composable
fun SettingsToggle(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = OrcaColors.CoolGrey, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.W500, fontSize = 14.sp, color = OrcaColors.PureWhite)
            Text(subtitle, fontSize = 11.sp, color = OrcaColors.WarmGrey)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = OrcaColors.NeonRed, checkedTrackColor = OrcaColors.NeonRedFaint))
    }
}

@Composable
fun SettingsClickRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = OrcaColors.CoolGrey, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.W500, fontSize = 14.sp, color = OrcaColors.PureWhite)
            Text(subtitle, fontSize = 11.sp, color = OrcaColors.WarmGrey)
        }
    }
}
