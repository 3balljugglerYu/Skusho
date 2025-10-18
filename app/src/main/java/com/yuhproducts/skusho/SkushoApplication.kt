package com.yuhproducts.skusho

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.HiltAndroidApp

/**
 * Hiltを使用するためのApplicationクラス
 */
@HiltAndroidApp
class SkushoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf(TEST_DEVICE_ID))
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)

        // Initialize Mobile Ads SDK once at app startup
        MobileAds.initialize(this)
    }

    private companion object {
        const val TEST_DEVICE_ID = "f1aafff0-b1c2-44c7-9fd3-10ca1ad1abe1"
    }
}
