package com.example.skusho.domain.usecase.settings

import com.example.skusho.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * 画像形式を保存するUseCase
 */
class SetImageFormatUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(format: String) {
        repository.setImageFormat(format)
    }
}

