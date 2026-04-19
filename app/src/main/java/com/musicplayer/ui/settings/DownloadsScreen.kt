package com.musicplayer.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQualityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline / Downloads") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ListItem(
                headlineContent = { Text("Download Location") },
                supportingContent = { Text(uiState.downloadLocation.ifEmpty { "Internal storage" }) },
                leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { viewModel.showDownloadLocationDialog = true }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Download Quality") },
                supportingContent = { Text(uiState.downloadQuality) },
                leadingContent = { Icon(Icons.Default.HighQuality, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { showQualityDialog = true }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Storage Used") },
                supportingContent = { Text(uiState.storageUsed) },
                leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Clear Cache") },
                supportingContent = { Text("Free up storage space") },
                leadingContent = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                modifier = Modifier.clickable { viewModel.clearCache() }
            )
            HorizontalDivider()
        }
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Download Quality") },
            text = {
                Column {
                    listOf("High (320 kbps)", "Medium (192 kbps)", "Low (128 kbps)").forEach { quality ->
                        ListItem(
                            headlineContent = { Text(quality) },
                            modifier = Modifier.clickable {
                                viewModel.setDownloadQuality(quality)
                                showQualityDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Cancel") }
            }
        )
    }
}