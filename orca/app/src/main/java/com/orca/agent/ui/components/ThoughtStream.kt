// app/src/main/java/com/orca/agent/ui/components/ThoughtStream.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.orca.agent.ui.theme.OrcaColors
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun ThoughtStream(
    thoughtStream: SharedFlow<String>,
    modifier: Modifier = Modifier
) {
    val thoughts = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    
    LaunchedEffect(Unit) {
        thoughtStream.collect { thought ->
            thoughts.add(thought)
            // Keep only last 100 thoughts
            if (thoughts.size > 100) {
                thoughts.removeAt(0)
            }
        }
    }
    
    // Auto-scroll to bottom
    LaunchedEffect(thoughts.size) {
        if (thoughts.isNotEmpty()) {
            listState.animateScrollToItem(thoughts.size - 1)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                OrcaColors.GlassDark,
                RoundedCornerShape(12.dp)
            )
            .blur(0.5.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(thoughts) { thought ->
                ThoughtLine(thought)
            }
        }
    }
}

@Composable
fun ThoughtLine(thought: String) {
    Text(
        text = "> $thought",
        fontFamily = FontFamily.Monospace,
        fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
        color = OrcaColors.CyanIntelligence.copy(alpha = 0.7f),
        lineHeight = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)
    )
}
