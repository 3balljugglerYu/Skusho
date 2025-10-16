package com.yuhproducts.skusho.domain.usecase.settings

import com.yuhproducts.skusho.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * 撮影音有効状態を保存するUseCase
 */
class SetCaptureSoundEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setCaptureSoundEnabled(enabled)
    }
}

