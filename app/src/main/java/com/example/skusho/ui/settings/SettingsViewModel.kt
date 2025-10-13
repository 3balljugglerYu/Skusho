package com.example.skusho.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skusho.domain.usecase.settings.GetCaptureSoundEnabledUseCase
import com.example.skusho.domain.usecase.settings.GetContinuousShotCountUseCase
import com.example.skusho.domain.usecase.settings.GetImageFormatUseCase
import com.example.skusho.domain.usecase.settings.GetImageQualityUseCase
import com.example.skusho.domain.usecase.settings.SetCaptureSoundEnabledUseCase
import com.example.skusho.domain.usecase.settings.SetContinuousShotCountUseCase
import com.example.skusho.domain.usecase.settings.SetImageFormatUseCase
import com.example.skusho.domain.usecase.settings.SetImageQualityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getImageFormatUseCase: GetImageFormatUseCase,
    private val setImageFormatUseCase: SetImageFormatUseCase,
    private val getImageQualityUseCase: GetImageQualityUseCase,
    private val setImageQualityUseCase: SetImageQualityUseCase,
    private val getCaptureSoundEnabledUseCase: GetCaptureSoundEnabledUseCase,
    private val setCaptureSoundEnabledUseCase: SetCaptureSoundEnabledUseCase,
    private val getContinuousShotCountUseCase: GetContinuousShotCountUseCase,
    private val setContinuousShotCountUseCase: SetContinuousShotCountUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            getImageFormatUseCase().collect { format ->
                _uiState.value = _uiState.value.copy(imageFormat = format)
            }
        }
        viewModelScope.launch {
            getImageQualityUseCase().collect { quality ->
                _uiState.value = _uiState.value.copy(imageQuality = quality)
            }
        }
        viewModelScope.launch {
            getCaptureSoundEnabledUseCase().collect { enabled ->
                _uiState.value = _uiState.value.copy(captureSoundEnabled = enabled)
            }
        }
        viewModelScope.launch {
            getContinuousShotCountUseCase().collect { count ->
                _uiState.value = _uiState.value.copy(continuousShotCount = count)
            }
        }
    }
    
    fun onImageFormatChanged(format: String) {
        viewModelScope.launch {
            setImageFormatUseCase(format)
        }
    }
    
    fun onImageQualityChanged(quality: Int) {
        viewModelScope.launch {
            setImageQualityUseCase(quality)
        }
    }
    
    fun onCaptureSoundChanged(enabled: Boolean) {
        viewModelScope.launch {
            setCaptureSoundEnabledUseCase(enabled)
        }
    }
    
    fun onContinuousShotCountChanged(count: Int) {
        viewModelScope.launch {
            setContinuousShotCountUseCase(count)
        }
    }
}

data class SettingsUiState(
    val imageFormat: String = "PNG",
    val imageQuality: Int = 100,
    val captureSoundEnabled: Boolean = false,
    val continuousShotCount: Int = 0
)
