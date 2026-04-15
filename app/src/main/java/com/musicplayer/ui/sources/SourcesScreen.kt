package com.musicplayer.ui.sources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.MediaSourceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onNavigateBack: () -> Unit,
    viewModel: SourcesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music Sources") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
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
                    onDelete = { viewModel.deleteSource(source.id) }
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
            }
        )
    }
}

@Composable
private fun SourceListItem(
    source: MediaSource,
    onDelete: () -> Unit
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
                },
                contentDescription = null
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete source")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAdd: (MediaSource) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MediaSourceType.JELLYFIN) }

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
                        modifier = Modifier.fillMaxWidth()
                    )
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
