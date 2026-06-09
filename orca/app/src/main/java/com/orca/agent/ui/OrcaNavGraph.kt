// app/src/main/java/com/orca/agent/ui/OrcaNavGraph.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.orca.agent.ui.screens.*

@Composable
fun OrcaNavGraph() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        // Main Orb Screen
        composable("main") {
            MainOrbScreen(
                onNavigateToChat = {
                    navController.navigate("chat/new_session")
                },
                onNavigateToMemory = {
                    navController.navigate("memory_stream")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        
        // Chat Screen
        composable(
            route = "chat/{sessionId}",
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: "new"
            ChatScreen(
                sessionId = sessionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Memory Stream Screen
        composable("memory_stream") {
            MemoryStreamScreen(
                sessions = emptyList(), // Injected via ViewModel
                onSessionClick = { session ->
                    navController.navigate("chat/${session.id}")
                },
                onForkClick = { session ->
                    navController.navigate("chat/fork_${session.id}")
                },
                onMergeClick = { target, source ->
                    // Handle merge logic via ViewModel
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Settings Screen
        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Task Timeline / War Room
        composable("war_room/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            WarRoomScreen(
                sessionId = sessionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
