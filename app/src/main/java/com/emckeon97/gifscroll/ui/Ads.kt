package com.emckeon97.gifscroll.ui

import android.content.Context
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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * AdMob config. These are Google's TEST ids — they always serve test ads.
 * When you're ready to earn: create an app + ad units at apps.admob.com and
 * swap the values here (and the app id in app/build.gradle's manifestPlaceholders).
 */
object Ads {
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val AD_EVERY_N_ITEMS = 6
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
                setAdSize(AdSize.getAnchoredAdaptiveBannerAdSize(ctx, adWidth))
                adUnitId = Ads.BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() }
    )
}
