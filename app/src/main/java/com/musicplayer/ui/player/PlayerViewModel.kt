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
    private val downloadRepository: DownloadRepository
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
        val track = if (mediaItem != null) {
            queue.find { it.id == mediaItem.mediaId }
        } else null

        _uiState.update { current ->
            current.copy(
                currentTrack = track,
                queue = queue,
                currentQueueIndex = player.currentMediaItemIndex,
                durationMs = player.duration.takeIf { it > 0 } ?: 0L
            )
        }

        if (track != null && track.id != lastLoadedTrackId) {
            lastLoadedTrackId = track.id
            loadLyrics(track)
        } else if (track == null) {
            lastLoadedTrackId = null
            _uiState.update { it.copy(lyrics = null, currentLyricsLineIndex = -1) }
        }
    }

    private fun loadLyrics(track: Track) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _uiState.update { it.copy(lyrics = null, currentLyricsLineIndex = -1) }
            val lyrics = lyricsLoader.loadLyrics(track)
            _uiState.update { it.copy(lyrics = lyrics, currentLyricsLineIndex = -1) }
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
                _uiState.update {
                    it.copy(currentPositionMs = posMs, currentLyricsLineIndex = lineIndex)
                }
                delay(500)
            }
        }
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

    override fun onCleared() {
        lastPlayer?.removeListener(playerListener)
        progressJob?.cancel()
        lyricsJob?.cancel()
        super.onCleared()
    }
}
