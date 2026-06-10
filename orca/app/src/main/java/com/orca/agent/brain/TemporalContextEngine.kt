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
    
    private var lastKnownTimezone: String = ""
    private var timezoneChangeDetected = false
    private var vacationMode = false
    
    data class TemporalContext(
        val dayOfWeek: DayCategory = DayCategory.WEEKDAY,
        val timeOfDay: TimeCategory = TimeCategory.MORNING,
        val isHoliday: Boolean = false,
        val isVacation: Boolean = false,
        val timezoneChanged: Boolean = false,
        val userIsLikelyAsleep: Boolean = false,
        val isWeekend: Boolean = false,
        val seasonalContext: SeasonalContext = SeasonalContext.NORMAL
    )
    
    enum class DayCategory { WEEKDAY, WEEKEND, HOLIDAY, VACATION }
    enum class TimeCategory { EARLY_MORNING, MORNING, AFTERNOON, EVENING, NIGHT, LATE_NIGHT }
    enum class SeasonalContext { NORMAL, HOLIDAY_SEASON, SUMMER_VACATION, BACK_TO_SCHOOL }

    // ============================================================
    // TEMPORAL AWARENESS
    // ============================================================
    
    fun updateContext() {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val month = calendar.get(java.util.Calendar.MONTH)
        
        // Determine day category
        val dayCategory = when {
            vacationMode -> DayCategory.VACATION
            isHoliday(month, calendar.get(java.util.Calendar.DAY_OF_MONTH)) -> DayCategory.HOLIDAY
            dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY -> DayCategory.WEEKEND
            else -> DayCategory.WEEKDAY
        }
        
        // Determine time category
        val timeCategory = when (hour) {
            in 0..4 -> TimeCategory.LATE_NIGHT
            in 5..7 -> TimeCategory.EARLY_MORNING
            in 8..11 -> TimeCategory.MORNING
            in 12..16 -> TimeCategory.AFTERNOON
            in 17..20 -> TimeCategory.EVENING
            else -> TimeCategory.NIGHT
        }
        
        // Check if user is likely asleep
        val likelyAsleep = hour in 0..5 && dayCategory != DayCategory.WEEKEND
        
        // Detect timezone change
        val currentTimezone = java.util.TimeZone.getDefault().id
        if (lastKnownTimezone.isNotEmpty() && currentTimezone != lastKnownTimezone) {
            timezoneChangeDetected = true
        } else {
            timezoneChangeDetected = false
        }
        lastKnownTimezone = currentTimezone
        
        _temporalContext.value = TemporalContext(
            dayOfWeek = dayCategory,
            timeOfDay = timeCategory,
            isHoliday = dayCategory == DayCategory.HOLIDAY,
            isVacation = vacationMode,
            timezoneChanged = timezoneChangeDetected,
            userIsLikelyAsleep = likelyAsleep,
            isWeekend = dayCategory == DayCategory.WEEKEND,
            seasonalContext = getSeasonalContext(month)
        )
    }

    // ============================================================
    // VACATION DETECTION
    // ============================================================
    
    fun detectVacationMode(
        locationChanged: Boolean,
        routineDisrupted: Boolean,
        daysAwayFromNormal: Int
    ): Boolean {
        if (locationChanged && routineDisrupted && daysAwayFromNormal >= 3) {
            vacationMode = true
            return true
        }
        
        if (!locationChanged && daysAwayFromNormal == 0) {
            vacationMode = false
        }
        
        return vacationMode
    }

    // ============================================================
    // ADAPTIVE BEHAVIOR
    // ============================================================
    
    fun shouldBeQuiet(): Boolean {
        val ctx = _temporalContext.value
        return ctx.userIsLikelyAsleep || 
               ctx.timeOfDay == TimeCategory.LATE_NIGHT ||
               ctx.isVacation
    }
    
    fun shouldSuppressPredictions(): Boolean {
        val ctx = _temporalContext.value
        return ctx.isWeekend || 
               ctx.isHoliday || 
               ctx.isVacation ||
               ctx.timezoneChanged
    }
    
    fun getOptimalInteractionStyle(): InteractionStyle {
        val ctx = _temporalContext.value
        
        return when {
            ctx.userIsLikelyAsleep -> InteractionStyle.SILENT
            ctx.isVacation -> InteractionStyle.MINIMAL
            ctx.isWeekend -> InteractionStyle.RELAXED
            ctx.timeOfDay == TimeCategory.MORNING -> InteractionStyle.EFFICIENT
            ctx.timezoneChanged -> InteractionStyle.GENTLE
            else -> InteractionStyle.NORMAL
        }
    }
    
    fun shouldDelayNonUrgentTasks(): Boolean {
        val ctx = _temporalContext.value
        return ctx.userIsLikelyAsleep || ctx.isVacation
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun isHoliday(month: Int, day: Int): Boolean {
        // Major US holidays - expand based on user's region
        val holidays = mapOf(
            0 to listOf(1),    // Jan 1 - New Year
            4 to listOf(4),    // May 4 - placeholder
            6 to listOf(4),    // Jul 4
            9 to listOf(31),   // Oct 31
            10 to listOf(11),  // Nov 11
            11 to listOf(25)   // Dec 25
        )
        return holidays[month]?.contains(day) == true
    }
    
    private fun getSeasonalContext(month: Int): SeasonalContext {
        return when (month) {
            in 10..11 -> SeasonalContext.HOLIDAY_SEASON
            in 5..7 -> SeasonalContext.SUMMER_VACATION
            in 7..8 -> SeasonalContext.BACK_TO_SCHOOL
            else -> SeasonalContext.NORMAL
        }
    }
}

enum class InteractionStyle {
    NORMAL, EFFICIENT, RELAXED, MINIMAL, SILENT, GENTLE
}
