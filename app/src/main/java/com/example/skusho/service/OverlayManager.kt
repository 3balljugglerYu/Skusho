package com.example.skusho.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import com.example.skusho.R
import kotlin.math.abs

class OverlayManager(
    private val context: Context,
    private val onCapture: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isShowing = false
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    
    companion object {
        private const val TAG = "SKUSHO_OVERLAY"
    }
    
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isShowing) return
        
        Log.e(TAG, "show() called")
        
        // オーバーレイビューを作成
        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.overlay_capture_button, null)
        
        Log.e(TAG, "overlayView created: ${overlayView != null}")
        
        val captureButton = overlayView?.findViewById<ImageButton>(R.id.capture_button)
        Log.e(TAG, "captureButton found: ${captureButton != null}")
        
        // レイアウトパラメータを設定
        // FLAG_NOT_TOUCH_MODAL: タッチイベントを受け取るが、外側のタッチは下に通す
        // FLAG_LAYOUT_NO_LIMITS: 画面外にも配置可能
        // FLAG_NOT_FOCUSABLEを削除してタッチイベントを受け取る
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }
        
        Log.e(TAG, "Layout params created")
        
        // ビューをタッチ可能にする
        overlayView?.isClickable = true
        overlayView?.isFocusable = false
        overlayView?.isFocusableInTouchMode = false
        
        Log.e(TAG, "Setting listeners on captureButton directly")
        
        // ボタン自体にクリックリスナーを設定
        captureButton?.setOnClickListener {
            Log.e(TAG, "Button OnClickListener triggered!")
            onCapture()
        }
        
        // ボタン自体にタッチリスナーを設定（ドラッグ機能用）
        var buttonInitialX = 0
        var buttonInitialY = 0
        var buttonInitialTouchX = 0f
        var buttonInitialTouchY = 0f
        var buttonIsDragging = false
        
        captureButton?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.e(TAG, "Button ACTION_DOWN")
                    buttonInitialX = params.x
                    buttonInitialY = params.y
                    buttonInitialTouchX = event.rawX
                    buttonInitialTouchY = event.rawY
                    buttonIsDragging = false
                    false // OnClickListenerも発火させる
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - buttonInitialTouchX
                    val deltaY = event.rawY - buttonInitialTouchY
                    
                    Log.e(TAG, "Button ACTION_MOVE deltaX=$deltaX, deltaY=$deltaY")
                    
                    // 一定距離以上動いたらドラッグとみなす
                    if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                        buttonIsDragging = true
                        params.x = buttonInitialX + deltaX.toInt()
                        params.y = buttonInitialY + deltaY.toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        Log.e(TAG, "Button Dragging, isDragging=$buttonIsDragging")
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    Log.e(TAG, "Button ACTION_UP isDragging=$buttonIsDragging")
                    if (buttonIsDragging) {
                        // ドラッグしていた場合は画面端にスナップ
                        Log.e(TAG, "Button Snapping to edge")
                        snapToEdge(params)
                        buttonIsDragging = false
                        true // クリックイベントをキャンセル
                    } else {
                        buttonIsDragging = false
                        false // OnClickListenerを発火させる
                    }
                }
                else -> {
                    Log.e(TAG, "Button Other action: ${event.action}")
                    false
                }
            }
        }
        
        // ビューを追加
        try {
            Log.e(TAG, "Adding view to WindowManager")
            windowManager.addView(overlayView, params)
            isShowing = true
            Log.e(TAG, "View added successfully, isShowing=$isShowing")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding view: ${e.message}", e)
        }
    }
    
    fun hide() {
        Log.e(TAG, "hide() called")
        overlayView?.visibility = View.INVISIBLE
    }
    
    fun showAfterCapture() {
        Log.e(TAG, "showAfterCapture() called")
        overlayView?.visibility = View.VISIBLE
    }
    
    fun remove() {
        if (isShowing && overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
            isShowing = false
        }
    }
    
    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        
        // 画面の左右どちらに近いか判定
        params.x = if (params.x < screenWidth / 2) {
            0 // 左端
        } else {
            screenWidth - (overlayView?.width ?: 0) // 右端
        }
        
        overlayView?.let {
            windowManager.updateViewLayout(it, params)
        }
    }
}
