package com.orca.agent.brain

import android.content.Context
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemporalContextEngine @Inject constructor(
    private val context: Context
) {
    private val _temporalContext = MutableStateFlow(TemporalContext())
    val temporalContext: StateFlow<TemporalContext> = _temporalContext.asStateFlow()

    data class TemporalContext(
        val dayOfWeek: String = "WEEKDAY",
        val timeOfDay: String = "MORNING",
        val isHoliday: Boolean = false,
        val isVacation: Boolean = false,
        val timezoneChanged: Boolean = false,
        val userIsLikelyAsleep: Boolean = false
    )

    fun updateContext() {
        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val day = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val isWeekend = day == java.util.Calendar.SATURDAY || day == java.util.Calendar.SUNDAY

        _temporalContext.value = TemporalContext(
            dayOfWeek = if (isWeekend) "WEEKEND" else "WEEKDAY",
            timeOfDay = when (hour) { in 0..5 -> "NIGHT"; in 6..11 -> "MORNING"; in 12..17 -> "AFTERNOON"; else -> "EVENING" },
            userIsLikelyAsleep = hour in 0..5
        )
    }

    fun shouldBeQuiet(): Boolean {
        return _temporalContext.value.userIsLikelyAsleep
    }

    fun shouldSuppressPredictions(): Boolean {
        return _temporalContext.value.isWeekend || _temporalContext.value.isVacation
    }
}

internal val TemporalContext.isWeekend: Boolean get() = dayOfWeek == "WEEKEND"
internal val TemporalContext.isVacation: Boolean get() = isVacation
