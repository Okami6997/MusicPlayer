package com.musicplayer.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.ui.components.TrackListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    onNavigateToPlayer: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(artistName) {
        viewModel.loadArtist(artistName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.artistName ?: "Artist") },
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
                items(uiState.tracks) { track ->
                    TrackListItem(
                        track = track,
                        onClick = {
                            viewModel.playTrack(track)
                            onNavigateToPlayer()
                        },
                        onDownloadClick = { viewModel.downloadTrack(track) }
                    )
                }
            }
        }
    }
}
