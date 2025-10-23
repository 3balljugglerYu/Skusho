package com.yuhproducts.skusho.domain.usecase.settings

import com.yuhproducts.skusho.domain.repository.SettingsRepository
import javax.inject.Inject

class SetContinuousShotIntervalUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(intervalMs: Int) {
        repository.setContinuousShotIntervalMs(intervalMs)
    }
}
