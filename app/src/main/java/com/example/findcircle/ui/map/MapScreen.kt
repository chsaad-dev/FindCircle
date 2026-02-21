package com.example.findcircle.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.findcircle.domain.model.Post
import com.example.findcircle.domain.model.PostType
import com.example.findcircle.ui.home.HomeState
import com.example.findcircle.ui.home.HomeViewModel
import com.example.findcircle.ui.home.HomeViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
) {
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // Default fallback location
    val defaultLocation = LatLng(37.7749, -122.4194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            ),
            onMapLoaded = { isMapLoaded = true },
            onMapClick = { selectedPost = null } // Dismiss bottom card on map click
        ) {
            if (state is HomeState.Success) {
                val posts = (state as HomeState.Success).posts
                
                // Recenter map on first load if we have posts
                LaunchedEffect(posts) {
                    if (posts.isNotEmpty() && !isMapLoaded) {
                         val firstPost = posts.first()
                         val pos = LatLng(firstPost.latitude, firstPost.longitude)
                         cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 12f))
                    }
                }

                posts.forEach { post ->
                    val position = LatLng(post.latitude, post.longitude)
                    val isLost = post.type == PostType.LOST
                    val markerColor = if (isLost) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_BLUE
                    
                    Marker(
                        state = MarkerState(position = position),
                        title = post.title,
                        snippet = post.ownerName,
                        icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                        onClick = {
                            selectedPost = post
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLng(position))
                            }
                            true // Return true to consume the click and prevent default info window
                        }
                    )
                }
            }
        }

        // Floating Action Button to center on roughly the average location or default
        FloatingActionButton(
            onClick = {
                if (state is HomeState.Success) {
                    val posts = (state as HomeState.Success).posts
                    if (posts.isNotEmpty()) {
                         val firstPost = posts.first()
                         val pos = LatLng(firstPost.latitude, firstPost.longitude)
                         coroutineScope.launch {
                             cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 12f))
                         }
                    } else {
                         coroutineScope.launch {
                             cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
                         }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Recenter")
        }

        // Bottom Details Card
        AnimatedVisibility(
            visible = selectedPost != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedPost?.let { post ->
                MapPostPreviewCard(
                    post = post,
                    onClick = { /* TODO: Navigate to Detail Screen */ },
                    onDismiss = { selectedPost = null }
                )
            }
        }
    }
}

@Composable
fun MapPostPreviewCard(post: Post, onClick: () -> Unit, onDismiss: () -> Unit) {
    val isLost = post.type == PostType.LOST
    val badgeContainerColor = if (isLost) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val badgeOnContainerColor = if (isLost) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 80.dp) // Padding for bottom nav bar
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tiny Image Thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (post.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Post Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Surface(
                        shape = CircleShape,
                        color = badgeContainerColor,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = post.type.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeOnContainerColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "By ${post.ownerName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
