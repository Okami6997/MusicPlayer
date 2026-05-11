package com.musicplayer.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.musicplayer.data.lyrics.LyricsLoader
import com.musicplayer.domain.model.Lyrics
import com.musicplayer.domain.model.PlayerState
import com.musicplayer.domain.model.PlayerUiState
import com.musicplayer.domain.model.RepeatMode
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerHolder
import com.musicplayer.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerHolder: PlayerHolder,
    private val lyricsLoader: LyricsLoader,
    private val downloadRepository: DownloadRepository,
    private val musicRepository: com.musicplayer.data.repository.MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var lyricsJob: Job? = null
    private var lastPlayer: Player? = null
    private var lastLoadedTrackId: String? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState()
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
            updateCurrentTrack()
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            updatePlaybackState()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentTrack()
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
        }
        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.update {
                it.copy(
                    repeatMode = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                        else -> RepeatMode.OFF
                    }
                )
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            Timber.e(error, "Player error: ${error.message}")
            updatePlaybackState()
        }
    }

    init {
        viewModelScope.launch {
            playerHolder.queue.collect {
                updateCurrentTrack()
            }
        }
        viewModelScope.launch {
            playerHolder.currentPlayerFlow.collect { player ->
                lastPlayer?.removeListener(playerListener)
                player.addListener(playerListener)
                lastPlayer = player

                @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
                val isCasting = player is androidx.media3.cast.CastPlayer
                _uiState.update { it.copy(castDevice = if (isCasting) "Chromecast" else null) }

                updatePlaybackState()
                updateCurrentTrack()
            }
        }
        startProgressUpdates()
    }

    private fun updatePlaybackState() {
        val player = playerHolder.currentPlayer
        val state = when {
            player.playbackState == Player.STATE_IDLE -> PlayerState.IDLE
            player.playbackState == Player.STATE_BUFFERING && player.playWhenReady -> PlayerState.LOADING
            player.playWhenReady && player.playbackState != Player.STATE_ENDED -> PlayerState.PLAYING
            player.playbackState == Player.STATE_ENDED -> PlayerState.STOPPED
            else -> PlayerState.PAUSED
        }
        _uiState.update { it.copy(
            playerState = state,
            playWhenReady = player.playWhenReady
        ) }
    }

    private fun updateCurrentTrack() {
        val player = playerHolder.currentPlayer
        val mediaItem = player.currentMediaItem
        val queue = playerHolder.queue.value
        
        var track = if (mediaItem != null) {
            queue.find { it.id == mediaItem.mediaId }
        } else null

        // If not in queue, try to create from MediaItem metadata (for external files)
        if (track == null && mediaItem != null) {
            val metadata = mediaItem.mediaMetadata
            track = Track(
                id = mediaItem.mediaId,
                title = (metadata.title ?: metadata.displayTitle ?: "External Audio").toString(),
                artist = (metadata.artist ?: metadata.albumArtist ?: "Unknown Artist").toString(),
                album = (metadata.albumTitle ?: "External File").toString(),
                duration = player.duration.coerceAtLeast(0L),
                uri = mediaItem.localConfiguration?.uri?.toString() ?: "",
                artworkUri = metadata.artworkUri?.toString(),
                sourceId = "external",
                sourceName = "External File",
                sourceType = com.musicplayer.domain.model.MediaSourceType.LOCAL
            )
        }

        _uiState.update { current ->
            current.copy(
                currentTrack = track,
                queue = queue,
                currentQueueIndex = player.currentMediaItemIndex,
                durationMs = player.duration.takeIf { it > 0 } ?: 0L
            )
        }

        if (track != null && track.id != lastLoadedTrackId && track.sourceId != "external") {
            lastLoadedTrackId = track.id
            loadLyrics(track)
        } else if (track == null || track.sourceId == "external") {
            lastLoadedTrackId = null
            _uiState.update { it.copy(lyrics = null, currentLyricsLineIndex = -1) }
        }
    }

    private fun loadLyrics(track: Track) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _uiState.update { it.copy(lyrics = null, currentLyricsLineIndex = -1, currentWordIndex = -1) }
            val lyrics = lyricsLoader.loadLyrics(track)
            // Update lyrics and immediately calculate current line index based on current player position
            val currentPos = try {
                playerHolder.currentPlayer.currentPosition
            } catch (e: Exception) {
                0L
            }
            val lineIndex = if (lyrics != null && lyrics.isSynced) {
                findCurrentLyricsLine(currentPos, lyrics)
            } else -1
            val wordIndex = if (lineIndex >= 0 && lyrics != null && lyrics.lines[lineIndex].words.isNotEmpty()) {
                findCurrentWordIndex(currentPos, lyrics.lines[lineIndex])
            } else -1
            _uiState.update { it.copy(lyrics = lyrics, currentLyricsLineIndex = lineIndex, currentWordIndex = wordIndex) }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val player = playerHolder.currentPlayer
                val posMs = player.currentPosition
                val lyrics = _uiState.value.lyrics
                val lineIndex = if (lyrics != null && lyrics.isSynced) {
                    findCurrentLyricsLine(posMs, lyrics)
                } else -1

                // Calculate word progress within current line for wavy flow
                val wordIndex = if (lineIndex >= 0 && lyrics != null) {
                    findCurrentWordIndex(posMs, lyrics.lines[lineIndex])
                } else -1

                _uiState.update {
                    it.copy(
                        currentPositionMs = posMs,
                        currentLyricsLineIndex = lineIndex,
                        currentWordIndex = wordIndex
                    )
                }
                delay(100)
            }
        }
    }

    private fun findCurrentWordIndex(positionMs: Long, line: com.musicplayer.domain.model.LyricsLine): Int {
        if (line.words.isEmpty()) return -1
        for (i in line.words.indices) {
            if (positionMs >= line.words[i].startTimeMs && positionMs < line.words[i].endTimeMs) {
                return i
            }
        }
        // If past all words, return last word index
        return if (positionMs >= line.words.last().endTimeMs) line.words.size - 1 else -1
    }

    private fun findCurrentLyricsLine(positionMs: Long, lyrics: Lyrics): Int {
        var idx = -1
        for (i in lyrics.lines.indices) {
            if (lyrics.lines[i].timeMs <= positionMs) idx = i else break
        }
        return idx
    }

    fun toggleLyrics() {
        _uiState.update { it.copy(showLyrics = !it.showLyrics) }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        playerHolder.playTracks(tracks, startIndex)
        updateCurrentTrack()
    }

    fun togglePlayPause() {
        val player = playerHolder.currentPlayer
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
            player.prepare()
            player.play()
        } else {
            if (player.playWhenReady) player.pause() else player.play()
        }
        updatePlaybackState()
    }

    fun skipToNext() = playerHolder.currentPlayer.seekToNextMediaItem()
    fun skipToPrevious() = playerHolder.currentPlayer.seekToPreviousMediaItem()

    fun seekTo(positionMs: Long) {
        playerHolder.currentPlayer.seekTo(positionMs)
    }

    fun toggleShuffle() {
        val player = playerHolder.currentPlayer
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        _uiState.update { it.copy(isShuffleEnabled = player.shuffleModeEnabled) }
    }

    fun toggleRepeat() {
        val player = playerHolder.currentPlayer
        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
        _uiState.update {
            it.copy(
                repeatMode = when (nextMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
            )
        }
    }

    fun downloadCurrentTrack() {
        val track = _uiState.value.currentTrack ?: return
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
        }
    }

    fun getAllPlaylists() = musicRepository.getAllPlaylists()

    fun addCurrentTrackToPlaylist(playlistId: String) {
        val track = _uiState.value.currentTrack ?: return
        viewModelScope.launch {
            musicRepository.addTrackToPlaylist(playlistId, track.id)
        }
    }

    fun createPlaylistAndAddCurrentTrack(name: String) {
        val track = _uiState.value.currentTrack ?: return
        viewModelScope.launch {
            val playlistId = musicRepository.createPlaylist(name)
            musicRepository.addTrackToPlaylist(playlistId, track.id)
        }
    }

    fun toggleQueue() {
        _uiState.update { it.copy(showQueue = !it.showQueue) }
    }

    fun playQueueIndex(index: Int) {
        playerHolder.currentPlayer.seekTo(index, 0)
        _uiState.update { it.copy(showQueue = false) }
    }

    override fun onCleared() {
        lastPlayer?.removeListener(playerListener)
        progressJob?.cancel()
        lyricsJob?.cancel()
        super.onCleared()
    }
}
