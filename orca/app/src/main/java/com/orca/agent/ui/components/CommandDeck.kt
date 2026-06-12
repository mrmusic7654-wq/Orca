package com.orca.agent.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orca.agent.ui.theme.OrcaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandDeck(onTextSubmit: (String) -> Unit, onVoiceClick: () -> Unit, onCameraClick: () -> Unit, onImageClick: () -> Unit, modifier: Modifier = Modifier) {
    var textInput by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    Card(modifier = modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.9f)), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCameraClick, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Star, "Camera", tint = OrcaColors.CoolGrey, modifier = Modifier.size(22.dp)) }
                OutlinedTextField(value = textInput, onValueChange = { textInput = it }, modifier = Modifier.weight(1f).height(48.dp), placeholder = { Text("Message Orca...", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = OrcaColors.WarmGrey) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OrcaColors.PureWhite, focusedBorderColor = OrcaColors.NeonRed, unfocusedBorderColor = Color.Transparent, cursorColor = OrcaColors.NeonRed, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent), shape = RoundedCornerShape(12.dp), singleLine = true)
                IconButton(onClick = { if (textInput.isNotBlank()) { onTextSubmit(textInput); textInput = "" } }, modifier = Modifier.size(40.dp), enabled = textInput.isNotBlank()) { Icon(Icons.Default.Send, "Send", tint = if (textInput.isNotBlank()) OrcaColors.NeonRed else OrcaColors.WarmGrey, modifier = Modifier.size(22.dp)) }
            }
            AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ActionChip(Icons.Default.Phone, "VOICE", OrcaColors.NeonRedPulse, onVoiceClick)
                    ActionChip(Icons.Default.Add, "IMAGE", OrcaColors.CyanIntelligence, onImageClick)
                    ActionChip(Icons.Default.Star, "DEEP", OrcaColors.NeonRed) { }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }, contentAlignment = Alignment.Center) {
                Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Toggle", tint = OrcaColors.WarmGrey.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(8.dp)) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(icon, label, tint = color, modifier = Modifier.size(18.dp)) }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = color.copy(alpha = 0.7f), letterSpacing = 1.sp)
    }
}
