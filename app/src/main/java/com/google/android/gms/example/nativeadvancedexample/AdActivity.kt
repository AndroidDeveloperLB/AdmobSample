/*
 * Copyright (C) 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.example.nativeadvancedexample

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.example.nativeadvancedexample.databinding.ActivityAdBinding
import com.google.android.gms.example.nativeadvancedexample.databinding.AdUnifiedBinding
import java.util.Locale

const val ADMOB_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
var currentNativeAd: NativeAd? = null

class AdActivity : AppCompatActivity() {
    lateinit var binding: ActivityAdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdBinding.inflate(layoutInflater).also { setContentView(it.rootView) }
        MobileAds.initialize(this) {}
        binding.refreshButton.setOnClickListener { refreshAd() }
        refreshAd()
    }

    private fun populateUnifiedNativeAdView(
        nativeAd: NativeAd,
        adViewBinding: AdUnifiedBinding
    ) {
        val adView = adViewBinding.root
        adView.mediaView = adViewBinding.adMedia
        adView.headlineView = adViewBinding.adHeadline
        adView.bodyView = adViewBinding.adBody
        adView.callToActionView = adViewBinding.adCallToAction
        adView.iconView = adViewBinding.adAppIcon
        adView.priceView = adViewBinding.adPrice
        adView.starRatingView = adViewBinding.adStars
        adView.storeView = adViewBinding.adStore
        adView.advertiserView = adViewBinding.adAdvertiser
        (adView.headlineView as TextView).text = nativeAd.headline
        adView.mediaView!!.setMediaContent(nativeAd.mediaContent!!)
        if (nativeAd.body == null) {
            adView.bodyView!!.visibility = View.INVISIBLE
        } else {
            adView.bodyView!!.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }
        if (nativeAd.callToAction == null) {
            adView.callToActionView!!.visibility = View.INVISIBLE
        } else {
            adView.callToActionView!!.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }
        if (nativeAd.icon == null) {
            adView.iconView!!.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon!!.drawable
            )
            adView.iconView!!.visibility = View.VISIBLE
        }
        if (nativeAd.price == null) {
            adView.priceView!!.visibility = View.INVISIBLE
        } else {
            adView.priceView!!.visibility = View.VISIBLE
            (adView.priceView as TextView).text = nativeAd.price
        }
        if (nativeAd.store == null) {
            adView.storeView!!.visibility = View.INVISIBLE
        } else {
            adView.storeView!!.visibility = View.VISIBLE
            (adView.storeView as TextView).text = nativeAd.store
        }
        if (nativeAd.starRating == null) {
            adView.starRatingView!!.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView!!.visibility = View.VISIBLE
        }
        if (nativeAd.advertiser == null) {
            adView.advertiserView!!.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as TextView).text = nativeAd.advertiser
            adView.advertiserView!!.visibility = View.VISIBLE
        }
        adView.setNativeAd(nativeAd)
        if (nativeAd.mediaContent!!.hasVideoContent()) {
            binding.videostatusText.text = String.format(
                Locale.getDefault(),
                "Video status: Ad contains a %.2f:1 video asset.",
                nativeAd.mediaContent!!.aspectRatio
            )

            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            nativeAd.mediaContent!!.videoController.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoEnd() {
                    // Publishers should allow native ads to complete video playback before
                    // refreshing or replacing them with another ad in the same UI location.
                    binding.refreshButton.isEnabled = true
                    binding.videostatusText.text = "Video status: Video playback has ended."
                    super.onVideoEnd()
                }
            }
        } else {
            binding.videostatusText.text = "Video status: Ad does not contain a video asset."
            binding.refreshButton.isEnabled = true
        }
    }

    /**
     * Creates a request for a new native ad based on the boolean parameters and calls the
     * corresponding "populate" method when one is successfully returned.
     *
     */
    private fun refreshAd() {
        binding.refreshButton.isEnabled = false

        val builder = AdLoader.Builder(this, ADMOB_AD_UNIT_ID)

        builder.forNativeAd { nativeAd: NativeAd ->
            // OnUnifiedNativeAdLoadedListener implementation.
            // If this callback occurs after the activity is destroyed, you must call
            // destroy and return or you may get a memory leak.
            var activityDestroyed = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                activityDestroyed = isDestroyed
            }
            if (activityDestroyed || isFinishing || isChangingConfigurations) {
                nativeAd.destroy()
                return@forNativeAd
            }
            // You must call destroy on old ads when you are done with them,
            // otherwise you will have a memory leak.
            currentNativeAd?.destroy()
            currentNativeAd = nativeAd
            val adView = AdUnifiedBinding.inflate(layoutInflater, binding.adFrame, false)
            populateUnifiedNativeAdView(nativeAd, adView)
            binding.adFrame.removeAllViews()
            binding.adFrame.addView(adView.root)
        }

        val videoOptions = VideoOptions.Builder()
            .setStartMuted(binding.startMutedCheckbox.isChecked)
            .build()

        val adOptions = com.google.android.gms.ads.nativead.NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .build()

        builder.withNativeAdOptions(adOptions)

        val adLoader = builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                binding.refreshButton.isEnabled = true
                Toast.makeText(
                    this@AdActivity, "Failed to load native ad with error $error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }).build()

        adLoader.loadAd(AdRequest.Builder().build())

        binding.videostatusText.text = ""
    }

    override fun onDestroy() {
        currentNativeAd?.destroy()
        super.onDestroy()
    }
}
