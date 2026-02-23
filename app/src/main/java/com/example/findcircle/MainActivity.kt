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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
            val startDestination = if (authViewModel.isUserLoggedIn) Screen.Main.route else Screen.Login.route

            FindCircleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FindCircleNavGraph(startDestination = startDestination)
                }
            }
        }
    }
}