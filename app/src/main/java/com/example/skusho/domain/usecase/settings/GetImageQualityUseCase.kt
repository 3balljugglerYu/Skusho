package com.example.skusho.domain.usecase.settings

import com.example.skusho.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 画像品質を取得するUseCase
 */
class GetImageQualityUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Int> = repository.getImageQuality()
}

