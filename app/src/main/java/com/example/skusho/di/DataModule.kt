package com.example.skusho.di

import com.example.skusho.data.repository.SettingsRepositoryImpl
import com.example.skusho.data.source.local.AppPreferences
import com.example.skusho.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Data層の依存関係を提供するモジュール
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    
    @Provides
    @Singleton
    fun provideSettingsRepository(
        appPreferences: AppPreferences
    ): SettingsRepository {
        return SettingsRepositoryImpl(appPreferences)
    }
}

