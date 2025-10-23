package com.yuhproducts.skusho.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuhproducts.skusho.domain.usecase.settings.GetCaptureSoundEnabledUseCase
import com.yuhproducts.skusho.domain.usecase.settings.GetContinuousShotCountUseCase
import com.yuhproducts.skusho.domain.usecase.settings.GetContinuousShotIntervalUseCase
import com.yuhproducts.skusho.domain.usecase.settings.GetImageFormatUseCase
import com.yuhproducts.skusho.domain.usecase.settings.GetImageQualityUseCase
import com.yuhproducts.skusho.domain.usecase.settings.SetCaptureSoundEnabledUseCase
import com.yuhproducts.skusho.domain.usecase.settings.SetContinuousShotCountUseCase
import com.yuhproducts.skusho.domain.usecase.settings.SetContinuousShotIntervalUseCase
import com.yuhproducts.skusho.domain.usecase.settings.SetImageFormatUseCase
import com.yuhproducts.skusho.domain.usecase.settings.SetImageQualityUseCase
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
    private val setContinuousShotCountUseCase: SetContinuousShotCountUseCase,
    private val getContinuousShotIntervalUseCase: GetContinuousShotIntervalUseCase,
    private val setContinuousShotIntervalUseCase: SetContinuousShotIntervalUseCase
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
        viewModelScope.launch {
            getContinuousShotIntervalUseCase().collect { intervalMs ->
                _uiState.value = _uiState.value.copy(continuousShotIntervalMs = intervalMs)
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
    
    fun onContinuousShotIntervalChanged(intervalMs: Int) {
        viewModelScope.launch {
            setContinuousShotIntervalUseCase(intervalMs)
        }
    }
}

data class SettingsUiState(
    val imageFormat: String = "PNG",
    val imageQuality: Int = 100,
    val captureSoundEnabled: Boolean = false,
    val continuousShotCount: Int = 0,
    val continuousShotIntervalMs: Int = 500
)
