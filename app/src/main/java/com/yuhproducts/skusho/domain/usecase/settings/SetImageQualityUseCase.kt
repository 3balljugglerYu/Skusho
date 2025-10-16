package com.yuhproducts.skusho.domain.usecase.settings

import com.yuhproducts.skusho.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * 画像品質を保存するUseCase
 */
class SetImageQualityUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(quality: Int) {
        repository.setImageQuality(quality)
    }
}

