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
fun LibraryScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tracks", "Albums", "Artists", "Playlists")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn {
                        items(uiState.tracks) { track ->
                            TrackListItem(track = track, onClick = { onNavigateToPlayer() })
                        }
                    }
                }
                1 -> {
                    LazyColumn {
                        items(uiState.albums) { album ->
                            ListItem(
                                headlineContent = { Text(album.title) },
                                supportingContent = { Text("${album.artist} • ${album.trackCount} tracks") }
                            )
                        }
                    }
                }
                2 -> {
                    LazyColumn {
                        items(uiState.artists) { artist ->
                            ListItem(
                                headlineContent = { Text(artist.name) },
                                supportingContent = { Text("${artist.albumCount} albums") }
                            )
                        }
                    }
                }
                3 -> {
                    LazyColumn {
                        items(uiState.playlists) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = { Text("${playlist.trackCount} tracks") }
                            )
                        }
                    }
                }
            }
        }
    }
}
