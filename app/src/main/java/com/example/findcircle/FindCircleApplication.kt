package com.example.findcircle

import android.app.Application

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.findcircle.worker.MatchWorker
import java.util.concurrent.TimeUnit

import com.google.android.libraries.places.api.Places

class FindCircleApplication : Application() {
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
}
