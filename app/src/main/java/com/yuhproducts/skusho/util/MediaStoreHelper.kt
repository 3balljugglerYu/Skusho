package com.yuhproducts.skusho.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaStoreHelper {
    
    /**
     * Bitmapを MediaStore に保存
     * @param context コンテキスト
     * @param bitmap 保存するBitmap
     * @param format 画像形式（"PNG" or "JPEG"）
     * @param quality JPEG品質（1-100）
     * @param sequenceNumber 連番（連写時の順序を保証）
     * @param captureTimestamp 撮影時のタイムスタンプ（連写時の順序保証用）
     * @return 保存されたURI、失敗時はnull
     */
    fun saveBitmap(
        context: Context,
        bitmap: Bitmap,
        format: String = "PNG",
        quality: Int = 100,
        sequenceNumber: Int? = null,
        captureTimestamp: Long = System.currentTimeMillis()
    ): Uri? {
        Log.e("SkushoMediaStore", " MediaStoreHelper - saveBitmap called, format=$format, quality=$quality, sequenceNumber=$sequenceNumber, captureTimestamp=$captureTimestamp")
        val filename = generateFilename(format, sequenceNumber)
        Log.e("SkushoMediaStore", " MediaStoreHelper - Filename: $filename")
        val mimeType = when (format.uppercase()) {
            "JPEG", "JPG" -> "image/jpeg"
            else -> "image/png"
        }
        Log.e("SkushoMediaStore", " MediaStoreHelper - MimeType: $mimeType")
        
        // 連番がある場合は、撮影時のタイムスタンプに連番を加算して順序を保証
        val dateTaken = if (sequenceNumber != null) {
            captureTimestamp + (sequenceNumber - 1)  // 1枚目=+0, 2枚目=+1, 3枚目=+2...
        } else {
            captureTimestamp
        }
        Log.e("SkushoMediaStore", " MediaStoreHelper - DATE_TAKEN: $dateTaken (base: $captureTimestamp, offset: ${if (sequenceNumber != null) sequenceNumber - 1 else 0})")
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Screenshots")
            put(MediaStore.Images.Media.DATE_TAKEN, dateTaken)
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        Log.e("SkushoMediaStore", " MediaStoreHelper - URI created: $uri")
        
        uri?.let {
            try {
                Log.e("SkushoMediaStore", " MediaStoreHelper - Opening output stream")
                resolver.openOutputStream(it)?.use { outputStream ->
                    val compressFormat = when (format.uppercase()) {
                        "JPEG", "JPG" -> Bitmap.CompressFormat.JPEG
                        else -> Bitmap.CompressFormat.PNG
                    }
                    Log.e("SkushoMediaStore", " MediaStoreHelper - Compressing bitmap, format=$compressFormat")
                    bitmap.compress(compressFormat, quality, outputStream)
                    Log.e("SkushoMediaStore", " MediaStoreHelper - Bitmap compressed successfully")
                }
                
                // IS_PENDINGフラグを解除（ギャラリーに即時反映）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Log.e("SkushoMediaStore", " MediaStoreHelper - Clearing IS_PENDING flag")
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                
                Log.e("SkushoMediaStore", " MediaStoreHelper - Save successful, returning URI: $it")
                return it
            } catch (e: Exception) {
                Log.e("SkushoMediaStore", " MediaStoreHelper - Error saving: ${e.message}")
                e.printStackTrace()
                // エラー時はURIを削除
                resolver.delete(it, null, null)
                return null
            }
        }
        
        Log.e("SkushoMediaStore", " MediaStoreHelper - URI is null, returning null")
        return null
    }
    
    /**
     * ファイル名を生成
     * 形式: yyyyMMdd_HHmmss_SSS_seq.ext (連番付き) or yyyyMMdd_HHmmss_SSS.ext
     */
    private fun generateFilename(format: String, sequenceNumber: Int? = null): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())
            .format(Date())
        val extension = when (format.uppercase()) {
            "JPEG", "JPG" -> "jpg"
            else -> "png"
        }
        
        return if (sequenceNumber != null) {
            // 連写時は連番を追加（例: Screenshot_20231225_120000_001_01.png）
            "Screenshot_${timestamp}_${String.format("%02d", sequenceNumber)}.$extension"
        } else {
            "Screenshot_$timestamp.$extension"
        }
    }
}

