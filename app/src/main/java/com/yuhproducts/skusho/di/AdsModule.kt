package com.yuhproducts.skusho.di

import android.content.Context
import com.yuhproducts.skusho.R
import com.yuhproducts.skusho.ads.RewardedAdManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdsModule {

    @Provides
    @Singleton
    fun provideRewardedAdManager(
        @ApplicationContext context: Context,
        @Named("rewardAdUnitId") adUnitId: String
    ): RewardedAdManager {
        return RewardedAdManager(context, adUnitId)
    }

    @Provides
    @Named("rewardAdUnitId")
    fun provideRewardAdUnitId(
        @ApplicationContext context: Context
    ): String = context.getString(R.string.admob_reward_ad_unit_id)
}
