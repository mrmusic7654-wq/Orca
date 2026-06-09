// app/src/main/java/com/orca/agent/execution/ScreenCaptureService.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.execution

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import android.util.Base64
import android.view.WindowManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenCaptureService @Inject constructor(
    private val context: Context
) : Service() {
    
    private val binder = LocalBinder()
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val _screenshots = MutableSharedFlow<Bitmap>(replay = 1)
    val screenshots: SharedFlow<Bitmap> = _screenshots.asSharedFlow()
    
    private var isCapturing = false
    
    inner class LocalBinder : Binder() {
        fun getService(): ScreenCaptureService = this@ScreenCaptureService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    fun startCapture(resultCode: Int, data: Intent) {
        if (isCapturing) return
        
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "OrcaScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null, null
        )
        
        isCapturing = true
        startImageCapture()
    }
    
    private fun startImageCapture() {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            while (isActive && isCapturing) {
                captureScreen()
                delay(500) // Capture every 500ms for real-time feedback
            }
        }
    }
    
    private suspend fun captureScreen() {
        try {
            val image = imageReader?.acquireLatestImage() ?: return
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            _screenshots.emit(bitmap)
            
            image.close()
        } catch (e: Exception) {
            // Silently handle capture errors
        }
    }
    
    fun getLatestScreenshotBase64(): String {
        // Return base64 encoded screenshot for Gemini Vision API
        return "" // Implement with actual screenshot capture
    }
    
    fun stopCapture() {
        isCapturing = false
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        stopSelf()
    }
    
    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }
}
