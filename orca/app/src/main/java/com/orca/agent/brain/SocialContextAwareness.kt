package com.orca.agent.brain

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialContextAwareness @Inject constructor(
    private val context: Context
) {
    private val _socialContext = MutableStateFlow(SocialContext())
    val socialContext: StateFlow<SocialContext> = _socialContext.asStateFlow()
    private val _privacyMode = MutableStateFlow(PrivacyMode.NORMAL)
    val privacyMode: StateFlow<PrivacyMode> = _privacyMode.asStateFlow()
    private val _deadManSwitch = MutableStateFlow("ACTIVE")
    val deadManSwitch: StateFlow<String> = _deadManSwitch.asStateFlow()
    private var lastUserInteraction = System.currentTimeMillis()

    fun startMonitoring() {
        CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val cal = java.util.Calendar.getInstance()
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val isMeeting = hour in 9..17
                val isAlone = hour in 22..23 || hour in 0..5
                _socialContext.value = SocialContext(isInMeeting = isMeeting, isAlone = isAlone, hourOfDay = hour)
                checkUserAlive()
                delay(30000)
            }
        }
    }

    private fun checkUserAlive() {
        val hoursSince = (System.currentTimeMillis() - lastUserInteraction) / (1000 * 60 * 60)
        _deadManSwitch.value = when {
            hoursSince >= 336 -> "EMERGENCY_SHUTDOWN"
            hoursSince >= 168 -> "CRITICAL_WARNING"
            hoursSince >= 72 -> "WARNING"
            hoursSince >= 24 -> "NO_RECENT_ACTIVITY"
            else -> "ACTIVE"
        }
    }

    fun recordUserInteraction() { lastUserInteraction = System.currentTimeMillis() }
    fun detect(): String = "alone"
    fun shouldSpeakAloud(): Boolean = _privacyMode.value == PrivacyMode.NORMAL
    fun shouldShowNotification(): Boolean = _privacyMode.value != PrivacyMode.SILENT
}

data class SocialContext(
    val isInMeeting: Boolean = false,
    val isInPublic: Boolean = false,
    val screenIsVisible: Boolean = false,
    val isAlone: Boolean = true,
    val hourOfDay: Int = 0
)

enum class PrivacyMode { NORMAL, DISCREET, SILENT }
