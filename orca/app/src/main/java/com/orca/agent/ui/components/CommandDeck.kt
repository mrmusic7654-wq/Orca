// app/src/main/java/com/orca/agent/ui/components/CommandDeck.kt - REPLACE WITH REAL CONTENT
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orca.agent.ui.theme.OrcaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandDeck(
    onTextSubmit: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onCameraClick: () -> Unit,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = OrcaColors.AbyssBlack.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(
                    OrcaColors.NeonRed.copy(alpha = 0.3f),
                    OrcaColors.CyanIntelligence.copy(alpha = 0.1f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Main input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SeeAndAct Camera button
                IconButton(
                    onClick = onCameraClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "See & Act",
                        tint = if (isExpanded) OrcaColors.CyanIntelligence else OrcaColors.CoolGrey,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                // Text input
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    placeholder = {
                        Text(
                            text = "Message Orca...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = OrcaColors.WarmGrey
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OrcaColors.PureWhite,
                        unfocusedTextColor = OrcaColors.CoolGrey,
                        focusedBorderColor = OrcaColors.NeonRed,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = OrcaColors.NeonRed,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                // Send button
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onTextSubmit(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    enabled = textInput.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (textInput.isNotBlank()) OrcaColors.NeonRed else OrcaColors.WarmGrey,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            // Expandable panel
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice input
                    ActionButton(
                        icon = Icons.Default.Mic,
                        label = "VOICE",
                        color = OrcaColors.NeonRedPulse,
                        onClick = onVoiceClick
                    )
                    
                    // Image upload
                    ActionButton(
                        icon = Icons.Default.Image,
                        label = "IMAGE",
                        color = OrcaColors.CyanIntelligence,
                        onClick = onImageClick
                    )
                    
                    // File upload
                    ActionButton(
                        icon = Icons.Default.AttachFile,
                        label = "FILE",
                        color = OrcaColors.CoolGrey,
                        onClick = { }
                    )
                    
                    // Deep Think mode
                    ActionButton(
                        icon = Icons.Default.Psychology,
                        label = "DEEP",
                        color = OrcaColors.NeonRed,
                        onClick = { }
                    )
                }
            }
            
            // Expand/collapse toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle",
                    tint = OrcaColors.WarmGrey.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = color.copy(alpha = 0.7f),
            letterSpacing = 1.sp
        )
    }
}
