package com.musicplayer.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.musicplayer.data.repository.QueueRepository
import com.musicplayer.domain.model.Track
import com.musicplayer.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

private fun Track.toResumptionMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri?.let { Uri.parse(it) })
                .build()
        )
        .build()

/**
 * Long-running foreground service that owns the [MediaSession].
 *
 * Manages ExoPlayer for local/network playback and transparently switches to
 * CastPlayer when a Chromecast session becomes available.
 */
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject
    lateinit var playerHolder: PlayerHolder

    @Inject
    lateinit var queueRepository: QueueRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, playerHolder.currentPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(SessionCallback())
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .build()
        )

        serviceScope.launch {
            playerHolder.currentPlayerFlow.collect { newPlayer ->
                withContext(Dispatchers.Main) {
                    Timber.d("Switching MediaSession player to ${if (newPlayer is androidx.media3.cast.CastPlayer) "CastPlayer" else "ExoPlayer"}")
                    mediaSession?.player = newPlayer
                }
            }
        }

        Timber.d("MusicPlaybackService created")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.d("onTaskRemoved called — rootIntent=$rootIntent")
        // Keep service alive if media is playing
        val player = mediaSession?.player
        if (player != null && player.isPlaying) {
            Timber.d("Media is playing — keeping service alive")
        } else {
            Timber.d("Media not playing — stopping self")
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Timber.d("MusicPlaybackService onDestroy")
        playerHolder.saveCurrentPosition()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        playerHolder.release()
        serviceScope.cancel()
        super.onDestroy()
        Timber.d("MusicPlaybackService destroyed")
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                try {
                    val state = queueRepository.loadQueueState()
                    if (state != null && state.tracks.isNotEmpty()) {
                        playerHolder.restoreQueue(state.tracks, state.currentIndex)
                        val mediaItems = state.tracks.map { it.toResumptionMediaItem() }
                        future.set(
                            MediaSession.MediaItemsWithStartPosition(
                                mediaItems,
                                state.currentIndex,
                                state.currentPositionMs,
                            )
                        )
                        Timber.d("Resuming queue: ${state.tracks.size} tracks, index=${state.currentIndex}")
                    } else {
                        future.setException(UnsupportedOperationException("No saved queue for resumption"))
                    }
                } catch (e: Exception) {
                    future.setException(e)
                }
            }
            return future
        }
    }
}
