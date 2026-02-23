package com.example.findcircle

import android.app.Application

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.findcircle.worker.MatchWorker
import java.util.concurrent.TimeUnit

import com.google.android.libraries.places.api.Places

class FindCircleApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize the Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
        
        // Schedule MatchWorker to run every 15 minutes (minimum interval for PeriodicWork)
        val matchWorkRequest = PeriodicWorkRequestBuilder<MatchWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(matchWorkRequest)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }
}
