package com.musicplayer.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.musicplayer.domain.model.Album
import com.musicplayer.domain.model.Track
import com.musicplayer.ui.components.AddToPlaylistDialog
import com.musicplayer.ui.components.TrackListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onNavigateToPlayer: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playlists by viewModel.getAllPlaylists().collectAsState(initial = emptyList())
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }

    if (trackToAddToPlaylist != null) {
        AddToPlaylistDialog(
            track = trackToAddToPlaylist!!,
            playlists = playlists,
            onDismiss = { trackToAddToPlaylist = null },
            onPlaylistSelected = { playlist ->
                viewModel.addTrackToPlaylist(trackToAddToPlaylist!!, playlist.id)
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylistAndAddTrack(name, trackToAddToPlaylist!!)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.album?.title ?: "Album") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    AlbumHeader(
                        album = uiState.album,
                        onPlayClick = {
                            viewModel.playAlbum()
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
                        onMoreClick = { trackToAddToPlaylist = track }
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumHeader(
    album: Album?,
    onPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album?.artworkUri,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(album?.title ?: "", style = MaterialTheme.typography.headlineSmall)
            Text(album?.artist ?: "", style = MaterialTheme.typography.titleMedium)
            Text("${album?.trackCount ?: 0} tracks", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onPlayClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Play")
            }
        }
    }
}
