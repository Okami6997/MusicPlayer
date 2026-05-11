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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.domain.model.Track
import com.musicplayer.ui.components.AddToPlaylistDialog
import com.musicplayer.ui.components.TrackListItem
import com.musicplayer.ui.components.MiniPlayer
import com.musicplayer.ui.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateBack: () -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()
    val isSyncing by libraryViewModel.isSyncing.collectAsState()
    val syncMessage by libraryViewModel.syncMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val tabs = listOf("Tracks", "Albums", "Artists", "Playlists")
    val context = LocalContext.current

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            libraryViewModel.clearSyncMessage()
        }
    }

    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            libraryViewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            libraryViewModel.scanLocalLibrary()
        }
    }

    fun checkAndScanLocal() {
        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            libraryViewModel.scanLocalLibrary()
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    if (trackToAddToPlaylist != null) {
        AddToPlaylistDialog(
            track = trackToAddToPlaylist!!,
            playlists = uiState.playlists,
            onDismiss = { trackToAddToPlaylist = null },
            onPlaylistSelected = { playlist ->
                libraryViewModel.addTrackToPlaylist(trackToAddToPlaylist!!, playlist.id)
            },
            onCreatePlaylist = { name ->
                libraryViewModel.createPlaylistAndAddTrack(name, trackToAddToPlaylist!!)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search library...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            }
                        )
                    } else {
                        Text("Library")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp)
                        )
                    } else {
                        IconButton(onClick = { libraryViewModel.syncCurrentProfile() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync library")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 3) {
                FloatingActionButton(onClick = { showCreatePlaylistDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Playlist")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    val filteredTracks = if (searchQuery.isBlank()) {
                        uiState.tracks
                    } else {
                        val query = searchQuery.lowercase()
                        uiState.tracks.filter { track ->
                            track.title.lowercase().contains(query) ||
                            track.artist.lowercase().contains(query) ||
                            track.album.lowercase().contains(query)
                        }
                    }
                    if (filteredTracks.isEmpty()) {
                        EmptyLibraryState(
                            onScanClick = { checkAndScanLocal() },
                            message = if (searchQuery.isNotBlank()) "No tracks match your search" else "No music found"
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filteredTracks) { track ->
                                TrackListItem(
                                    track = track,
                                    onClick = {
                                        libraryViewModel.playTrack(track)
                                        onNavigateToPlayer()
                                    },
                                    onDownloadClick = { libraryViewModel.downloadTrack(track) },
                                    onMoreClick = { trackToAddToPlaylist = track }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    val filteredAlbums = if (searchQuery.isBlank()) {
                        uiState.albums
                    } else {
                        val query = searchQuery.lowercase()
                        uiState.albums.filter { album ->
                            album.title.lowercase().contains(query) ||
                            album.artist.lowercase().contains(query)
                        }
                    }
                    if (filteredAlbums.isEmpty()) {
                        EmptyLibraryState(
                            onScanClick = { checkAndScanLocal() },
                            message = if (searchQuery.isNotBlank()) "No albums match your search" else "No music found"
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filteredAlbums) { album ->
                                ListItem(
                                    modifier = Modifier.clickable { onNavigateToAlbum(album.id) },
                                    headlineContent = { Text(album.title) },
                                    supportingContent = { Text("${album.artist} • ${album.trackCount} tracks • ${album.sourceName}") },
                                    trailingContent = {
                                        IconButton(onClick = { libraryViewModel.downloadAlbum(album) }) {
                                            Icon(Icons.Default.Download, contentDescription = "Download Album")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    val filteredArtists = if (searchQuery.isBlank()) {
                        uiState.artists
                    } else {
                        val query = searchQuery.lowercase()
                        uiState.artists.filter { artist ->
                            artist.name.lowercase().contains(query)
                        }
                    }
                    if (filteredArtists.isEmpty()) {
                        EmptyLibraryState(
                            onScanClick = { checkAndScanLocal() },
                            message = if (searchQuery.isNotBlank()) "No artists match your search" else "No music found"
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filteredArtists) { artist ->
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
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(uiState.playlists) { playlist ->
                                ListItem(
                                    modifier = Modifier.clickable {
                                        onNavigateToPlaylist(playlist.id)
                                    },
                                    headlineContent = { Text(playlist.name) },
                                    supportingContent = { Text("${playlist.trackCount} tracks") },
                                    trailingContent = {
                                        var showMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { showMenu = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                                            }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Play") },
                                                    leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                                                    onClick = {
                                                        libraryViewModel.playPlaylist(playlist)
                                                        onNavigateToPlayer()
                                                        showMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete") },
                                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                                    onClick = {
                                                        libraryViewModel.deletePlaylist(playlist.id)
                                                        showMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            MiniPlayer(
                uiState = playerUiState,
                onExpand = onNavigateToPlayer,
                onPlayPause = { playerViewModel.togglePlayPause() },
                onSkipNext = { playerViewModel.skipToNext() }
            )
        }
    }
}

@Composable
fun EmptyLibraryState(
    onScanClick: () -> Unit,
    message: String = "No music found in your library"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LibraryMusic,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Scan your local library or add a source in Settings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onScanClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Scan Local Library")
        }
    }
}
