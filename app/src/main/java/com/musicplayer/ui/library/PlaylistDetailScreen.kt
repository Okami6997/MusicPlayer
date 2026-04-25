package com.musicplayer.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.musicplayer.domain.model.Playlist
import com.musicplayer.domain.model.Track
import com.musicplayer.ui.components.AddToPlaylistDialog
import com.musicplayer.ui.components.TrackListItem

@androidx.media3.common.util.UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onNavigateToPlayer: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playlists by viewModel.getAllPlaylists().collectAsState(initial = emptyList())
    var trackForMenu by remember { mutableStateOf<Track?>(null) }
    var showTrackMenu by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    if (showAddToPlaylistDialog && trackForMenu != null) {
        AddToPlaylistDialog(
            track = trackForMenu!!,
            playlists = playlists,
            onDismiss = { showAddToPlaylistDialog = false },
            onPlaylistSelected = { playlist ->
                viewModel.addTrackToPlaylist(trackForMenu!!, playlist.id)
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylistAndAddTrack(name, trackForMenu!!)
            }
        )
    }

    if (showDeletePlaylistConfirm) {
        AlertDialog(
            onDismissRequest = { showDeletePlaylistConfirm = false },
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete this playlist? The tracks will remain in your library.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist()
                        showDeletePlaylistConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePlaylistConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.playlist?.name ?: "Playlist") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeletePlaylistConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Playlist")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    PlaylistHeader(
                        playlist = uiState.playlist,
                        onPlayClick = {
                            viewModel.playPlaylist()
                            onNavigateToPlayer()
                        }
                    )
                }
                items(uiState.tracks) { track ->
                    TrackListItem(
                        track = track,
                        onClick = {
                            viewModel.playTrack(track)
                            onNavigateToPlayer()
                        },
                        onDownloadClick = { viewModel.downloadTrack(track) },
                        onMoreClick = {
                            trackForMenu = track
                            showTrackMenu = true
                        }
                    )
                }
            }
        }
    }

    if (showTrackMenu && trackForMenu != null) {
        ModalBottomSheet(
            onDismissRequest = { showTrackMenu = false }
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("Add to another playlist") },
                    leadingContent = { Icon(Icons.Default.MoreVert, null) },
                    modifier = Modifier.clickable {
                        showTrackMenu = false
                        showAddToPlaylistDialog = true
                    }
                )
                ListItem(
                    headlineContent = { Text("Remove from this playlist") },
                    leadingContent = { Icon(Icons.Default.Delete, null) },
                    modifier = Modifier.clickable {
                        viewModel.removeTrackFromPlaylist(trackForMenu!!)
                        showTrackMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistHeader(
    playlist: Playlist?,
    onPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (playlist?.artworkUri != null) {
            AsyncImage(
                model = playlist.artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .clip(MaterialTheme.shapes.medium),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(playlist?.name ?: "", style = MaterialTheme.typography.headlineSmall)
            Text("${playlist?.trackCount ?: 0} tracks", style = MaterialTheme.typography.bodyMedium)
            if (!playlist?.description.isNullOrBlank()) {
                Text(playlist?.description ?: "", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onPlayClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Play")
            }
        }
    }
}
