package com.yuhproducts.skusho.data.repository

import com.yuhproducts.skusho.data.source.local.AppPreferences
import com.yuhproducts.skusho.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 設定リポジトリの実装
 */
class SettingsRepositoryImpl @Inject constructor(
    private val appPreferences: AppPreferences
) : SettingsRepository {
    
    override fun getOnboardingCompleted(): Flow<Boolean> = 
        appPreferences.onboardingCompleted
    
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        appPreferences.setOnboardingCompleted(completed)
    }
    
    override fun getImageFormat(): Flow<String> = 
        appPreferences.imageFormat
    
    override suspend fun setImageFormat(format: String) {
        appPreferences.setImageFormat(format)
    }
    
    override fun getImageQuality(): Flow<Int> = 
        appPreferences.imageQuality
    
    override suspend fun setImageQuality(quality: Int) {
        appPreferences.setImageQuality(quality)
    }
    
    override fun getCaptureSoundEnabled(): Flow<Boolean> = 
        appPreferences.captureSoundEnabled
    
    override suspend fun setCaptureSoundEnabled(enabled: Boolean) {
        appPreferences.setCaptureSoundEnabled(enabled)
    }
    
    override fun getContinuousShotCount(): Flow<Int> = 
        appPreferences.continuousShotCount
    
    override suspend fun setContinuousShotCount(count: Int) {
        appPreferences.setContinuousShotCount(count)
    }

    override fun getContinuousShotIntervalMs(): Flow<Int> = 
        appPreferences.continuousShotIntervalMs

    override suspend fun setContinuousShotIntervalMs(intervalMs: Int) {
        appPreferences.setContinuousShotIntervalMs(intervalMs)
    }

    override fun getCaptureUnlockExpiryMillis(): Flow<Long> =
        appPreferences.captureUnlockExpiryMillis

    override suspend fun setCaptureUnlockExpiryMillis(expiryMillis: Long) {
        appPreferences.setCaptureUnlockExpiryMillis(expiryMillis)
    }
}
