package com.musicplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.QueueRepository
import com.musicplayer.domain.model.Track
import com.musicplayer.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

private fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri?.let { Uri.parse(it) })
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .build()

/**
 * Custom session commands exposed to media controllers and notification actions.
 */
enum class CustomCommands(val customAction: String) {
    TOGGLE_SLEEP_TIMER("com.musicplayer.ACTION_TOGGLE_SLEEP_TIMER"),
    CLOSE_PLAYER("com.musicplayer.ACTION_CLOSE_PLAYER");

    val sessionCommand: SessionCommand
        get() = SessionCommand(customAction, Bundle.EMPTY)

    companion object {
        private val values = entries
        fun fromSessionCommand(command: SessionCommand): CustomCommands? =
            values.find { it.customAction == command.customAction }
    }
}

/**
 * Long-running foreground service that owns the [MediaLibrarySession].
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicPlaybackService : MediaLibraryService() {

    @Inject
    lateinit var playerHolder: PlayerHolder

    @Inject
    lateinit var queueRepository: QueueRepository

    @Inject
    lateinit var musicRepository: MusicRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationProvider: DefaultMediaNotificationProvider
    private var mediaSession: MediaLibraryService.MediaLibrarySession? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "music_playback_channel"
        private const val ROOT_ID = "[root]"
        private const val TRACKS_ID = "[tracks]"
        private const val PLAYLISTS_ID = "[playlists]"

        var isPlaying: Boolean = false
            private set
        var currentMediaItem: MediaItem? = null
            private set

        fun updatePlaybackInfo(player: Player) {
            currentMediaItem = player.currentMediaItem
            isPlaying = player.isPlaying || player.playWhenReady
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(Intent.ACTION_VIEW, Uri.parse("musicplayer://player")).apply {
                setClass(this@MusicPlaybackService, MainActivity::class.java)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setNotificationId(NOTIFICATION_ID)
            .build()
        setMediaNotificationProvider(notificationProvider)

        showBootstrapNotification(sessionActivityPendingIntent)

        mediaSession = MediaLibraryService.MediaLibrarySession.Builder(this, playerHolder.currentPlayer, SessionCallback())
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
        mediaSession?.let { addSession(it) }

        // Update custom layout with action buttons
        mediaSession?.setCustomLayout(getCustomLayout())

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

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(com.musicplayer.R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun showBootstrapNotification(pendingIntent: PendingIntent) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(com.musicplayer.R.string.app_name))
            .setContentText("Playback service is ready")
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Timber.w(e, "Unable to start foreground notification during service creation")
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.d("onTaskRemoved called — rootIntent=$rootIntent")
        withPlayer {
            if (isPlaying) {
                Timber.d("Media is playing — keeping service alive")
            } else {
                Timber.d("Media not playing — stopping self")
                stopSelf()
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Timber.d("MusicPlaybackService onDestroy")
        playerHolder.saveCurrentPosition()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        playerHolder.release()
        serviceScope.cancel()
        super.onDestroy()
        Timber.d("MusicPlaybackService destroyed")
    }

    internal fun withPlayer(callback: Player.() -> Unit) {
        playerHolder.currentPlayer.runOnPlayerThread { callback(this) }
    }

    fun stopService() {
        withPlayer {
            pause()
            stop()
        }
        stopSelf()
    }

    private fun toggleSleepTimer() {
        // TODO: Implement sleep timer logic
        Timber.i("Sleep timer toggled (not yet implemented)")
    }

    private inner class SessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val result = super.onConnect(session, controller)
            val sessionCommands = result.availableSessionCommands.buildUpon()
            CustomCommands.entries.forEach { sessionCommands.add(it.sessionCommand) }

            // Ensure player commands like Shuffle and Repeat are available to the controller/notification
            val playerCommands = result.availablePlayerCommands.buildUpon()
                .add(Player.COMMAND_SET_SHUFFLE_MODE)
                .add(Player.COMMAND_SET_REPEAT_MODE)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands.build())
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            val customLayout = getCustomLayout()
            if (customLayout.isNotEmpty() && controller.controllerVersion != 0) {
                mediaSession?.setCustomLayout(controller, customLayout)
            }
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            val command = CustomCommands.fromSessionCommand(customCommand)
            when (command) {
                CustomCommands.TOGGLE_SLEEP_TIMER -> toggleSleepTimer()
                CustomCommands.CLOSE_PLAYER -> stopService()
                null -> { /* unknown command */ }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibraryService.MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootMetadata = MediaMetadata.Builder()
                .setTitle("Music Library")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(rootMetadata)
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, null))
        }

        override fun onGetChildren(
            session: MediaLibraryService.MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                val items = when (parentId) {
                    ROOT_ID -> listOf(
                        createBrowsableItem(TRACKS_ID, "All Tracks"),
                        createBrowsableItem(PLAYLISTS_ID, "Playlists")
                    )
                    TRACKS_ID -> musicRepository.getAllTracks().first().map { it.toMediaItem() }
                    else -> emptyList()
                }
                future.set(LibraryResult.ofItemList(items, null))
            }
            return future
        }

        private fun createBrowsableItem(id: String, title: String): MediaItem {
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            if (controller.packageName == packageName) {
                return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
            }

            val firstItem = mediaItems.firstOrNull()
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()

            serviceScope.launch {
                val allTracks = musicRepository.getAllTracks().first()
                val expandedItems = allTracks.map { it.toMediaItem() }
                
                val newIndex = if (firstItem != null) {
                    expandedItems.indexOfFirst { it.mediaId == firstItem.mediaId }.coerceAtLeast(0)
                } else {
                    startIndex
                }

                future.set(MediaSession.MediaItemsWithStartPosition(expandedItems, newIndex, startPositionMs))
                
                withContext(Dispatchers.Main) {
                    playerHolder.restoreQueue(allTracks, newIndex)
                }
            }

            return future
        }

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
                        val mediaItems = state.tracks.map { it.toMediaItem() }
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

    private fun getCustomLayout(): ImmutableList<CommandButton> {
        return ImmutableList.of(
            CommandButton.Builder()
                .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE)
                .setIconResId(android.R.drawable.ic_menu_directions) // Placeholder icon
                .setDisplayName("Shuffle")
                .build(),
            CommandButton.Builder()
                .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE)
                .setIconResId(android.R.drawable.ic_menu_revert) // Placeholder icon
                .setDisplayName("Repeat")
                .build(),
            CommandButton.Builder()
                .setSessionCommand(CustomCommands.TOGGLE_SLEEP_TIMER.sessionCommand)
                .setIconResId(android.R.drawable.ic_menu_agenda)
                .setDisplayName("Sleep Timer")
                .build(),
            CommandButton.Builder()
                .setSessionCommand(CustomCommands.CLOSE_PLAYER.sessionCommand)
                .setIconResId(android.R.drawable.ic_menu_close_clear_cancel)
                .setDisplayName("Close")
                .build()
        )
    }
}
