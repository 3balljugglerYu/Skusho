package com.yuhproducts.skusho.domain.usecase.settings

import com.yuhproducts.skusho.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * オンボーディング完了状態を保存するUseCase
 */
class SetOnboardingCompletedUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(completed: Boolean) {
        repository.setOnboardingCompleted(completed)
    }
}

