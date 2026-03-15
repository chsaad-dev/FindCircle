package com.example.findcircle.ui.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit = {},
    viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = ProfileViewModelFactory())
) {
    val context = LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("FindCirclePrefs", Context.MODE_PRIVATE)
    }

    var notificationsEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("notifications_enabled", true))
    }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }
    
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var pendingAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    val authActionState by viewModel.authActionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Preferences ─────────────────────
            SettingsSection(title = "Preferences") {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Push Notifications",
                    subtitle = "Receive alerts for matches and saved searches.",
                    checked = notificationsEnabled,
                    onCheckedChange = { checked ->
                        notificationsEnabled = checked
                        sharedPrefs.edit().putBoolean("notifications_enabled", checked).apply()
                    }
                )
            }

            // ── Support & Info ──────────────────
            SettingsSection(title = "Support & Information") {
                SettingsActionItem(
                    icon = Icons.Outlined.Info,
                    title = "About FindCircle",
                    onClick = { showAboutDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
                SettingsActionItem(
                    icon = Icons.Outlined.Email,
                    title = "Contact Support",
                    onClick = { showContactDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
                SettingsActionItem(
                    icon = Icons.Outlined.Lock,
                    title = "Privacy Policy",
                    onClick = { showPrivacyDialog = true }
                )
            }

            // ── Account ─────────────────────────
            SettingsSection(title = "Account") {
                SettingsActionItem(
                    icon = Icons.Outlined.Email,
                    title = "Change Email",
                    onClick = { showChangeEmailDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
                SettingsActionItem(
                    icon = Icons.Outlined.Lock,
                    title = "Change Password",
                    onClick = { showChangePasswordDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
                SettingsActionItem(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Delete Account",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteAccountDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Dialogs ─────────────────────────────

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About FindCircle", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "FindCircle v2.0\n\nA community platform to reunite people with their lost belongings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close") } }
        )
    }

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("Contact Support", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Need help? Reach out to us at support@findcircle.com.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = { TextButton(onClick = { showContactDialog = false }) { Text("Close") } }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Your privacy is important to us. FindCircle collects minimal data required to provide location-based lost and found services. All location data is strictly used for matchmaking purposes. We do not sell your personal information.\n\nAll imagery and chat logs are securely stored. Deleting your account will remove your data from our active databases.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") } }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
            title = { Text("Delete Account", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "This action is permanent. All your posts, messages, and saved data will be erased forever.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeletingAccount = true
                        viewModel.deleteAccount(
                            onSuccess = {
                                isDeletingAccount = false
                                showDeleteAccountDialog = false
                                onAccountDeleted()
                            },
                            onError = { errorMsg ->
                                isDeletingAccount = false
                                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isDeletingAccount,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Delete Forever")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    enabled = !isDeletingAccount
                ) { Text("Cancel") }
            }
        )
    }

    if (showReauthDialog) {
        AlertDialog(
            onDismissRequest = { 
                showReauthDialog = false
                reauthPassword = ""
                viewModel.resetAuthActionState()
            },
            title = { Text("Security Check") },
            text = {
                Column {
                    Text("Please re-enter your current password to verify your identity.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        }
                    )
                    if (authActionState is com.example.findcircle.ui.auth.AuthState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (authActionState as com.example.findcircle.ui.auth.AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.reauthenticate(reauthPassword) {
                            showReauthDialog = false
                            reauthPassword = ""
                            pendingAuthAction?.invoke()
                            pendingAuthAction = null
                        }
                    },
                    enabled = authActionState !is com.example.findcircle.ui.auth.AuthState.Loading && reauthPassword.isNotBlank()
                ) {
                    if (authActionState is com.example.findcircle.ui.auth.AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Verify")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showReauthDialog = false
                    reauthPassword = ""
                    pendingAuthAction = null
                    viewModel.resetAuthActionState()
                }) { Text("Cancel") }
            }
        )
    }

    if (showChangeEmailDialog) {
        AlertDialog(
            onDismissRequest = { 
                showChangeEmailDialog = false
                newEmail = ""
                viewModel.resetAuthActionState()
            },
            title = { Text("Change Email") },
            text = {
                Column {
                    Text("Enter your new email address.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("New Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (authActionState is com.example.findcircle.ui.auth.AuthState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (authActionState as com.example.findcircle.ui.auth.AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (authActionState is com.example.findcircle.ui.auth.AuthState.Success) {
                        LaunchedEffect(Unit) {
                            android.widget.Toast.makeText(context, "Verification email sent! Check your new inbox to confirm the change.", android.widget.Toast.LENGTH_LONG).show()
                            showChangeEmailDialog = false
                            newEmail = ""
                            viewModel.resetAuthActionState()
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        pendingAuthAction = { viewModel.updateEmail(newEmail) }
                        showReauthDialog = true 
                    },
                    enabled = authActionState !is com.example.findcircle.ui.auth.AuthState.Loading && newEmail.isNotBlank()
                ) {
                    if (authActionState is com.example.findcircle.ui.auth.AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Update")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showChangeEmailDialog = false
                    newEmail = ""
                    viewModel.resetAuthActionState()
                }) { Text("Cancel") }
            }
        )
    }
    
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showChangePasswordDialog = false
                newPassword = ""
                viewModel.resetAuthActionState()
            },
            title = { Text("Change Password") },
            text = {
                Column {
                    Text("Enter your new password below.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        }
                    )
                    if (authActionState is com.example.findcircle.ui.auth.AuthState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (authActionState as com.example.findcircle.ui.auth.AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (authActionState is com.example.findcircle.ui.auth.AuthState.Success) {
                        LaunchedEffect(Unit) {
                            android.widget.Toast.makeText(context, "Password updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                            showChangePasswordDialog = false
                            newPassword = ""
                            viewModel.resetAuthActionState()
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        pendingAuthAction = { viewModel.updatePassword(newPassword) }
                        showReauthDialog = true 
                    },
                    enabled = authActionState !is com.example.findcircle.ui.auth.AuthState.Loading && newPassword.length >= 6
                ) {
                    if (authActionState is com.example.findcircle.ui.auth.AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Update")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showChangePasswordDialog = false
                    newPassword = ""
                    viewModel.resetAuthActionState()
                }) { Text("Cancel") }
            }
        )
    }
}

// ─── Reusable Section Card ──────────────────────────────────────────────

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == MaterialTheme.colorScheme.error) titleColor else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
