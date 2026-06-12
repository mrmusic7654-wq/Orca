package com.orca.agent.brain
import android.app.Notification
import com.orca.agent.core.ScreenState
import com.orca.agent.brain.PredictedIntent
import com.orca.agent.memory.MemoryCortex
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class IntentPredictor @Inject constructor(private val mc: MemoryCortex) {
    fun initialize() {}
    suspend fun predict(screen: ScreenState): List<PredictedIntent> = emptyList()
    fun evaluateNotificationImportance(n: Notification): Float {
        val title = n.extras.getString(Notification.EXTRA_TITLE) ?: ""
        return if (listOf("Mom","Partner","Boss").any { title.contains(it,true) }) 0.95f else 0.3f
    }
}
