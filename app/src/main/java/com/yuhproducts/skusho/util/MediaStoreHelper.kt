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
     * @return 保存されたURI、失敗時はnull
     */
    fun saveBitmap(
        context: Context,
        bitmap: Bitmap,
        format: String = "PNG",
        quality: Int = 100
    ): Uri? {
        Log.e("SkushoMediaStore", " MediaStoreHelper - saveBitmap called, format=$format, quality=$quality")
        val filename = generateFilename(format)
        Log.e("SkushoMediaStore", " MediaStoreHelper - Filename: $filename")
        val mimeType = when (format.uppercase()) {
            "JPEG", "JPG" -> "image/jpeg"
            else -> "image/png"
        }
        Log.e("SkushoMediaStore", " MediaStoreHelper - MimeType: $mimeType")
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Screenshots")
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
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
     * 形式: yyyyMMdd_HHmmss_SSS.ext
     */
    private fun generateFilename(format: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())
            .format(Date())
        val extension = when (format.uppercase()) {
            "JPEG", "JPG" -> "jpg"
            else -> "png"
        }
        return "Screenshot_$timestamp.$extension"
    }
}

