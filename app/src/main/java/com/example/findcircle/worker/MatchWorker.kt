package com.example.findcircle.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.Post
import com.example.findcircle.domain.model.PostType
import kotlin.math.*

class MatchWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val postRepository = ServiceLocator.postRepository
            val recentPostsResult = postRepository.getRecentPosts(100)
            
            if (recentPostsResult.isSuccess) {
                val posts = recentPostsResult.getOrNull() ?: emptyList()
                
                val lostPosts = posts.filter { it.type == PostType.LOST }
                val foundPosts = posts.filter { it.type == PostType.FOUND }

                // Check for overlapping matches
                for (lost in lostPosts) {
                    for (found in foundPosts) {
                        val distance = calculateDistanceInKm(
                            lat1 = lost.latitude, lon1 = lost.longitude,
                            lat2 = found.latitude, lon2 = found.longitude
                        )
                        
                        // If within 5 km
                        if (distance <= 5.0) {
                            Log.d("MatchWorker", "Match found! Lost item: ${lost.title}, Found item: ${found.title}, Distance: $distance km")
                            // In a full implementation, we would send a local notification or trigger an FCM push message
                        }
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("MatchWorker", "Error calculating matches", e)
            Result.failure()
        }
    }

    private fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Earth radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
                
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
