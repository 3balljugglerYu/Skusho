package com.yuhproducts.skusho.di

import android.content.Context
import com.yuhproducts.skusho.data.source.local.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * アプリケーション全体で使用する依存関係を提供するモジュール
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppPreferences(
        @ApplicationContext context: Context
    ): AppPreferences {
        return AppPreferences(context)
    }
}

