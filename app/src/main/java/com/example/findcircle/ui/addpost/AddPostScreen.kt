package com.example.findcircle.ui.addpost

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.findcircle.domain.model.PostType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    viewModel: AddPostViewModel = viewModel(factory = AddPostViewModelFactory()),
    onPostSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var postType by remember { mutableStateOf(PostType.LOST) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    // For simplicity right now, hardcoding location to a default place
    val latitude = 37.7749
    val longitude = -122.4194

    val state by viewModel.state.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
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
                title = { Text("Create Post") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Post Type Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = postType == PostType.LOST,
                    onClick = { postType = PostType.LOST },
                    label = { Text("Lost Item") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = postType == PostType.FOUND,
                    onClick = { postType = PostType.FOUND },
                    label = { Text("Found Item") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Image Picker Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (e.g., Electronics, Pets)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (state is AddPostState.Error) {
                Text(
                    text = (state as AddPostState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    viewModel.createPost(
                        title = title,
                        description = description,
                        category = category,
                        type = postType,
                        latitude = latitude,
                        longitude = longitude,
                        imageUri = imageUri
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = state !is AddPostState.Loading
            ) {
                if (state is AddPostState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Post", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
