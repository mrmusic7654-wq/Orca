package com.orca.agent.brain

import android.app.Notification
import com.orca.agent.core.ScreenState
import com.orca.agent.memory.MemoryCortex
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentPredictor @Inject constructor(private val memoryCortex: MemoryCortex) {
    fun initialize() {}

    suspend fun predict(currentScreen: ScreenState): List<PredictedIntent> {
        val predictions = mutableListOf<PredictedIntent>()
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour in 7..9) predictions.add(PredictedIntent("Open Uber for commute", 0.85f, "Morning commute"))
        return predictions
    }

    fun evaluateNotificationImportance(notification: Notification): Float {
        val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
        val importantContacts = listOf("Mom", "Partner", "Boss")
        for (contact in importantContacts) {
            if (title.contains(contact, ignoreCase = true)) return 0.95f
        }
        return 0.3f
    }
}
