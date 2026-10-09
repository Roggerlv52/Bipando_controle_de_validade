package com.rogger.bp

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.rogger.bp.ui.commun.AnalyticsManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.util.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BipandoApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        // Inicialização do Firebase
        FirebaseApp.initializeApp(this)

        // Inicialização do Firebase App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        // Inicialização do Google Mobile Ads SDK em segundo plano
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@BipandoApplication)
        }

        AnalyticsManager.initialize(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }
}
