package com.example.findcircle.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory())
) {
    val state by viewModel.state.collectAsState()

    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editNeighborhood by remember { mutableStateOf("") }
    var initialLoadDone by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSearching by remember { mutableStateOf(false) }
    var locationSuggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    fun fetchSuggestions(query: String) {
        if (query.isBlank()) {
            locationSuggestions = emptyList()
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
                locationSuggestions = response.autocompletePredictions
            } catch (e: Exception) {
                locationSuggestions = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadProfilePicture(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Edit Profile", style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.updateFullProfile(editName, editBio, editPhone, editNeighborhood)
                            onNavigateBack()
                        }
                    ) {
                        Text(
                            "Save",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val currentState = state) {
            is ProfileState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ProfileState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error loading profile", color = MaterialTheme.colorScheme.error)
                }
            }
            is ProfileState.Success -> {
                val user = currentState.user
                if (user != null) {
                    if (!initialLoadDone) {
                        editName = user.name
                        editBio = user.bio
                        editPhone = user.phone
                        editNeighborhood = user.neighborhood
                        initialLoadDone = true
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Avatar ──────────────────────
                        Box(modifier = Modifier.size(120.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    .clip(CircleShape)
                                    .clickable { galleryLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (user.profileImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = user.profileImageUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile Placeholder",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }

                            // Camera Badge
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .size(34.dp),
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.CameraAlt,
                                        contentDescription = "Change Photo",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // ── Form Fields ─────────────────
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Badge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Bio") },
                            placeholder = { Text("Tell us about yourself") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // ── Neighborhood ────────────────
                        var expandedNeighborhood by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expandedNeighborhood && locationSuggestions.isNotEmpty(),
                            onExpandedChange = { expandedNeighborhood = !expandedNeighborhood }
                        ) {
                            OutlinedTextField(
                                value = editNeighborhood,
                                onValueChange = {
                                    editNeighborhood = it
                                    fetchSuggestions(it)
                                    expandedNeighborhood = true
                                },
                                label = { Text("Neighborhood / City") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                trailingIcon = {
                                    if (isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else if (editNeighborhood.isNotEmpty()) {
                                        IconButton(onClick = {
                                            locationSuggestions = emptyList()
                                            editNeighborhood = ""
                                            expandedNeighborhood = false
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )

                            ExposedDropdownMenu(
                                expanded = expandedNeighborhood && locationSuggestions.isNotEmpty(),
                                onDismissRequest = { expandedNeighborhood = false }
                            ) {
                                locationSuggestions.forEach { prediction ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = prediction.getFullText(null).toString(),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        onClick = {
                                            editNeighborhood = prediction.getPrimaryText(null).toString()
                                            locationSuggestions = emptyList()
                                            expandedNeighborhood = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
