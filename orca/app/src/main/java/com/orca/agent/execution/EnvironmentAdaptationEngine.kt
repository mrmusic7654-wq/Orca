package com.orca.agent.execution

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.provider.Settings
import com.orca.agent.core.AgentAction
import com.orca.agent.core.ScreenState
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvironmentAdaptationEngine @Inject constructor(
    private val context: Context
) {
    private val _environmentProfile = MutableStateFlow(EnvironmentProfile())
    val environmentProfile: StateFlow<EnvironmentProfile> = _environmentProfile.asStateFlow()
    
    private val unstableApps = mutableSetOf<String>()
    
    data class EnvironmentProfile(
        val fontSize: Float = 1.0f, // 1.0 = normal
        val displayDensity: Float = 1.0f,
        val screenWidth: Int = 1080,
        val screenHeight: Int = 2400,
        val deviceLanguage: String = "en",
        val isRightToLeft: Boolean = false,
        val hasAccessibilityFont: Boolean = false,
        val hasColorInversion: Boolean = false,
        val hasColorCorrection: Boolean = false,
        val installedApps: List<String> = emptyList(),
        val recentlyUninstalledApps: List<String> = emptyList()
    )

    // ============================================================
    // ENVIRONMENT DETECTION
    // ============================================================
    
    fun buildEnvironmentProfile(): EnvironmentProfile {
        val resources = context.resources
        val configuration = resources.configuration
        
        // Detect font scale
        val fontScale = configuration.fontScale
        
        // Detect display density
        val density = resources.displayMetrics.density
        
        // Detect screen size
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        
        // Detect language
        val language = configuration.locales[0]?.language ?: "en"
        
        // Detect RTL
        val isRtl = configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
        
        // Detect accessibility settings
        val hasAccessibilityFont = fontScale > 1.3f
        val hasColorInversion = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0
        ) == 1
        
        // Get installed apps
        val installedApps = getInstalledApps()
        
        return EnvironmentProfile(
            fontSize = fontScale,
            displayDensity = density,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            deviceLanguage = language,
            isRightToLeft = isRtl,
            hasAccessibilityFont = hasAccessibilityFont,
            hasColorInversion = hasColorInversion,
            installedApps = installedApps
        )
    }

    // ============================================================
    // APP EXISTENCE VERIFICATION
    // ============================================================
    
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    fun getAppVersion(packageName: String): String? {
        return try {
            val info = context.packageManager.getPackageInfo(packageName, 0)
            info.versionName ?: info.longVersionCode.toString()
        } catch (e: Exception) {
            null
        }
    }
    
    fun isAppStable(packageName: String): Boolean {
        return packageName !in unstableApps
    }
    
    fun markAppAsUnstable(packageName: String, reason: String) {
        unstableApps.add(packageName)
    }

    // ============================================================
    // COORDINATE ADAPTATION
    // ============================================================
    
    fun adaptCoordinatesForEnvironment(x: Int, y: Int): Pair<Int, Int> {
        val profile = _environmentProfile.value
        var adaptedX = x
        var adaptedY = y
        
        // Adapt for font scaling
        if (profile.hasAccessibilityFont) {
            // Larger fonts push elements down and right
            adaptedY = (y * profile.fontSize).toInt()
        }
        
        // Adapt for RTL layouts
        if (profile.isRightToLeft) {
            adaptedX = profile.screenWidth - x
        }
        
        // Clamp to screen bounds
        adaptedX = adaptedX.coerceIn(0, profile.screenWidth)
        adaptedY = adaptedY.coerceIn(0, profile.screenHeight)
        
        return Pair(adaptedX, adaptedY)
    }

    // ============================================================
    // MULTI-LINGUAL SUPPORT
    // ============================================================
    
    fun getLocalizedButtonTexts(englishText: String): List<String> {
        // Common button translations for major languages
        val translations = mapOf(
            "submit" to listOf("submit", "enviar", "soumettre", "einreichen", "invio", "送信"),
            "cancel" to listOf("cancel", "cancelar", "annuler", "abbrechen", "annulla", "キャンセル"),
            "ok" to listOf("ok", "aceptar", "d'accord", "okay", "va bene", "確認"),
            "next" to listOf("next", "siguiente", "suivant", "weiter", "successivo", "次へ"),
            "back" to listOf("back", "atrás", "retour", "zurück", "indietro", "戻る"),
            "search" to listOf("search", "buscar", "rechercher", "suchen", "cerca", "検索"),
            "save" to listOf("save", "guardar", "enregistrer", "speichern", "salva", "保存"),
            "delete" to listOf("delete", "eliminar", "supprimer", "löschen", "elimina", "削除"),
            "login" to listOf("login", "iniciar sesión", "connexion", "anmelden", "accesso", "ログイン"),
            "sign in" to listOf("sign in", "iniciar sesión", "se connecter", "anmelden", "accedi", "サインイン")
        )
        
        return translations[englishText.lowercase()] ?: listOf(englishText)
    }

    // ============================================================
    // LOGIN WALL DETECTION
    // ============================================================
    
    fun detectLoginWall(screen: ScreenState): Boolean {
        val loginIndicators = listOf(
            "sign in", "log in", "login", "password", "username",
            "email address", "forgot password", "create account",
            "iniciar sesión", "connexion", "anmelden"
        )
        
        val screenText = screen.visibleText.lowercase()
        val hasLoginFields = screen.editableElements.size >= 2 // Usually email + password
        val hasLoginText = loginIndicators.any { screenText.contains(it) }
        
        return hasLoginFields && hasLoginText
    }
    
    fun isSessionExpiredLogin(screen: ScreenState, expectedApp: String): Boolean {
        // Check if we're on a login screen for an app we expected to be logged into
        return detectLoginWall(screen) && screen.currentApp == expectedApp
    }

    // ============================================================
    // OVERLAY DETECTION
    // ============================================================
    
    fun detectScreenOverlay(screen: ScreenState): OverlayInfo? {
        // Check for common overlay apps
        val overlayIndicators = listOf(
            "twilight", "night shift", "blue light", "flux",
            "overlay", "filter"
        )
        
        // Check if there's a translucent layer
        val suspiciousElements = screen.clickableElements.filter {
            it.className.contains("overlay", true) ||
            it.className.contains("filter", true)
        }
        
        if (suspiciousElements.isNotEmpty()) {
            return OverlayInfo(
                detected = true,
                type = "Screen overlay/filter detected",
                mayAffectColors = true,
                mayAffectCoordinates = false
            )
        }
        
        return null
    }

    // ============================================================
    // NOTIFICATION STORM DETECTION
    // ============================================================
    
    private val notificationTimestamps = mutableListOf<Long>()
    
    fun isNotificationStorm(): Boolean {
        val now = System.currentTimeMillis()
        val windowMs = 60000L // 1 minute
        
        // Remove old timestamps
        notificationTimestamps.removeAll { now - it > windowMs }
        
        // More than 20 notifications in 1 minute = storm
        return notificationTimestamps.size > 20
    }
    
    fun recordNotification() {
        notificationTimestamps.add(System.currentTimeMillis())
        if (notificationTimestamps.size > 100) {
            notificationTimestamps.removeAt(0)
        }
    }
    
    fun shouldThrottleAutoReply(): Boolean {
        return isNotificationStorm()
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun getInstalledApps(): List<String> {
        return try {
            context.packageManager.getInstalledApplications(0)
                .map { it.packageName }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class OverlayInfo(
    val detected: Boolean,
    val type: String,
    val mayAffectColors: Boolean,
    val mayAffectCoordinates: Boolean
)
