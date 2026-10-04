package com.emckeon97.gifscroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.emckeon97.gifscroll.data.AppContainer
import com.emckeon97.gifscroll.ui.MainScreen
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val context = LocalContext.current
                val container = remember { AppContainer(context) }
                // Refresh auth token on launch — Supabase tokens expire after 1 hour.
                LaunchedEffect(Unit) {
                    try { container.authManager.refreshSession() } catch (_: Exception) {}
                }
                MainScreen(container)
            }
        }
    }
}
