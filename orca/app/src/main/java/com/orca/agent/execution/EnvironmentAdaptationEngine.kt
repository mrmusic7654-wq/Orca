package com.orca.agent.execution

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvironmentAdaptationEngine @Inject constructor(
    private val context: Context
) {
    private val _environmentProfile = MutableStateFlow(EnvironmentProfile())
    val environmentProfile: StateFlow<EnvironmentProfile> = _environmentProfile.asStateFlow()

    fun buildProfile(): EnvironmentProfile {
        val resources = context.resources
        val config = resources.configuration
        val metrics = resources.displayMetrics

        return EnvironmentProfile(
            fontSize = config.fontScale,
            displayDensity = metrics.density,
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels,
            deviceLanguage = config.locales[0]?.language ?: "en",
            isRightToLeft = config.layoutDirection == Configuration.LAYOUT_DIRECTION_RTL,
            hasAccessibilityFont = config.fontScale > 1.3f
        )
    }

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun adaptCoordinatesForEnvironment(x: Int, y: Int): Pair<Int, Int> {
        val profile = _environmentProfile.value
        val adaptedX = if (profile.isRightToLeft) profile.screenWidth - x else x
        return Pair(adaptedX.coerceIn(0, profile.screenWidth), y.coerceIn(0, profile.screenHeight))
    }
}

data class EnvironmentProfile(
    val fontSize: Float = 1.0f,
    val displayDensity: Float = 1.0f,
    val screenWidth: Int = 1080,
    val screenHeight: Int = 2400,
    val deviceLanguage: String = "en",
    val isRightToLeft: Boolean = false,
    val hasAccessibilityFont: Boolean = false,
    val hasColorInversion: Boolean = false
)
