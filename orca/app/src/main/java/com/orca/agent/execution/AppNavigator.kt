package com.orca.agent.execution
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class AppNavigator @Inject constructor(private val ctx: Context, private val ge: GestureEngine, private val sp: ScreenParser) {
    suspend fun openApp(pkg: String): Boolean {
        return try { ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(it); delay(1500); true } ?: false } catch (e: Exception) { false }
    }
}
