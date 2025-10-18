package com.yuhproducts.skusho.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handles loading and displaying rewarded ads via AdMob.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("rewardAdUnitId") private val adUnitId: String
) {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun loadAd(
        onLoaded: (() -> Unit)? = null,
        onFailedToLoad: ((LoadAdError) -> Unit)? = null
    ) {
        if (rewardedAd != null || isLoading) {
            onLoaded?.invoke()
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    onFailedToLoad?.invoke(adError)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    onLoaded?.invoke()
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        onUserEarnedReward: (RewardItem) -> Unit,
        onDismissed: (() -> Unit)? = null,
        onFailedToShow: (() -> Unit)? = null
    ) {
        val ad = rewardedAd ?: run {
            onFailedToShow?.invoke()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onDismissed?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                rewardedAd = null
                onFailedToShow?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                rewardedAd = null
            }
        }

        ad.show(activity) { rewardItem ->
            onUserEarnedReward(rewardItem)
        }
    }

    fun hasLoadedAd(): Boolean = rewardedAd != null
}
