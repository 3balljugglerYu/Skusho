package com.example.skusho.ui.home

import android.app.Application
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.skusho.data.preferences.AppPreferences
import com.example.skusho.service.CaptureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val appPreferences = AppPreferences(application)
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        checkServiceStatus()
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
        val context = getApplication<Application>()
        val intent = Intent(context, CaptureService::class.java).apply {
            action = CaptureService.ACTION_STOP
        }
        context.startService(intent)
        checkServiceStatus()
    }
    
    fun getMediaProjectionIntent(): Intent {
        val context = getApplication<Application>()
        val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
        return projectionManager.createScreenCaptureIntent()
    }
}

data class HomeUiState(
    val isServiceRunning: Boolean = false,
    val shouldRequestMediaProjection: Boolean = false
)

