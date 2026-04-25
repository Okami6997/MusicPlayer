package com.musicplayer.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.ui.components.TrackListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tracks", "Albums", "Artists", "Playlists")
    val context = LocalContext.current

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.scanLocalLibrary()
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
                    if (uiState.tracks.isEmpty()) {
                        EmptyLibraryState(onScanClick = { checkAndScanLocal() })
                    } else {
                        LazyColumn {
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
                1 -> {
                    if (uiState.albums.isEmpty()) {
                        EmptyLibraryState(onScanClick = { checkAndScanLocal() })
                    } else {
                        LazyColumn {
                            items(uiState.albums) { album ->
                                ListItem(
                                    modifier = Modifier.clickable { onNavigateToAlbum(album.id) },
                                    headlineContent = { Text(album.title) },
                                    supportingContent = { Text("${album.artist} • ${album.trackCount} tracks • ${album.sourceName}") },
                                    trailingContent = {
                                        IconButton(onClick = { viewModel.downloadAlbum(album) }) {
                                            Icon(Icons.Default.Download, contentDescription = "Download Album")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    if (uiState.artists.isEmpty()) {
                        EmptyLibraryState(onScanClick = { checkAndScanLocal() })
                    } else {
                        LazyColumn {
                            items(uiState.artists) { artist ->
                                ListItem(
                                    modifier = Modifier.clickable { onNavigateToArtist(artist.name) },
                                    headlineContent = { Text(artist.name) },
                                    supportingContent = { Text("${artist.albumCount} albums") }
                                )
                            }
                        }
                    }
                }
                3 -> {
                    if (uiState.playlists.isEmpty()) {
                        EmptyLibraryState(onScanClick = { checkAndScanLocal() }, message = "No playlists found")
                    } else {
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
}

@Composable
fun EmptyLibraryState(
    onScanClick: () -> Unit,
    message: String = "No music found in your library"
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan your local library or add a music source in Settings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onScanClick) {
            Text("Scan Local Library")
        }
    }
}
