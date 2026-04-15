package com.musicplayer.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.musicplayer.domain.model.PlayerState
import com.musicplayer.domain.model.PlayerUiState
import com.musicplayer.domain.model.RepeatMode
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerHolder: PlayerHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState()
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentTrack()
        }
    }

    init {
        playerHolder.currentPlayer.addListener(playerListener)
        startProgressUpdates()
    }

    private fun updatePlaybackState() {
        val player = playerHolder.currentPlayer
        val state = when {
            player.playbackState == Player.STATE_BUFFERING -> PlayerState.LOADING
            player.isPlaying -> PlayerState.PLAYING
            player.playbackState == Player.STATE_ENDED -> PlayerState.STOPPED
            player.playWhenReady -> PlayerState.PAUSED
            else -> PlayerState.PAUSED
        }
        _uiState.update { it.copy(playerState = state) }
    }

    private fun updateCurrentTrack() {
        val player = playerHolder.currentPlayer
        val mediaItem = player.currentMediaItem ?: return
        // Map back to Track from extras — in a real app you'd look up the track by id
        _uiState.update { current ->
            current.copy(
                durationMs = player.duration.takeIf { it > 0 } ?: 0L
            )
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val player = playerHolder.currentPlayer
                _uiState.update { it.copy(currentPositionMs = player.currentPosition) }
                delay(500)
            }
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        val player = playerHolder.currentPlayer
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(track.artworkUri?.let { android.net.Uri.parse(it) })
                        .build()
                )
                .build()
        }
        player.setMediaItems(mediaItems, startIndex, 0)
        player.prepare()
        player.play()
        _uiState.update { it.copy(queue = tracks, currentQueueIndex = startIndex) }
    }

    fun togglePlayPause() {
        val player = playerHolder.currentPlayer
        if (player.isPlaying) player.pause() else player.play()
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

    override fun onCleared() {
        playerHolder.currentPlayer.removeListener(playerListener)
        progressJob?.cancel()
        super.onCleared()
    }
}
