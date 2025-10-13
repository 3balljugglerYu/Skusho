package com.example.skusho.domain.usecase.settings

import com.example.skusho.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * オンボーディング完了状態を取得するUseCase
 */
class GetOnboardingCompletedUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.getOnboardingCompleted()
}

