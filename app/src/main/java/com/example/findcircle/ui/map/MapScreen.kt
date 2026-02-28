package com.example.findcircle.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import android.location.Geocoder
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.findcircle.domain.model.Post
import com.example.findcircle.domain.model.PostType
import com.example.findcircle.domain.model.PostCategories
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.findcircle.ui.home.HomeState
import com.example.findcircle.ui.home.HomeViewModel
import com.example.findcircle.ui.home.HomeViewModelFactory
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.SavedSearch
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.AutocompletePrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
) {
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val defaultLocation = LatLng(37.7749, -122.4194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val latLng = LatLng(location.latitude, location.longitude)
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                            }
                        } else {
                            Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: SecurityException) {
                }
            }
            else -> {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    var filterRadius by remember { mutableStateOf(50f) } // Max 50km
    var filterCategory by remember { mutableStateOf<String?>(null) }
    var filterDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSavingAlert by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    fun distanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun fetchSuggestions(query: String) {
        if (query.isBlank()) {
            suggestions = emptyList()
            return
        }
        isSearching = true
        coroutineScope.launch {
            try {
                val placesClient = Places.createClient(context)
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .build()
                
                val response = withContext(Dispatchers.IO) {
                    placesClient.findAutocompletePredictions(request).await()
                }
                suggestions = response.autocompletePredictions
            } catch (e: Exception) {
                Log.e("MapScreen", "Places API exception: ", e)
                suggestions = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    fun onSuggestionSelected(prediction: AutocompletePrediction) {
        searchQuery = prediction.getPrimaryText(null).toString()
        suggestions = emptyList()
        isSearching = true
        
        coroutineScope.launch {
            try {
                val placesClient = Places.createClient(context)
                val placeFields = listOf(Place.Field.LAT_LNG)
                val fetchRequest = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
                
                val fetchResponse = withContext(Dispatchers.IO) {
                    placesClient.fetchPlace(fetchRequest).await()
                }
                
                val latLng = fetchResponse.place.latLng
                if(latLng != null) {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                }
            } catch (e: Exception) {
                Log.e("MapScreen", "Places API fetch exception: ", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading location", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSearching = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            ),
            onMapLoaded = { isMapLoaded = true },
            onMapClick = { selectedPost = null }
        ) {
            if (state is HomeState.Success) {
                val posts = (state as HomeState.Success).posts
                
                LaunchedEffect(posts) {
                    if (posts.isNotEmpty() && !isMapLoaded) {
                         val firstPost = posts.first()
                         val pos = LatLng(firstPost.latitude, firstPost.longitude)
                         cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 12f))
                    }
                }

                val currentCenter = cameraPositionState.position.target
                val filteredPosts = posts.filter { post ->
                    val dist = distanceInKm(currentCenter.latitude, currentCenter.longitude, post.latitude, post.longitude)
                    val matchesRadius = dist <= filterRadius
                    val matchesCategory = if (filterCategory == null) true else post.category == filterCategory
                    val matchesDate = if (filterDate == null) true else {
                        val postDate = java.util.Date(if (post.dateReported > 0) post.dateReported else post.timestamp)
                        val filterD = java.util.Date(filterDate!!)
                        val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                        sdf.format(postDate) == sdf.format(filterD)
                    }
                    matchesRadius && matchesCategory && matchesDate
                }

                filteredPosts.forEach { post ->
                    val position = LatLng(post.latitude, post.longitude)
                    val isLost = post.type == PostType.LOST
                    val isRecentUrgent = post.isUrgent && (System.currentTimeMillis() - post.timestamp <= 48L * 60L * 60L * 1000L)
                    
                    val markerColor = when {
                        isRecentUrgent -> BitmapDescriptorFactory.HUE_RED
                        isLost -> BitmapDescriptorFactory.HUE_ORANGE
                        else -> BitmapDescriptorFactory.HUE_AZURE
                    }
                    
                    val titlePrefix = if (isRecentUrgent) "🚨 URGENT: " else ""
                    
                    Marker(
                        state = MarkerState(position = position),
                        title = "\$titlePrefix\${post.title}",
                        snippet = post.ownerName,
                        icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                        onClick = {
                            selectedPost = post
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLng(position))
                            }
                            true
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                
                if (hasFineLocation || hasCoarseLocation) {
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                val latLng = LatLng(location.latitude, location.longitude)
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                }
                            } else {
                                Toast.makeText(context, "Turn on Location", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: SecurityException) {
                    }
                } else {
                    locationPermissionRequest.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 84.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Recenter")
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it 
                            fetchSuggestions(it)
                        },
                        placeholder = { Text("Search location...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    suggestions = emptyList() 
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { 
                        })
                    )
                    
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Filters", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                if (suggestions.isNotEmpty()) {
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                    ) {
                        items(suggestions) { prediction ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSuggestionSelected(prediction) }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = prediction.getPrimaryText(null).toString(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = prediction.getSecondaryText(null).toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

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
        
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        
                        Text("Search Radius: ${filterRadius.toInt()} km", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = filterRadius,
                            onValueChange = { filterRadius = it },
                            valueRange = 1f..50f,
                            steps = 49
                        )
                        
                        Text("Category", style = MaterialTheme.typography.labelLarge)
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = filterCategory == null,
                                    onClick = { filterCategory = null },
                                    label = { Text("All") }
                                )
                            }
                            items(PostCategories.ALL) { category ->
                                FilterChip(
                                    selected = filterCategory == category,
                                    onClick = { filterCategory = category },
                                    label = { Text(category) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                        
                        Text("Date", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dateStr = if (filterDate != null) {
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(filterDate!!))
                            } else {
                                "Any time"
                            }
                            OutlinedButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dateStr)
                            }
                            if (filterDate != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { filterDate = null }) { Text("Clear") }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            isSavingAlert = true
                            coroutineScope.launch {
                                try {
                                    val user = ServiceLocator.auth.currentUser
                                    if (user == null) {
                                        Toast.makeText(context, "Must be logged in to save alerts", Toast.LENGTH_SHORT).show()
                                        showFilterSheet = false
                                        return@launch
                                    }

                                    val alert = SavedSearch(
                                        userId = user.uid,
                                        query = searchQuery,
                                        radiusKm = filterRadius,
                                        category = filterCategory,
                                        latitude = cameraPositionState.position.target.latitude,
                                        longitude = cameraPositionState.position.target.longitude
                                    )
                                    val result = ServiceLocator.savedSearchRepository.createSavedSearch(alert)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Search Alert Saved!", Toast.LENGTH_SHORT).show()
                                        showFilterSheet = false
                                    } else {
                                        Toast.makeText(context, "Failed to save alert", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isSavingAlert = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isSavingAlert
                    ) {
                        if (isSavingAlert) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save Search Alert", fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { filterDate = it }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun MapPostPreviewCard(post: Post, onClick: () -> Unit, onDismiss: () -> Unit) {
    val isLost = post.type == PostType.LOST
    val badgeContainerColor = if (isLost) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val badgeOnContainerColor = if (isLost) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

    val isRecentUrgent = post.isUrgent && (System.currentTimeMillis() - post.timestamp <= 48L * 60L * 60L * 1000L)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRecentUrgent) 12.dp else 8.dp),
        border = if (isRecentUrgent) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
