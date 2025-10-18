package com.yuhproducts.skusho.data.source.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * DataStore を使用したローカル設定データソース
 */
class AppPreferences @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val IMAGE_FORMAT = stringPreferencesKey("image_format")
        private val IMAGE_QUALITY = intPreferencesKey("image_quality")
        private val CAPTURE_SOUND_ENABLED = booleanPreferencesKey("capture_sound_enabled")
        private val CONTINUOUS_SHOT_COUNT = intPreferencesKey("continuous_shot_count")
        private val CAPTURE_UNLOCK_EXPIRY = longPreferencesKey("capture_unlock_expiry")
    }
    
    // オンボーディング完了フラグ
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }
    
    // 画像形式（PNG or JPEG）
    val imageFormat: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[IMAGE_FORMAT] ?: "PNG"
    }
    
    suspend fun setImageFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[IMAGE_FORMAT] = format
        }
    }
    
    // 画像品質（1-100）
    val imageQuality: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[IMAGE_QUALITY] ?: 100
    }
    
    suspend fun setImageQuality(quality: Int) {
        context.dataStore.edit { preferences ->
            preferences[IMAGE_QUALITY] = quality.coerceIn(1, 100)
        }
    }
    
    // 撮影音
    val captureSoundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CAPTURE_SOUND_ENABLED] ?: false
    }
    
    suspend fun setCaptureSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CAPTURE_SOUND_ENABLED] = enabled
        }
    }
    
    // 連写枚数（0=OFF, 1-5）
    val continuousShotCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CONTINUOUS_SHOT_COUNT] ?: 0
    }
    
    suspend fun setContinuousShotCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[CONTINUOUS_SHOT_COUNT] = count.coerceIn(0, 5)
        }
    }

    // リワード広告視聴による撮影解放有効期限（Epoch millis）
    val captureUnlockExpiryMillis: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[CAPTURE_UNLOCK_EXPIRY] ?: 0L
    }

    suspend fun setCaptureUnlockExpiryMillis(expiryMillis: Long) {
        context.dataStore.edit { preferences ->
            preferences[CAPTURE_UNLOCK_EXPIRY] = expiryMillis
        }
    }
}
