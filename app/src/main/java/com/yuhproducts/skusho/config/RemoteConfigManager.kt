package com.yuhproducts.skusho.config

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor() {
    
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    
    init {
        // Remote Configの設定
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 本番: 3600秒（1時間）
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // デフォルト値を設定
        setDefaultValues()
    }
    
    private fun setDefaultValues() {
        val defaults = mapOf(
            "ad_required_from_date" to "2025-12-01T00:00:00Z",
            "ad_free_mode_enabled" to false,
            "ad_free_message" to "無料期間中です。広告なしで撮影できます！",
            "ad_required_message" to "広告を視聴して撮影機能を有効にしてください"
        )
        remoteConfig.setDefaultsAsync(defaults)
    }
    
    /**
     * Remote Configから値を取得
     */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 文字列値を取得
     */
    fun getString(key: String): String {
        return remoteConfig.getString(key)
    }
    
    /**
     * ブール値を取得
     */
    fun getBoolean(key: String): Boolean {
        return remoteConfig.getBoolean(key)
    }
    
    /**
     * 数値を取得
     */
    fun getLong(key: String): Long {
        return remoteConfig.getLong(key)
    }
    
    /**
     * 整数値を取得
     */
    fun getInt(key: String): Int {
        return remoteConfig.getLong(key).toInt()
    }
    
    /**
     * 最後に取得した時刻を取得
     */
    fun getLastFetchTime(): Long {
        return remoteConfig.info.fetchTimeMillis
    }
    
    /**
     * 最後に取得したステータスを取得
     */
    fun getLastFetchStatus(): Int {
        return remoteConfig.info.lastFetchStatus
    }
    
    /**
     * 広告視聴が必須かどうかを判定
     * @return true: 広告視聴必須, false: 広告なしで使用可能
     */
    fun isAdRequired(): Boolean {
        // 緊急フラグが有効な場合は広告不要
        if (getBoolean("ad_free_mode_enabled")) {
            Log.d(TAG, "Ad-free mode is enabled via remote config")
            return false
        }
        
        // 日付で判定
        val adRequiredFromDateStr = getString("ad_required_from_date")
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val adRequiredFromDate = formatter.parse(adRequiredFromDateStr)
            val currentDate = Date()
            val isRequired = currentDate.time >= (adRequiredFromDate?.time ?: Long.MAX_VALUE)
            
            Log.d(TAG, "Ad required check - Current: ${formatter.format(currentDate)}, " +
                    "Required from: $adRequiredFromDateStr, Is required: $isRequired")
            
            isRequired
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse ad_required_from_date: $adRequiredFromDateStr", e)
            // パースエラー時はデフォルトで広告必須とする（安全側に倒す）
            true
        }
    }
    
    /**
     * 現在の状態に応じたメッセージを取得
     */
    fun getAdStatusMessage(): String {
        return if (isAdRequired()) {
            getString("ad_required_message")
        } else {
            getString("ad_free_message")
        }
    }
    
    companion object {
        private const val TAG = "RemoteConfigManager"
    }
}
