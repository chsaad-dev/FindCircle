package com.example.findcircle

import android.app.Application

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.findcircle.worker.MatchWorker
import java.util.concurrent.TimeUnit

class FindCircleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Schedule MatchWorker to run every 15 minutes (minimum interval for PeriodicWork)
        val matchWorkRequest = PeriodicWorkRequestBuilder<MatchWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(matchWorkRequest)
    }
}
