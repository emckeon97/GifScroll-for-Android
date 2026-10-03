package com.emckeon97.gifscroll.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * AdMob config — live IDs. Change AD_EVERY_N_ITEMS to adjust ad frequency.
 */
object Ads {
    const val BANNER_AD_UNIT_ID = "ca-app-pub-8263714518098380/5630499497"
    /** Ad slots follow this repeating pattern: 5 memes, ad, 10 memes, ad, ... */
    val AD_PATTERN = listOf(5, 10)
}

/** Full-screen ad page slotted into the vertical feed pager. */
@Composable
fun AdPage() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Advertisement", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        BannerAd()
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx: Context ->
            AdView(ctx).apply {
                val metrics = ctx.resources.displayMetrics
                val adWidth = (metrics.widthPixels / metrics.density).toInt()
                @Suppress("DEPRECATION")
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth))
                adUnitId = Ads.BANNER_AD_UNIT_ID
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("GifScrollAds", "Banner failed: ${error.code} ${error.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() }
    )
}
