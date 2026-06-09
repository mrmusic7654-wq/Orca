// app/src/main/java/com/orca/agent/execution/AppNavigator.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.execution

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.orca.agent.core.AgentAction
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNavigator @Inject constructor(
    private val context: Context,
    private val gestureEngine: GestureEngine,
    private val screenParser: ScreenParser
) {
    suspend fun openApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                delay(1500) // Wait for app to load
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun openUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            delay(1000)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun navigateTo(elementDescription: String, maxAttempts: Int = 5): NavigationResult {
        var attempts = 0
        
        while (attempts < maxAttempts) {
            // Parse current screen
            val screenState = AccessibilityBridge.screenState.first()
            
            // Find the target element
            val target = screenState?.clickableElements?.find { 
                it.description.contains(elementDescription, ignoreCase = true) 
            }
            
            if (target != null) {
                // Tap the element
                val success = gestureEngine.tapAt(target.bounds.centerX(), target.bounds.centerY())
                if (success) {
                    delay(500)
                    return NavigationResult.Success(elementDescription)
                }
            }
            
            // If not found on current screen, try scrolling
            gestureEngine.scrollList(ScrollDirection.DOWN)
            delay(300)
            attempts++
        }
        
        return NavigationResult.NotFound(elementDescription)
    }
    
    suspend fun searchInApp(query: String): Boolean {
        // Look for a search bar/icon
        val searchBar = findElement(listOf("search", "Search", "search icon", "Search field"))
        
        if (searchBar != null) {
            gestureEngine.tapAt(searchBar.bounds.centerX(), searchBar.bounds.centerY())
            delay(300)
            
            // Type the query using Accessibility Service
            AccessibilityBridge.executeAction(AgentAction.Type(query, "Search"))
            delay(200)
            
            // Press enter/search
            // Find and tap the search button
            val searchButton = findElement(listOf("search button", "Search button", "search_btn"))
            if (searchButton != null) {
                gestureEngine.tapAt(searchButton.bounds.centerX(), searchButton.bounds.centerY())
            }
            
            return true
        }
        
        return false
    }
    
    private suspend fun findElement(names: List<String>): ScreenParser.UIElement? {
        val screenState = AccessibilityBridge.screenState.first()
        return screenState?.clickableElements?.find { element ->
            names.any { name ->
                element.description.contains(name, ignoreCase = true) ||
                element.text.contains(name, ignoreCase = true)
            }
        }
    }
}

sealed class NavigationResult {
    data class Success(val element: String) : NavigationResult()
    data class NotFound(val element: String) : NavigationResult()
    data class Error(val message: String) : NavigationResult()
}
