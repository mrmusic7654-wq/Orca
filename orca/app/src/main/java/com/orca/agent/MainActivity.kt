package com.orca.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.orca.agent.ui.OrcaNavGraph
import com.orca.agent.ui.theme.AbyssalNeonTheme
import com.orca.agent.ui.theme.OrcaColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AbyssalNeonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = OrcaColors.VantaBlack
                ) {
                    OrcaNavGraph()
                }
            }
        }
    }
}
