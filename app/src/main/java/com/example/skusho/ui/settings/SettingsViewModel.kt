package com.example.skusho.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.skusho.data.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val appPreferences = AppPreferences(application)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            appPreferences.imageFormat.collect { format ->
                _uiState.value = _uiState.value.copy(imageFormat = format)
            }
        }
        viewModelScope.launch {
            appPreferences.imageQuality.collect { quality ->
                _uiState.value = _uiState.value.copy(imageQuality = quality)
            }
        }
        viewModelScope.launch {
            appPreferences.captureSoundEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(captureSoundEnabled = enabled)
            }
        }
        viewModelScope.launch {
            appPreferences.continuousShotCount.collect { count ->
                _uiState.value = _uiState.value.copy(continuousShotCount = count)
            }
        }
    }
    
    fun onImageFormatChanged(format: String) {
        viewModelScope.launch {
            appPreferences.setImageFormat(format)
        }
    }
    
    fun onImageQualityChanged(quality: Int) {
        viewModelScope.launch {
            appPreferences.setImageQuality(quality)
        }
    }
    
    fun onCaptureSoundChanged(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setCaptureSoundEnabled(enabled)
        }
    }
    
    fun onContinuousShotCountChanged(count: Int) {
        viewModelScope.launch {
            appPreferences.setContinuousShotCount(count)
        }
    }
}

data class SettingsUiState(
    val imageFormat: String = "PNG",
    val imageQuality: Int = 100,
    val captureSoundEnabled: Boolean = false,
    val continuousShotCount: Int = 0
)

