package com.orca.agent.execution
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class ScreenCaptureService @Inject constructor(private val ctx: Context) : Service() {
    inner class LocalBinder : Binder() { fun getService() = this@ScreenCaptureService }
    override fun onBind(i: Intent?) = LocalBinder()
    fun startCapture(code: Int, data: Intent) {}
    fun stopCapture() { stopSelf() }
}
