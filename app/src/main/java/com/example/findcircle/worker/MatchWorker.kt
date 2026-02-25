package com.example.findcircle.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.Post
import com.example.findcircle.domain.model.PostType
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.findcircle.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.findcircle.domain.model.SavedSearch
import kotlin.math.*

class MatchWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sharedPrefs = applicationContext.getSharedPreferences("FindCirclePrefs", Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)

        if (!notificationsEnabled) {
            Log.d("MatchWorker", "Notifications are disabled in Settings. Skipping match alerts.")
            return Result.success()
        }

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
                            showMatchNotification(lost, found)
                        }
                    }
                }

                // Check for Saved Searches
                val searchesResult = ServiceLocator.savedSearchRepository.getAllSavedSearches()
                if (searchesResult.isSuccess) {
                    val savedSearches = searchesResult.getOrNull() ?: emptyList()
                    val currentUser = ServiceLocator.auth.currentUser
                    
                    if (currentUser != null) {
                        // Only alert on searches owned by the current user
                        val mySearches = savedSearches.filter { it.userId == currentUser.uid }
                        for (search in mySearches) {
                            for (post in posts) {
                                // Don't alert on my own posts
                                if (post.ownerId == currentUser.uid) continue

                                val distance = calculateDistanceInKm(
                                    lat1 = search.latitude, lon1 = search.longitude,
                                    lat2 = post.latitude, lon2 = post.longitude
                                )
                                
                                val matchesQuery = post.title.contains(search.query, ignoreCase = true) || 
                                                   post.description.contains(search.query, ignoreCase = true) ||
                                                   post.tags.any { it.contains(search.query, ignoreCase = true) }
                                
                                val matchesCategory = if (search.category == null) true else post.category == search.category
                                
                                if (distance <= search.radiusKm && matchesQuery && matchesCategory) {
                                    showSavedSearchNotification(post, search)
                                }
                            }
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

    private fun showMatchNotification(lost: Post, found: Post) {
        val context = applicationContext
        val channelId = "match_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Match Alerts"
            val descriptionText = "Notifications for potential item matches"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            // Use a standard icon fallback if R.drawable.ic_launcher_foreground isn't standard
            .setSmallIcon(android.R.drawable.ic_dialog_alert) 
            .setContentTitle("Potential Match Found!")
            .setContentText("A found '${found.title}' is near your lost '${lost.title}'.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).notify(lost.id.hashCode(), notificationBuilder.build())
        } else {
            Log.w("MatchWorker", "No POST_NOTIFICATIONS permission to show match.")
        }
    }
    
    private fun showSavedSearchNotification(post: Post, search: SavedSearch) {
        val context = applicationContext
        val channelId = "match_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Match Alerts"
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle("Search Alert Triggered!")
            .setContentText("A new post matches your alert for '${search.query}': ${post.title}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).notify(post.id.hashCode() + search.id.hashCode(), notificationBuilder.build())
        } else {
            Log.w("MatchWorker", "No POST_NOTIFICATIONS permission to show saved search alert.")
        }
    }
}
