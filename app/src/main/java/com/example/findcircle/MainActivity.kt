package com.example.findcircle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.findcircle.navigation.FindCircleNavGraph
import com.example.findcircle.navigation.Screen
import com.example.findcircle.ui.auth.AuthViewModel
import com.example.findcircle.ui.auth.AuthViewModelFactory
import com.example.findcircle.ui.theme.FindCircleTheme
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    
    // Hold the latest intent so Compose can react to new deep links
    private var currentIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIntent = intent // Capture the intent that launched the app
        
        createNotificationChannel()
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
            val startDestination = if (authViewModel.isUserLoggedIn) Screen.Main.route else Screen.Login.route

            FindCircleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FindCircleNavGraph(
                        startDestination = startDestination,
                        externalIntent = currentIntent
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        currentIntent = intent // Update when tapped while app is already alive
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getString(R.string.default_notification_channel_id)
            val name = "FindCircle Alerts"
            val descriptionText = "Chat messages and match alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}