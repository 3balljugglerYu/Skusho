package com.yuhproducts.skusho.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.yuhproducts.skusho.MainActivity
import com.yuhproducts.skusho.R
import com.yuhproducts.skusho.config.RemoteConfigManager
import com.yuhproducts.skusho.data.source.local.AppPreferences
import com.yuhproducts.skusho.util.MediaStoreHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
class CaptureService : Service() {
    
    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager
    
    private var mediaProjection: MediaProjection? = null
    private var overlayManager: OverlayManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val appPreferences by lazy { AppPreferences(applicationContext) }
    private var rewardUnlockMonitorJob: Job? = null
    
    // VirtualDisplayとImageReaderを保持（使い回し）
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screencap_channel"
        const val ACTION_STOP = "com.yuhproducts.skusho.ACTION_STOP"
        const val ACTION_CAPTURE = "com.yuhproducts.skusho.ACTION_CAPTURE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        
        var isRunning = false
            private set
        
        private const val REWARD_UNLOCK_MONITOR_INTERVAL_MILLIS = 1000L
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.e("SkushoCapture", " CaptureService - onCreate() called")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopCapture()
                return START_NOT_STICKY
            }
            ACTION_CAPTURE -> {
                serviceScope.launch {
                    captureScreenshot()
                }
                return START_STICKY
            }
            else -> {
                // 初回起動時：先にフォアグラウンドサービスを開始してからMediaProjectionを初期化
                val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                }
                
                if (resultCode != 0 && resultData != null) {
                    // Android 14+ ではMediaProjection初期化前にstartForegroundが必須
                    startForeground(NOTIFICATION_ID, createNotification())
                    initMediaProjection(resultCode, resultData)
                    showOverlay()
                    
                    // サービス起動完了フラグを設定
                    isRunning = true
                    Log.e("SkushoCapture", " CaptureService - Service started, isRunning = true")
                    startRewardUnlockMonitor()
                }
            }
        }
        return START_STICKY
    }
    
    private fun initMediaProjection(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        // Android 14 (API 34)+ ではコールバック登録が必須
        if (Build.VERSION.SDK_INT >= 34) {
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.e("SkushoCapture", "MediaProjection stopped")
                }
            }, null)
        }
    }
    
    private fun showOverlay() {
        Log.e("SkushoCapture", " CaptureService - showOverlay() called")
        overlayManager = OverlayManager(this) { 
            Log.e("SkushoCapture", " CaptureService - Overlay callback triggered")
            captureScreenshot() 
        }
        overlayManager?.show()
        Log.e("SkushoCapture", " CaptureService - OverlayManager.show() returned")
    }
    
    private fun captureScreenshot() {
        Log.e("SkushoCapture", " CaptureService - captureScreenshot() called")
        serviceScope.launch(Dispatchers.Default) {
            try {
                Log.e("SkushoCapture", " CaptureService - Starting capture process")
                // オーバーレイを一時的に隠す
                launch(Dispatchers.Main) {
                    Log.e("SkushoCapture", " CaptureService - Hiding overlay")
                    overlayManager?.hide()
                }
                
                // 少し待機（オーバーレイが完全に消えるまで）
                delay(200)
                Log.e("SkushoCapture", " CaptureService - Overlay hidden, proceeding with capture")
                
                val projection = mediaProjection ?: throw IllegalStateException("MediaProjection not initialized")
                
                // 初回のみVirtualDisplayとImageReaderを作成
                if (virtualDisplay == null || imageReader == null) {
                    Log.e("SkushoCapture", " CaptureService - First capture, creating VirtualDisplay and ImageReader")
                    
                    // ディスプレイメトリクスを取得
                    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val (width, height, density) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val bounds = windowManager.currentWindowMetrics.bounds
                        Triple(bounds.width(), bounds.height(), resources.displayMetrics.densityDpi)
                    } else {
                        @Suppress("DEPRECATION")
                        val metrics = DisplayMetrics()
                        @Suppress("DEPRECATION")
                        windowManager.defaultDisplay.getMetrics(metrics)
                        Triple(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
                    }
                    
                    // ImageReaderを作成
                    imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
                    
                    // VirtualDisplayを作成
                    Log.e("SkushoCapture", " CaptureService - Creating VirtualDisplay")
                    virtualDisplay = projection.createVirtualDisplay(
                        "ScreenCapture",
                        width,
                        height,
                        density,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader!!.surface,
                        null,
                        null
                    )
                    Log.e("SkushoCapture", " CaptureService - VirtualDisplay created")
                } else {
                    Log.e("SkushoCapture", " CaptureService - Reusing existing VirtualDisplay")
                }
                
                Log.e("SkushoCapture", " CaptureService - Waiting for frame")
                
                // フレームを待機（複数回リトライ）
                var image: Image? = null
                var retryCount = 0
                while (image == null && retryCount < 10) {
                    delay(100)
                    image = imageReader?.acquireLatestImage()
                    retryCount++
                    Log.e("SkushoCapture", " CaptureService - Retry $retryCount, image=$image")
                }
                
                val bitmap = if (image != null) {
                    Log.e("SkushoCapture", " CaptureService - Image acquired, converting to bitmap")
                    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val (width, height) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val bounds = windowManager.currentWindowMetrics.bounds
                        Pair(bounds.width(), bounds.height())
                    } else {
                        @Suppress("DEPRECATION")
                        val metrics = DisplayMetrics()
                        @Suppress("DEPRECATION")
                        windowManager.defaultDisplay.getMetrics(metrics)
                        Pair(metrics.widthPixels, metrics.heightPixels)
                    }
                    imageToBitmap(image, width, height)
                } else {
                    Log.e("SkushoCapture", " CaptureService - Failed to acquire image after $retryCount retries")
                    null
                }
                
                image?.close()
                // VirtualDisplayとImageReaderは使い回すので解放しない
                
                // 保存
                if (bitmap != null) {
                    Log.e("SkushoCapture", " CaptureService - Bitmap captured, size: ${bitmap.width}x${bitmap.height}")
                    val uri = MediaStoreHelper.saveBitmap(this@CaptureService, bitmap)
                    if (uri != null) {
                        Log.e("SkushoCapture", " CaptureService - Saved to MediaStore: $uri")
                        showSuccessNotification(uri.toString())
                    } else {
                        Log.e("SkushoCapture", " CaptureService - Failed to save to MediaStore")
                    }
                    bitmap.recycle()
                } else {
                    Log.e("SkushoCapture", " CaptureService - Bitmap is null")
                }
                
                // オーバーレイを再表示
                launch(Dispatchers.Main) {
                    Log.e("SkushoCapture", " CaptureService - Re-showing overlay")
                    overlayManager?.showAfterCapture()
                }
                
            } catch (e: Exception) {
                Log.e("SkushoCapture", " CaptureService - Error: ${e.message}", e)
                e.printStackTrace()
                // エラーが発生してもオーバーレイは再表示
                launch(Dispatchers.Main) {
                    Log.e("SkushoCapture", " CaptureService - Re-showing overlay after error")
                    overlayManager?.showAfterCapture()
                }
            }
        }
    }
    
    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        
        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        
        return if (rowPadding == 0) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, width, height)
        }
    }
    
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, CaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("スクリーンショット待機中")
            .setContentText("撮影ボタンをタップしてキャプチャ")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "停止",
                stopPendingIntent
            )
            .setOngoing(true)
            .build()
    }
    
    private fun showSuccessNotification(uri: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("スクリーンショット保存完了")
            .setContentText("Pictures/Screenshots に保存されました")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "スクリーンショット",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "画面キャプチャサービスの通知"
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun stopCapture() {
        Log.e("SkushoCapture", " CaptureService - stopCapture() called")
        
        // サービス停止フラグを先に設定
        isRunning = false
        Log.e("SkushoCapture", " CaptureService - isRunning = false")
        
        rewardUnlockMonitorJob?.cancel()
        rewardUnlockMonitorJob = null
        
        overlayManager?.remove()
        overlayManager = null
        
        // VirtualDisplayとImageReaderを解放
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.e("SkushoCapture", " CaptureService - Service stopped")
    }
    
    private fun startRewardUnlockMonitor() {
        rewardUnlockMonitorJob?.cancel()
        rewardUnlockMonitorJob = serviceScope.launch(Dispatchers.Default) {
            while (isActive) {
                // 広告が不要な期間は監視をスキップ
                if (!remoteConfigManager.isAdRequired()) {
                    delay(REWARD_UNLOCK_MONITOR_INTERVAL_MILLIS)
                    continue
                }
                
                val expiryMillis = appPreferences.captureUnlockExpiryMillis.first()
                if (!isRunning) {
                    delay(REWARD_UNLOCK_MONITOR_INTERVAL_MILLIS)
                    continue
                }
                if (expiryMillis <= 0L) {
                    delay(REWARD_UNLOCK_MONITOR_INTERVAL_MILLIS)
                    continue
                }
                val remaining = expiryMillis - System.currentTimeMillis()
                if (remaining <= 0L) {
                    Log.e("SkushoCapture", " CaptureService - Reward unlock expired during background")
                    appPreferences.setCaptureUnlockExpiryMillis(0L)
                    stopCapture()
                    break
                }
                val waitMillis = min(remaining, REWARD_UNLOCK_MONITOR_INTERVAL_MILLIS)
                delay(waitMillis)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.e("SkushoCapture", " CaptureService - onDestroy() called")
        rewardUnlockMonitorJob?.cancel()
        rewardUnlockMonitorJob = null
        if (isRunning) {
            stopCapture()
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
