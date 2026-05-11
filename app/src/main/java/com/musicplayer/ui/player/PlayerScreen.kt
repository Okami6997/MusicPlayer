package com.musicplayer.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.musicplayer.domain.model.Lyrics
import com.musicplayer.domain.model.PlayerState
import com.musicplayer.domain.model.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: com.musicplayer.ui.settings.SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val track = uiState.currentTrack

    var showPlaylistDialog by remember { mutableStateOf(false) }
    val playlists by viewModel.getAllPlaylists().collectAsState(initial = emptyList())

    if (showPlaylistDialog && track != null) {
        com.musicplayer.ui.components.AddToPlaylistDialog(
            track = track,
            playlists = playlists,
            onDismiss = { showPlaylistDialog = false },
            onPlaylistSelected = { playlist ->
                viewModel.addCurrentTrackToPlaylist(playlist.id)
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylistAndAddCurrentTrack(name)
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ── blurred, faded album art background ──────────────────────
        if (settingsState.dynamicColorEnabled && track?.artworkUri != null) {
            AsyncImage(
                model = track.artworkUri,
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
                        IconButton(onClick = { viewModel.downloadCurrentTrack() }) {
                            Icon(
                                imageVector = if (track?.isDownloaded == true) Icons.Default.DownloadDone else Icons.Default.Download,
                                contentDescription = "Download"
                            )
                        }
                        IconButton(onClick = { showPlaylistDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to Playlist")
                        }
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
                        IconButton(onClick = { viewModel.toggleQueue() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = if (uiState.showQueue)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                PlayerContent(uiState, viewModel)
                
                if (uiState.showQueue) {
                    QueueView(
                        queue = uiState.queue,
                        currentIndex = uiState.currentQueueIndex,
                        onTrackClick = { index -> viewModel.playQueueIndex(index) },
                        onDismiss = { viewModel.toggleQueue() }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueView(
    queue: List<com.musicplayer.domain.model.Track>,
    currentIndex: Int,
    onTrackClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Up Next",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close queue")
                }
            }

            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(queue) { index, track ->
                    val isPlaying = index == currentIndex
                    val trackColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    ListItem(
                        modifier = Modifier.clickable { onTrackClick(index) },
                        headlineContent = {
                            Text(
                                track.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isPlaying) FontWeight.Medium else FontWeight.Normal,
                                color = trackColor
                            )
                        },
                        supportingContent = {
                            Text(
                                track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPlaying) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        "${index + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun PlayerContent(
    uiState: com.musicplayer.domain.model.PlayerUiState,
    viewModel: PlayerViewModel
) {
    val track = uiState.currentTrack

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                currentWordIndex = uiState.currentWordIndex,
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
            if (track?.sourceName?.isNotEmpty() == true) {
                Text(
                    text = track.sourceName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
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
                label = { Text("Casting to ${uiState.castDevice}", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Cast,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
    currentWordIndex: Int = -1,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to current line with smooth animation
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && lyrics.isSynced) {
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
                val isPreviousActive = lyrics.isSynced && index == currentLineIndex - 1
                val hasWords = line.words.isNotEmpty()

                // Animate color transition
                val textColor by animateColorAsState(
                    targetValue = when {
                        isActive -> MaterialTheme.colorScheme.primary
                        isPreviousActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(durationMillis = 300),
                    label = "lyricsColor"
                )

                // Animate scale for active line
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.05f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "lyricsScale"
                )

                val alpha by animateFloatAsState(
                    targetValue = when {
                        isActive -> 1f
                        isPreviousActive -> 0.7f
                        index < currentLineIndex -> 0.4f
                        else -> 1f
                    },
                    animationSpec = tween(durationMillis = 300),
                    label = "lyricsAlpha"
                )

                // Render words with wavy flow or regular text
                if (hasWords && isActive) {
                    // Wavy flow - render each word with individual animation
                    FlowingLyricsLine(
                        words = line.words,
                        currentWordIndex = currentWordIndex,
                        onWordClick = { wordStartTime ->
                            onLineClick(wordStartTime)
                        },
                        modifier = Modifier
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clickable(enabled = lyrics.isSynced && line.timeMs >= 0) {
                                onLineClick(line.timeMs)
                            }
                            .padding(vertical = 12.dp)
                    )
                } else {
                    // Regular text rendering (for inactive lines or lines without word data)
                    Text(
                        text = line.text.ifEmpty { "♪" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = if (isActive) MaterialTheme.typography.bodyLarge.fontSize * 1.1f
                                       else MaterialTheme.typography.bodyLarge.fontSize
                        ),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = textColor.copy(alpha = alpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clickable(enabled = lyrics.isSynced && line.timeMs >= 0) {
                                onLineClick(line.timeMs)
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Renders lyrics words with wavy flow animation - each word animates as it plays
 * Uses a single Text composable with AnnotatedString for proper text wrapping
 */
@Composable
private fun FlowingLyricsLine(
    words: List<com.musicplayer.domain.model.LyricsWord>,
    currentWordIndex: Int,
    onWordClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate the overall state for the line
    val isAnyWordActive = currentWordIndex >= 0

    val lineColor by animateColorAsState(
        targetValue = if (isAnyWordActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 150),
        label = "lineColor"
    )

    // Build annotated string with per-word styling
    // This allows proper text wrapping while maintaining word-level highlighting
    val annotatedString = buildAnnotatedString {
        words.forEachIndexed { index, word ->
            val isCurrentWord = index == currentWordIndex
            val isPastWord = index < currentWordIndex

            val color = when {
                isCurrentWord -> MaterialTheme.colorScheme.primary
                isPastWord -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            }

            pushStyle(
                SpanStyle(
                    color = color,
                    fontWeight = when {
                        isCurrentWord -> FontWeight.Bold
                        isPastWord -> FontWeight.SemiBold
                        else -> FontWeight.Normal
                    }
                )
            )
            append(word.word)
            pop()

            // Add space between words (except for last word)
            if (index < words.lastIndex) {
                append(" ")
            }
        }
    }

    // Handle word click by finding which word was tapped
    // This is a simplified approach - click detects on the whole line
    val clickableModifier = if (words.isNotEmpty() && currentWordIndex >= 0) {
        Modifier.clickable {
            words.getOrNull(currentWordIndex)?.let { onWordClick(it.startTimeMs) }
        }
    } else {
        Modifier
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.1f
        ),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(vertical = 12.dp)
    )
}
