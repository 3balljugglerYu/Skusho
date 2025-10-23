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
            // 撮影したBitmapを一時保存するリスト（エラー時のクリーンアップのためtryブロック外で宣言）
            val capturedBitmaps = mutableListOf<Bitmap>()
            
            try {
                Log.e("SkushoCapture", " CaptureService - Starting capture process")
                
                // オーバーレイを一時的に隠す
                launch(Dispatchers.Main) {
                    Log.e("SkushoCapture", " CaptureService - Hiding overlay")
                    overlayManager?.hide()
                }
                
                // 最適化: 待機時間を短縮（100ms → 50ms）
                delay(50)
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
                
                // 連写設定を取得
                val continuousShotCount = appPreferences.continuousShotCount.first()
                val shotCount = if (continuousShotCount > 0) continuousShotCount else 1
                Log.e("SkushoCapture", " CaptureService - Continuous shot count: $shotCount")
                
                // 撮影開始時のタイムスタンプを記録（順序保証用）
                val captureTimestamp = System.currentTimeMillis()
                Log.e("SkushoCapture", " CaptureService - Capture timestamp: $captureTimestamp")
                
                // 連写設定の間隔を取得
                val shotIntervalMs = appPreferences.continuousShotIntervalMs.first()
                Log.e("SkushoCapture", " CaptureService - Shot interval: ${shotIntervalMs}ms")
                
                // 連写処理：撮影のみを高速で実行
                repeat(shotCount) { index ->
                    Log.e("SkushoCapture", " CaptureService - Capturing shot ${index + 1}/$shotCount")
                    
                    // 2枚目以降は設定された間隔で待機
                    if (index > 0) {
                        delay(shotIntervalMs.toLong())
                    }
                    
                    Log.e("SkushoCapture", " CaptureService - Waiting for frame")
                    
                    // フレームを待機（1枚目は最速化）
                    var image: Image? = null
                    var retryCount = 0
                    val maxRetries = if (index == 0) 5 else 3  // 1枚目は5回に短縮
                    while (image == null && retryCount < maxRetries) {
                        delay(if (index == 0) 50 else 50)  // 1枚目も50msに短縮
                        image = imageReader?.acquireLatestImage()
                        retryCount++
                        Log.e("SkushoCapture", " CaptureService - Retry $retryCount/$maxRetries, image=$image")
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
                    
                    // Bitmapをリストに追加（保存処理は後で一括実行）
                    if (bitmap != null) {
                        capturedBitmaps.add(bitmap)
                        Log.e("SkushoCapture", " CaptureService - Bitmap captured and queued, size: ${bitmap.width}x${bitmap.height}")
                    } else {
                        Log.e("SkushoCapture", " CaptureService - Bitmap is null")
                    }
                }
                
                Log.e("SkushoCapture", " CaptureService - All shots captured: ${capturedBitmaps.size}/$shotCount")
                
                // オーバーレイを先に再表示（保存処理を待たない）
                launch(Dispatchers.Main) {
                    Log.e("SkushoCapture", " CaptureService - Re-showing overlay")
                    overlayManager?.showAfterCapture()
                }
                
                // 保存処理をバックグラウンドで実行
                if (capturedBitmaps.isNotEmpty()) {
                    launch(Dispatchers.IO) {
                        var savedCount = 0
                        // 連番を付けて順次保存（forEachIndexedで順序を保証）
                        capturedBitmaps.forEachIndexed { index, bitmap ->
                            // 連写時のみ連番を付ける（1枚のみの場合は連番なし）
                            val sequenceNumber = if (capturedBitmaps.size > 1) index + 1 else null
                            val uri = MediaStoreHelper.saveBitmap(
                                this@CaptureService, 
                                bitmap,
                                sequenceNumber = sequenceNumber,
                                captureTimestamp = captureTimestamp
                            )
                            if (uri != null) {
                                savedCount++
                                Log.e("SkushoCapture", " CaptureService - Saved to MediaStore: $uri (seq: $sequenceNumber)")
                            } else {
                                Log.e("SkushoCapture", " CaptureService - Failed to save to MediaStore")
                            }
                            bitmap.recycle()
                        }
                        
                        // 保存完了後に通知を表示
                        if (savedCount > 0) {
                            Log.e("SkushoCapture", " CaptureService - All bitmaps saved: $savedCount/${capturedBitmaps.size}")
                            showSuccessNotification(savedCount)
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e("SkushoCapture", " CaptureService - Error: ${e.message}", e)
                e.printStackTrace()
                
                // エラー時にBitmapをクリーンアップ
                val bitmapCount = capturedBitmaps.size
                capturedBitmaps.forEach { bitmap ->
                    bitmap.recycle()
                }
                capturedBitmaps.clear()
                Log.e("SkushoCapture", " CaptureService - Cleaned up $bitmapCount bitmaps after error")
                
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
    
    private fun showSuccessNotification(shotCount: Int = 1) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val contentText = if (shotCount > 1) {
            "${shotCount}枚のスクリーンショットを Pictures/Screenshots に保存しました"
        } else {
            "Pictures/Screenshots に保存されました"
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("スクリーンショット保存完了")
            .setContentText(contentText)
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
