package com.example.findcircle.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.findcircle.MainActivity
import com.example.findcircle.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FindCircleMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // If the device's FCM token rotates, save the new one to Firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId)
                .update("fcmToken", token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Parse notification payload if it exists
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "FindCircle Alert"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "You have a new message."
        val type = remoteMessage.data["type"] // e.g., "CHAT", "AMBER_ALERT", "MATCH"

        sendNotification(title, body, type, remoteMessage.data)
    }

    private fun sendNotification(title: String, messageBody: String, type: String?, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // Pass the type and target IDs to navigation
            putExtra("notification_type", type)
            if (type == "CHAT" && data.containsKey("chatId") && data.containsKey("otherUserName")) {
                putExtra("chatId", data["chatId"])
                putExtra("otherUserName", data["otherUserName"])
            } else if ((type == "AMBER_ALERT" || type == "MATCH") && data.containsKey("postId")) {
                putExtra("postId", data["postId"])
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo) // Using existing logo
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FindCircle Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat messages and match alerts"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Use a unique ID for each notification so they don't overwrite each other easily
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
