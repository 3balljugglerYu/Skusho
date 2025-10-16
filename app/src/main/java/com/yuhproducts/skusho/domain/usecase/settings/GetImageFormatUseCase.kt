package com.yuhproducts.skusho.domain.usecase.settings

import com.yuhproducts.skusho.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 画像形式を取得するUseCase
 */
class GetImageFormatUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<String> = repository.getImageFormat()
}

