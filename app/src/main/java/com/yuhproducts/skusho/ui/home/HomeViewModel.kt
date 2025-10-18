package com.yuhproducts.skusho.ui.home

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yuhproducts.skusho.ads.RewardedAdManager
import com.yuhproducts.skusho.domain.repository.SettingsRepository
import com.yuhproducts.skusho.service.CaptureService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val rewardedAdManager: RewardedAdManager,
    @Suppress("unused")
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var rewardUnlockMonitorJob: Job? = null
    
    init {
        observeRewardUnlockExpiry()
        loadRewardedAd()
        checkServiceStatus()
    }
    
    private fun observeRewardUnlockExpiry() {
        viewModelScope.launch {
            settingsRepository.getCaptureUnlockExpiryMillis().collect { expiryMillis ->
                val now = System.currentTimeMillis()
                if (expiryMillis > 0L && expiryMillis <= now) {
                    handleRewardUnlockExpired()
                } else {
                    val remaining = (expiryMillis - now).coerceAtLeast(0L)
                    _uiState.update {
                        it.copy(
                            rewardUnlockExpiryMillis = expiryMillis,
                            isRewardUnlockActive = expiryMillis > now,
                            rewardUnlockRemainingMillis = remaining
                        )
                    }
                    if (expiryMillis > now) {
                        startRewardUnlockMonitor(expiryMillis)
                    } else {
                        rewardUnlockMonitorJob?.cancel()
                        rewardUnlockMonitorJob = null
                    }
                }
            }
        }
    }
    
    private fun loadRewardedAd() {
        _uiState.update { it.copy(isRewardAdLoading = true) }
        rewardedAdManager.loadAd(
            onLoaded = {
                _uiState.update {
                    it.copy(
                        isRewardAdReady = true,
                        isRewardAdLoading = false
                    )
                }
            },
            onFailedToLoad = {
                _uiState.update {
                    it.copy(
                        isRewardAdReady = false,
                        isRewardAdLoading = false
                    )
                }
            }
        )
    }
    
    fun checkServiceStatus() {
        _uiState.value = _uiState.value.copy(
            isServiceRunning = CaptureService.isRunning
        )
    }
    
    fun onStartCaptureClick() {
        // MediaProjectionの同意を求める
        _uiState.value = _uiState.value.copy(
            shouldRequestMediaProjection = true
        )
    }
    
    fun onMediaProjectionRequested() {
        _uiState.value = _uiState.value.copy(
            shouldRequestMediaProjection = false
        )
    }
    
    fun onStopCaptureClick() {
        stopCaptureService()
    }
    
    fun getMediaProjectionIntent(): Intent {
        val context = getApplication<Application>()
        val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
        return projectionManager.createScreenCaptureIntent()
    }
    
    fun onWatchAdClick(
        activity: Activity,
        onAdUnavailable: (() -> Unit)? = null
    ) {
        if (!_uiState.value.isRewardAdReady) {
            onAdUnavailable?.invoke()
            loadRewardedAd()
            return
        }
        
        _uiState.update { it.copy(isRewardAdReady = false) }
        rewardedAdManager.showAd(
            activity = activity,
            onUserEarnedReward = {
                val newExpiry = System.currentTimeMillis() + REWARD_UNLOCK_DURATION_MILLIS
                viewModelScope.launch {
                    settingsRepository.setCaptureUnlockExpiryMillis(newExpiry)
                }
            },
            onDismissed = {
                loadRewardedAd()
            },
            onFailedToShow = {
                _uiState.update { it.copy(isRewardAdReady = false) }
                loadRewardedAd()
                onAdUnavailable?.invoke()
            }
        )
    }
    
    fun refreshRewardUnlockState() {
        // 位置づけ：UI リカバリ用。最新のキャッシュ値でステートを再計算。
        val expiry = _uiState.value.rewardUnlockExpiryMillis
        val isActive = expiry > System.currentTimeMillis()
        _uiState.update {
            it.copy(isRewardUnlockActive = isActive)
        }
    }
    
    private fun stopCaptureService() {
        _uiState.update { it.copy(isServiceRunning = false) }
        val context = getApplication<Application>()
        val intent = Intent(context, CaptureService::class.java).apply {
            action = CaptureService.ACTION_STOP
        }
        context.startService(intent)
        
        viewModelScope.launch {
            delay(100)
            checkServiceStatus()
        }
    }
    
    private fun startRewardUnlockMonitor(expiryMillis: Long) {
        rewardUnlockMonitorJob?.cancel()
        rewardUnlockMonitorJob = viewModelScope.launch {
            while (true) {
                val remaining = expiryMillis - System.currentTimeMillis()
                if (remaining <= 0L) {
                    handleRewardUnlockExpired()
                    break
                }
                _uiState.update {
                    it.copy(
                        rewardUnlockRemainingMillis = remaining
                    )
                }
                delay(REWARD_UNLOCK_MONITOR_INTERVAL_MILLIS)
            }
        }
    }
    
    private fun handleRewardUnlockExpired() {
        rewardUnlockMonitorJob?.cancel()
        rewardUnlockMonitorJob = null
        val hadExpiry = _uiState.value.rewardUnlockExpiryMillis != 0L
        
        if (_uiState.value.isRewardUnlockActive) {
            if (_uiState.value.isServiceRunning || CaptureService.isRunning) {
                stopCaptureService()
            }
        }
        
        _uiState.update {
            it.copy(
                rewardUnlockExpiryMillis = 0L,
                isRewardUnlockActive = false,
                rewardUnlockRemainingMillis = 0L
            )
        }
        
        if (hadExpiry) {
            viewModelScope.launch {
                settingsRepository.setCaptureUnlockExpiryMillis(0L)
            }
        }
    }
    
    companion object {
        private const val REWARD_UNLOCK_DURATION_MILLIS = 5 * 60 * 1000L
        private const val REWARD_UNLOCK_MONITOR_INTERVAL_MILLIS = 1000L
    }
}

data class HomeUiState(
    val isServiceRunning: Boolean = false,
    val shouldRequestMediaProjection: Boolean = false,
    val rewardUnlockExpiryMillis: Long = 0L,
    val isRewardUnlockActive: Boolean = false,
    val rewardUnlockRemainingMillis: Long = 0L,
    val isRewardAdReady: Boolean = false,
    val isRewardAdLoading: Boolean = false
)
