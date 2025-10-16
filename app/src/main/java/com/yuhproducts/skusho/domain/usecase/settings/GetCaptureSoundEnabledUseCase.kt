package com.yuhproducts.skusho.domain.usecase.settings

import com.yuhproducts.skusho.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 撮影音有効状態を取得するUseCase
 */
class GetCaptureSoundEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.getCaptureSoundEnabled()
}

