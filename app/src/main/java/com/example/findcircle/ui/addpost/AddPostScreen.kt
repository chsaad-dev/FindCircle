package com.example.findcircle.ui.addpost

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.findcircle.domain.model.PostType
import com.example.findcircle.domain.model.PostCategories
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.input.ImeAction
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    viewModel: AddPostViewModel = viewModel(factory = AddPostViewModelFactory()),
    onPostSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PostCategories.ALL.first()) }
    var expandedCategory by remember { mutableStateOf(false) }
    var postType by remember { mutableStateOf(PostType.LOST) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var dateReported by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isUrgent by remember { mutableStateOf(false) }
    var secretQuestion by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateReported)
    
    var showMapPicker by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 12f)
    }

    val state by viewModel.state.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        uri?.let { viewModel.analyzeImage(context, it) }
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
                Log.e("AddPostScreen", "Places API exception: ", e)
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
                Log.e("AddPostScreen", "Places API fetch exception: ", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading location", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(state) {
        if (state is AddPostState.Success) {
            viewModel.resetState()
            onPostSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Post", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { padding ->
        if (showMapPicker) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                )
                
                // Center pin
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Pin",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .offset(y = (-24).dp)
                )

                // Search Bar Overlay
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
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
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )
                        
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
                
                // Bottom actions
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = { showMapPicker = false }) {
                            Text("Cancel")
                        }
                        Button(onClick = { 
                            selectedLocation = cameraPositionState.position.target
                            showMapPicker = false 
                        }) {
                            Text("Select Location")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Post Type Toggle
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val lostSelected = postType == PostType.LOST
                    Button(
                        onClick = { postType = PostType.LOST },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lostSelected) MaterialTheme.colorScheme.error else Color.Transparent,
                            contentColor = if (lostSelected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        elevation = if (lostSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Lost", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { postType = PostType.FOUND },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!lostSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (!lostSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        elevation = if (!lostSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Found", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Image Picker Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Overlay edit button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .clickable { galleryLauncher.launch("image/*") }
                            .padding(8.dp)
                    ) {
                        Text("Change Photo", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Image",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap to add photo",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            AnimatedVisibility(visible = tags.isNotEmpty()) {
                Column {
                    Text("Auto-detected Tags (from image)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags.size) { index ->
                            val tag = tags[index]
                            InputChip(
                                selected = false,
                                onClick = { viewModel.removeTag(tag) },
                                label = { Text(tag) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove Tag", modifier = Modifier.size(16.dp)) },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("E.g., Blue Backpack, Golden Retriever") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Provide details about the item...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = MaterialTheme.shapes.medium
            )
            
            if (postType == PostType.FOUND) {
                OutlinedTextField(
                    value = secretQuestion,
                    onValueChange = { secretQuestion = it },
                    label = { Text("Secret Question (Optional)") },
                    placeholder = { Text("E.g., What is the lock screen wallpaper?") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = MaterialTheme.shapes.medium
                )
                Text(
                    text = "Add a secret question that the claimant must answer to verify ownership.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
                )
            }
            
            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = MaterialTheme.shapes.medium
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    PostCategories.ALL.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                category = selectionOption
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            // Date Picker Selection
            val dateString = remember(dateReported) {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dateReported))
            }
            
            Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                OutlinedTextField(
                    value = dateString,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date " + if (postType == PostType.LOST) "Lost" else "Found") },
                    trailingIcon = { 
                        Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Select Date")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = MaterialTheme.shapes.medium
                )
            }
            
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { dateReported = it }
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
            
            // Urgent Toggle / Amber Alert
            Card(
                modifier = Modifier.fillMaxWidth().clickable { isUrgent = !isUrgent },
                colors = CardDefaults.cardColors(containerColor = if (isUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Amber Alert", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
                        Text("Post will be pinned to top the feed for 48 hours", style = MaterialTheme.typography.bodySmall, color = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isUrgent,
                        onCheckedChange = { isUrgent = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.error, checkedTrackColor = MaterialTheme.colorScheme.errorContainer)
                    )
                }
            }
            
            // Location Preview Card
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showMapPicker = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (selectedLocation != null) {
                            Text("Lat: ${"%.4f".format(selectedLocation!!.latitude)} • Lng: ${"%.4f".format(selectedLocation!!.longitude)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("Tap to drop a pin on the map", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state is AddPostState.Error) {
                val errorMsg = (state as? AddPostState.Error)?.message ?: ""
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    viewModel.createPost(
                        context = context,
                        title = title,
                        description = description,
                        category = category,
                        type = postType,
                        latitude = selectedLocation?.latitude ?: 37.7749,
                        longitude = selectedLocation?.longitude ?: -122.4194,
                        imageUri = imageUri,
                        dateReported = dateReported,
                        tags = tags,
                        isUrgent = isUrgent,
                        secretQuestion = secretQuestion
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = state !is AddPostState.Loading && title.isNotBlank() && description.isNotBlank()
            ) {
                if (state is AddPostState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Post", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        }
    }
}
