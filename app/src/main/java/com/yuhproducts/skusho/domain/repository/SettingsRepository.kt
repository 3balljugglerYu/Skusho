package com.yuhproducts.skusho.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 設定データへのアクセスを抽象化するリポジトリインターフェース
 */
interface SettingsRepository {
    
    // オンボーディング
    fun getOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    
    // 画像形式
    fun getImageFormat(): Flow<String>
    suspend fun setImageFormat(format: String)
    
    // 画像品質
    fun getImageQuality(): Flow<Int>
    suspend fun setImageQuality(quality: Int)
    
    // 撮影音
    fun getCaptureSoundEnabled(): Flow<Boolean>
    suspend fun setCaptureSoundEnabled(enabled: Boolean)
    
    // 連写枚数
    fun getContinuousShotCount(): Flow<Int>
    suspend fun setContinuousShotCount(count: Int)

    // リワード広告視聴による撮影解放有効期限
    fun getCaptureUnlockExpiryMillis(): Flow<Long>
    suspend fun setCaptureUnlockExpiryMillis(expiryMillis: Long)
}
