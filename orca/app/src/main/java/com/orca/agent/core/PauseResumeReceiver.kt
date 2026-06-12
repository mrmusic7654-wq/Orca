package com.orca.agent.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*

class PauseResumeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.orca.agent.RESUME_TASK" -> { /* Handle resume */ }
            "com.orca.agent.CANCEL_TASK" -> { /* Handle cancel */ }
        }
    }
}
