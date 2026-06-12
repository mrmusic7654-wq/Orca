package com.orca.agent.execution
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class AccessibilityServiceWatchdog @Inject constructor(private val ctx: Context) {
    fun startWatching() {}; fun stopWatching() {}; fun isServiceHealthy() = true
}
