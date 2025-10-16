package com.yuhproducts.skusho.domain.usecase.settings

import com.yuhproducts.skusho.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * 連写枚数を保存するUseCase
 */
class SetContinuousShotCountUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(count: Int) {
        repository.setContinuousShotCount(count)
    }
}

