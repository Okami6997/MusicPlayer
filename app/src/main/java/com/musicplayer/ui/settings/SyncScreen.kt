package com.musicplayer.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.MediaSourceType

/**
 * Sync settings screen. Exposes global "Sync All (Delta)" and "Sync All (Full)"
 * actions, plus a per-source list with delta and full sync controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                // Header / explanation
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Library Sync",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Delta sync only downloads tracks that have changed since the last sync. " +
                            "Use Full sync if you want to rebuild the local cache from scratch.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                // Global actions
                ListItem(
                    headlineContent = { Text("Sync All Sources (Delta)") },
                    supportingContent = { Text("Fast — downloads only changes") },
                    leadingContent = { Icon(Icons.Default.Sync, contentDescription = null) },
                    trailingContent = {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { viewModel.syncAllDelta() }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run delta sync")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.isSyncing) { viewModel.syncAllDelta() }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Full Sync All Sources") },
                    supportingContent = { Text("Slow — rebuilds the entire local cache") },
                    leadingContent = { Icon(Icons.Default.CloudSync, contentDescription = null) },
                    trailingContent = {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { viewModel.syncAllFull() }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run full sync")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.isSyncing) { viewModel.syncAllFull() }
                )
                HorizontalDivider()
            }

            item {
                Text(
                    "Per-Source Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (uiState.sources.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No sources configured")
                        Text(
                            "Add a source from the Music Sources screen first",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.sources) { source ->
                    SyncSourceListItem(
                        source = source,
                        isActive = uiState.activeSourceId == source.id,
                        onDeltaSync = { viewModel.deltaSyncSource(source) },
                        onFullSync = { viewModel.fullSyncSource(source) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SyncSourceListItem(
    source: MediaSource,
    isActive: Boolean,
    onDeltaSync: () -> Unit,
    onFullSync: () -> Unit
) {
    val lastSyncText = remember(source.lastDeltaSyncAt, source.lastFullSyncAt) {
        formatLastSync(source.lastDeltaSyncAt, source.lastFullSyncAt)
    }
    ListItem(
        headlineContent = { Text(source.name) },
        supportingContent = {
            Column {
                Text(source.type.name)
                if (lastSyncText.isNotEmpty()) {
                    Text(
                        lastSyncText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
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
            if (isActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Row {
                    IconButton(onClick = onDeltaSync) {
                        Icon(Icons.Default.Sync, contentDescription = "Delta sync")
                    }
                    IconButton(onClick = onFullSync) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Full sync")
                    }
                }
            }
        }
    )
}

private fun formatLastSync(lastDeltaSyncAt: Long, lastFullSyncAt: Long): String {
    if (lastDeltaSyncAt <= 0L && lastFullSyncAt <= 0L) return "Never synced"
    val deltaText = if (lastDeltaSyncAt > 0L) "Delta: ${formatRelative(lastDeltaSyncAt)}" else null
    val fullText = if (lastFullSyncAt > 0L) "Full: ${formatRelative(lastFullSyncAt)}" else null
    return listOfNotNull(deltaText, fullText).joinToString(" • ")
}

private fun formatRelative(epochMillis: Long): String {
    val diffMs = System.currentTimeMillis() - epochMillis
    val mins = diffMs / 60_000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 60 * 24 -> "${mins / 60}h ago"
        else -> "${mins / (60 * 24)}d ago"
    }
}
