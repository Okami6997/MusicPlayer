package com.musicplayer.ui.sources

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.MediaSourceType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onNavigateBack: () -> Unit,
    viewModel: SourcesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var sourceToEdit by remember { mutableStateOf<MediaSource?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            viewModel.scanLocalLibrary()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Storage permission is required to scan local music")
            }
        }
    }

    fun checkAndScanLocal() {
        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            viewModel.scanLocalLibrary()
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    val sourcePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            // Re-trigger scan for the local source - we don't have the source here easily without extra state
            // but usually scanLocalLibrary() does the same thing for now.
            viewModel.scanLocalLibrary()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Storage permission is required to scan local music")
            }
        }
    }

    fun checkAndScanSource(source: MediaSource) {
        if (source.type == MediaSourceType.LOCAL) {
            val allGranted = permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) {
                viewModel.scanSource(source)
            } else {
                sourcePermissionLauncher.launch(permissionsToRequest)
            }
        } else {
            viewModel.scanSource(source)
        }
    }

    LaunchedEffect(uiState.scanProgress) {
        if (uiState.scanProgress.isNotEmpty()) {
            val result = snackbarHostState.showSnackbar(
                message = uiState.scanProgress,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music Sources") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { checkAndScanLocal() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan Library")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add source")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            items(uiState.sources) { source ->
                SourceListItem(
                    source = source,
                    onEdit = {
                        sourceToEdit = source
                        showEditDialog = true
                    },
                    onDelete = { viewModel.deleteSource(source.id) },
                    onScan = { checkAndScanSource(source) }
                )
                HorizontalDivider()
            }
            if (uiState.sources.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No sources configured")
                        Text("Tap + to add Plex, Emby, Jellyfin, Subsonic or Navidrome",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSourceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { source ->
                viewModel.addSource(source)
                showAddDialog = false
            },
            viewModel = viewModel
        )
    }

    if (showEditDialog && sourceToEdit != null) {
        EditSourceDialog(
            source = sourceToEdit!!,
            onDismiss = {
                showEditDialog = false
                sourceToEdit = null
            },
            onSave = { updatedSource ->
                viewModel.updateSource(updatedSource)
                showEditDialog = false
                sourceToEdit = null
            },
            viewModel = viewModel
        )
    }
}

@Composable
private fun SourceListItem(
    source: MediaSource,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onScan: () -> Unit
) {
    ListItem(
        headlineContent = { Text(source.name) },
        supportingContent = { Text("${source.type.name} • ${source.baseUrl.ifEmpty { "Local" }}") },
        leadingContent = {
            Icon(
                imageVector = when (source.type) {
                    MediaSourceType.LOCAL -> Icons.Default.FolderOpen
                    MediaSourceType.PLEX -> Icons.Default.Tv
                    MediaSourceType.EMBY, MediaSourceType.JELLYFIN -> Icons.Default.Stream
                    MediaSourceType.SUBSONIC, MediaSourceType.OPEN_SUBSONIC,
                    MediaSourceType.NAVIDROME -> Icons.Default.MusicNote
                    MediaSourceType.AUDIOBOOKSHELF -> Icons.Default.MenuBook
                    MediaSourceType.CLOUD_DRIVE -> Icons.Default.Cloud
                    MediaSourceType.USER -> Icons.Default.LibraryMusic
                },
                contentDescription = null
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onScan) {
                    Icon(Icons.Default.Refresh, contentDescription = "Scan source")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit source")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete source")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAdd: (MediaSource) -> Unit,
    viewModel: SourcesViewModel
) {
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(MediaSourceType.JELLYFIN) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestConnectionResult?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Music Source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Source type selector
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MediaSourceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = { selectedType = type; expanded = false }
                            )
                        }
                    }
                }

                if (selectedType != MediaSourceType.LOCAL) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("http://192.168.1.100:8096") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Default.Visibility
                            else Icons.Default.VisibilityOff

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    // Test connection button
                    OutlinedButton(
                        onClick = {
                            isTestingConnection = true
                            val testSource = MediaSource(
                                id = "",
                                name = name.ifBlank { selectedType.name },
                                type = selectedType,
                                baseUrl = baseUrl.trimEnd('/'),
                                username = username,
                                password = password
                            )
                            scope.launch {
                                testResult = viewModel.testConnection(testSource)
                                isTestingConnection = false
                            }
                        },
                        enabled = baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...")
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Connection")
                        }
                    }

                    // Test result display
                    testResult?.let { result ->
                        when (result) {
                            is TestConnectionResult.Success -> {
                                Text(
                                    "Connection successful!",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            is TestConnectionResult.Error -> {
                                Text(
                                    "Connection failed: ${result.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(
                        MediaSource(
                            id = java.util.UUID.randomUUID().toString(),
                            name = name.ifBlank { selectedType.name },
                            type = selectedType,
                            baseUrl = baseUrl.trimEnd('/'),
                            username = username,
                            password = password
                        )
                    )
                },
                enabled = name.isNotBlank() || selectedType == MediaSourceType.LOCAL
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSourceDialog(
    source: MediaSource,
    onDismiss: () -> Unit,
    onSave: (MediaSource) -> Unit,
    viewModel: SourcesViewModel
) {
    var name by remember { mutableStateOf(source.name) }
    var baseUrl by remember { mutableStateOf(source.baseUrl) }
    var username by remember { mutableStateOf(source.username) }
    var password by remember { mutableStateOf(source.password) }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(source.type) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestConnectionResult?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Music Source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Source type selector (read-only for editing)
                ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        enabled = false
                    )
                }

                if (selectedType != MediaSourceType.LOCAL) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Server URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Default.Visibility
                            else Icons.Default.VisibilityOff

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    // Test connection button
                    OutlinedButton(
                        onClick = {
                            isTestingConnection = true
                            val testSource = MediaSource(
                                id = source.id,
                                name = name,
                                type = selectedType,
                                baseUrl = baseUrl.trimEnd('/'),
                                username = username,
                                password = password
                            )
                            scope.launch {
                                testResult = viewModel.testConnection(testSource)
                                isTestingConnection = false
                            }
                        },
                        enabled = baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...")
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Connection")
                        }
                    }

                    // Test result display
                    testResult?.let { result ->
                        when (result) {
                            is TestConnectionResult.Success -> {
                                Text(
                                    "Connection successful!",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            is TestConnectionResult.Error -> {
                                Text(
                                    "Connection failed: ${result.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        source.copy(
                            name = name,
                            type = selectedType,
                            baseUrl = baseUrl.trimEnd('/'),
                            username = username,
                            password = password
                        )
                    )
                },
                enabled = name.isNotBlank() || selectedType == MediaSourceType.LOCAL
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
