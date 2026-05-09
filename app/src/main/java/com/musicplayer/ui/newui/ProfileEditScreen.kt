package com.musicplayer.ui.newui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.profile.MediaServiceType
import com.musicplayer.profile.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    profileId: String?,
    onNavigateBack: () -> Unit = {},
    viewModel: ProfileEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (profileId == null) "Create Profile" else "Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                ProfileEditForm(
                    profile = uiState.profile,
                    isTestingConnection = uiState.isTestingConnection,
                    testConnectionResult = uiState.testConnectionResult,
                    onTestConnection = { name, serviceType, ipAddress, portOverride, username, password, token ->
                        viewModel.testConnection(name, serviceType, ipAddress, portOverride, username, password, token)
                    },
                    onSave = { name, serviceType, ipAddress, portOverride, username, password, token, downloadPort ->
                        viewModel.saveProfile(name, serviceType, ipAddress, portOverride, username, password, token, downloadPort)
                    },
                    onCancel = onNavigateBack
                )
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
private fun ProfileEditForm(
    profile: Profile?,
    isTestingConnection: Boolean,
    testConnectionResult: com.musicplayer.data.repository.ConnectionTestResult?,
    onTestConnection: (String, MediaServiceType, String, Int?, String, String, String) -> Unit,
    onSave: (String, MediaServiceType, String, Int?, String, String, String, Int) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var serviceType by remember { mutableStateOf(profile?.serviceType ?: MediaServiceType.Jellyfin) }
    var ipAddress by remember { mutableStateOf(profile?.ipAddress ?: "") }
    var portOverride by remember { mutableStateOf(profile?.portOverride?.toString() ?: "") }
    var useCustomPort by remember { mutableStateOf(profile?.portOverride != null) }
    var username by remember { mutableStateOf(profile?.username ?: "") }
    var password by remember { mutableStateOf(profile?.password ?: "") }
    var token by remember { mutableStateOf(profile?.token ?: "") }
    var showPassword by remember { mutableStateOf(false) }
    var downloadPort by remember { mutableStateOf(profile?.downloadPort?.toString() ?: "3000") }

    val needsTokenAuth = serviceType == MediaServiceType.Jellyfin ||
            serviceType == MediaServiceType.Emby ||
            serviceType == MediaServiceType.Plex
    val needsUserPassAuth = serviceType == MediaServiceType.Subsonic ||
            serviceType == MediaServiceType.OpenSubsonic ||
            serviceType == MediaServiceType.Navidrome

    val isValid = name.isNotBlank() && ipAddress.isNotBlank() &&
            (useCustomPort && portOverride.isNotBlank() || !useCustomPort)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Profile Name") },
            placeholder = { Text("e.g., Home Server") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Label, contentDescription = null)
            }
        )

        // Service type selector
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Media Service Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                MediaServiceType.allTypes.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { serviceType = type }
                            .padding(8.dp)
                    ) {
                        RadioButton(
                            selected = serviceType == type,
                            onClick = { serviceType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(type.displayName ?: "Unknown")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "(Port ${type.defaultPort})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // IP Address field
        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("IP Address") },
            placeholder = { Text("e.g., 192.168.1.100") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Wifi, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // Port override toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Switch(
                checked = useCustomPort,
                onCheckedChange = { useCustomPort = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use Custom Port")
        }

        // Port override field
        if (useCustomPort) {
            OutlinedTextField(
                value = portOverride,
                onValueChange = { portOverride = it },
                label = { Text("Port") },
                placeholder = { Text("e.g., 8096") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.SettingsEthernet, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        // Credentials section
        if (needsTokenAuth) {
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("API Token") },
                placeholder = { Text("Paste your API key / token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
            )
        }

        if (needsUserPassAuth) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }

        // Download server port
        OutlinedTextField(
            value = downloadPort,
            onValueChange = { downloadPort = it },
            label = { Text("Download Server Port") },
            placeholder = { Text("3000") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
            supportingText = { Text("Port for the download service (default: 3000)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // Test connection button
        val effectivePortForTest = if (useCustomPort && portOverride.isNotBlank()) portOverride.toIntOrNull() else null
        OutlinedButton(
            onClick = { onTestConnection(name, serviceType, ipAddress, effectivePortForTest, username, password, token) },
            enabled = ipAddress.isNotBlank() && !isTestingConnection,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isTestingConnection) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing...")
            } else {
                Icon(Icons.Default.NetworkCheck, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Connection")
            }
        }

        testConnectionResult?.let { result ->
            when (result) {
                is com.musicplayer.data.repository.ConnectionTestResult.Success ->
                    Text(
                        "Connection successful!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                is com.musicplayer.data.repository.ConnectionTestResult.Error ->
                    Text(
                        "Failed: ${result.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
            }
        }

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Connection Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                val effectivePort = if (useCustomPort && portOverride.isNotBlank()) {
                    portOverride.toIntOrNull() ?: serviceType?.defaultPort ?: 8096
                } else {
                    serviceType?.defaultPort ?: 8096
                }
                Text(
                    "URL: http://$ipAddress:$effectivePort",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "Download URL: http://$ipAddress:${downloadPort.toIntOrNull() ?: 3000}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val port = if (useCustomPort && portOverride.isNotBlank()) {
                        portOverride.toIntOrNull()
                    } else {
                        null
                    }
                    onSave(name, serviceType, ipAddress, port, username, password, token, downloadPort.toIntOrNull() ?: 3000)
                },
                enabled = isValid,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (profile == null) "Create" else "Save")
            }
        }
    }
}
