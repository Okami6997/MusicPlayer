package com.musicplayer.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.musicplayer.domain.model.Lyrics
import com.musicplayer.domain.model.PlayerState
import com.musicplayer.domain.model.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val track = uiState.currentTrack

    // ── dynamic theme from album artwork ─────────────────────────────────
    val artworkColors = rememberArtworkColors(track?.artworkUri)
    val dynamicScheme = animatedArtworkColorScheme(artworkColors)

    MaterialTheme(colorScheme = dynamicScheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── blurred, faded album art background ──────────────────────
            if (track?.artworkUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = track.artworkUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(60.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )
                // Scrim: darken so controls remain readable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                topBar = {
                    TopAppBar(
                        title = { Text("Now Playing") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (uiState.lyrics != null) {
                                IconButton(onClick = { viewModel.toggleLyrics() }) {
                                    Icon(
                                        Icons.Default.Lyrics,
                                        contentDescription = "Lyrics",
                                        tint = if (uiState.showLyrics)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            IconButton(onClick = { /* Open queue */ }) {
                                Icon(Icons.Default.QueueMusic, contentDescription = "Queue")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                PlayerContent(uiState, viewModel, paddingValues)
            }
        }
    }
}

@Composable
private fun PlayerContent(
    uiState: com.musicplayer.domain.model.PlayerUiState,
    viewModel: PlayerViewModel,
    paddingValues: PaddingValues
) {
    val track = uiState.currentTrack

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Album artwork or lyrics view – tap to toggle
        val showLyrics = uiState.showLyrics && uiState.lyrics != null
        
        val artModifier = Modifier
            .size(280.dp)
            .clip(MaterialTheme.shapes.large)
            .then(
                if (uiState.lyrics != null)
                    Modifier.clickable { viewModel.toggleLyrics() }
                else Modifier
            )

        if (showLyrics) {
            LyricsView(
                lyrics = uiState.lyrics!!,
                currentLineIndex = uiState.currentLyricsLineIndex,
                onLineClick = { timeMs -> viewModel.seekTo(timeMs) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .clickable { viewModel.toggleLyrics() }
            )
        } else {
            AsyncImage(
                model = track?.artworkUri,
                contentDescription = "Album art",
                modifier = artModifier,
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Track info
            Text(
                text = track?.title ?: "No track selected",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track?.artist ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track?.album ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress – squiggly seek bar
        val progress = if (uiState.durationMs > 0)
            uiState.currentPositionMs.toFloat() / uiState.durationMs.toFloat()
        else 0f

        SquigglySeekBar(
            progress = progress,
            onSeek = { frac -> viewModel.seekTo((frac * uiState.durationMs).toLong()) },
            isPlaying = uiState.playWhenReady && uiState.playerState != PlayerState.STOPPED,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = uiState.currentPositionMs.toTimeString(),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = uiState.durationMs.toTimeString(),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (uiState.isShuffleEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            // Previous
            IconButton(onClick = { viewModel.skipToPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(32.dp))
            }

            // Play/Pause
            FilledIconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (uiState.playWhenReady && uiState.playerState != PlayerState.STOPPED)
                        Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.playWhenReady) "Pause" else "Play",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Next
            IconButton(onClick = { viewModel.skipToNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(32.dp))
            }

            // Repeat
            IconButton(onClick = { viewModel.toggleRepeat() }) {
                Icon(
                    imageVector = when (uiState.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (uiState.repeatMode != RepeatMode.OFF)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cast status indicator
        if (uiState.castDevice != null) {
            AssistChip(
                onClick = {},
                label = { Text("Casting to ${uiState.castDevice}") },
                leadingIcon = { Icon(Icons.Default.Cast, contentDescription = null) }
            )
        }
    }
}

private fun Long.toTimeString(): String {
    val minutes = this / 60_000
    val seconds = (this % 60_000) / 1000
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun LyricsView(
    lyrics: Lyrics,
    currentLineIndex: Int,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to current line
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && lyrics.isSynced) {
            // Centre the active line in the visible area
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = -200
            )
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(lyrics.lines) { index, line ->
                val isActive = lyrics.isSynced && index == currentLineIndex
                Text(
                    text = line.text.ifEmpty { "♪" },
                    style = if (isActive)
                        MaterialTheme.typography.bodyLarge
                    else
                        MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = lyrics.isSynced && line.timeMs >= 0) {
                            onLineClick(line.timeMs)
                        }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}
