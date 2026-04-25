package com.musicplayer.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.domain.model.Track
import com.musicplayer.ui.components.AddToPlaylistDialog
import com.musicplayer.ui.components.TrackListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playlists by viewModel.getAllPlaylists().collectAsState(initial = emptyList())
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    val focusRequester = remember { FocusRequester() }

    // Internal state to manage the TextField synchronously and maintain cursor position
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = uiState.query,
                selection = TextRange(uiState.query.length)
            )
        )
    }

    // Sync from VM (e.g. for Clear action or external updates)
    LaunchedEffect(uiState.query) {
        if (uiState.query != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = uiState.query,
                selection = TextRange(uiState.query.length)
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
                title = {
                    TextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            viewModel.updateQuery(it.text)
                        },
                        placeholder = { Text("Search tracks, artists, albums…") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        trailingIcon = {
                            if (textFieldValue.text.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (textFieldValue.text.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Type to search your music library", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (uiState.results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results for \"${textFieldValue.text}\"")
                }
            } else {
                LazyColumn {
                    items(uiState.results) { track ->
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
}
