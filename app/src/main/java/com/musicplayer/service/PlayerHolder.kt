package com.musicplayer.service

import android.content.Context
import android.net.Uri
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.cast.framework.CastContext
import com.musicplayer.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the active [Player] instance, switching between [ExoPlayer] (local/network)
 * and [CastPlayer] (Chromecast) transparently.
 */
@Singleton
class PlayerHolder @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionAvailabilityListener {

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    private val castPlayer: CastPlayer? by lazy {
        try {
            val castContext = CastContext.getSharedInstance(context)
            CastPlayer(castContext).also {
                it.setSessionAvailabilityListener(this)
            }
        } catch (e: Exception) {
            Timber.w(e, "Cast not available — disabling Chromecast support")
            null
        }
    }

    private val _currentPlayer = MutableStateFlow<Player>(exoPlayer)
    val currentPlayerFlow: StateFlow<Player> = _currentPlayer.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    var currentPlayer: Player
        get() = _currentPlayer.value
        private set(value) {
            _currentPlayer.value = value
        }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        _queue.value = tracks
        _currentIndex.value = startIndex

        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(track.artworkUri?.let { Uri.parse(it) })
                        .build()
                )
                .build()
        }
        currentPlayer.setMediaItems(mediaItems, startIndex, 0)
        currentPlayer.prepare()
        currentPlayer.play()
    }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentIndex.value = exoPlayer.currentMediaItemIndex
            }
        })
    }

    override fun onCastSessionAvailable() {
        Timber.d("Cast session available — switching to CastPlayer")
        val player = castPlayer ?: return
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentIndex.value = player.currentMediaItemIndex
            }
        })
        switchToPlayer(player)
    }

    override fun onCastSessionUnavailable() {
        Timber.d("Cast session unavailable — switching to ExoPlayer")
        switchToPlayer(exoPlayer)
    }

    private fun switchToPlayer(newPlayer: Player) {
        if (newPlayer == currentPlayer) return
        val playWhenReady = currentPlayer.playWhenReady
        val currentMediaItemIndex = currentPlayer.currentMediaItemIndex
        val currentPosition = currentPlayer.currentPosition

        val mediaItems = mutableListOf<MediaItem>()
        for (i in 0 until currentPlayer.mediaItemCount) {
            mediaItems.add(currentPlayer.getMediaItemAt(i))
        }

        currentPlayer.stop()
        currentPlayer.clearMediaItems()

        newPlayer.setMediaItems(mediaItems)
        newPlayer.playWhenReady = playWhenReady
        newPlayer.prepare()
        newPlayer.seekTo(currentMediaItemIndex, currentPosition)

        currentPlayer = newPlayer
        Timber.d("Switched to ${if (newPlayer is CastPlayer) "CastPlayer" else "ExoPlayer"}")
    }

    fun release() {
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.release()
        exoPlayer.release()
    }
}
