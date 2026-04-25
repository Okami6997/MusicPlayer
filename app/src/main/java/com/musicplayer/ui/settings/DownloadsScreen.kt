package com.musicplayer.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, takeFlags)
            viewModel.setDownloadLocation(it.toString())
        }
    }

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
                supportingContent = { Text(uiState.downloadLocation) },
                leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { launcher.launch(null) }
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
                    listOf("Lossless", "High (320 kbps)", "Medium (192 kbps)", "Low (128 kbps)").forEach { quality ->
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