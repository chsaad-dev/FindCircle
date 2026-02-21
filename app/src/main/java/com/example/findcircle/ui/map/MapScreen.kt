package com.example.findcircle.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.findcircle.ui.home.HomeState
import com.example.findcircle.ui.home.HomeViewModel
import com.example.findcircle.ui.home.HomeViewModelFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
) {
    val state by viewModel.state.collectAsState()
    
    // Default location (San Francisco)
    val defaultLocation = LatLng(37.7749, -122.4194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        if (state is HomeState.Success) {
            val posts = (state as HomeState.Success).posts
            posts.forEach { post ->
                val position = LatLng(post.latitude, post.longitude)
                Marker(
                    state = MarkerState(position = position),
                    title = post.title,
                    snippet = post.ownerName
                )
            }
        }
    }
}
