package com.musicplayer.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.collect.ImmutableList
import com.musicplayer.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom [MediaNotification.Provider] that creates a [MediaStyle] player card notification
 * (like YouTube Music / Amazon Music) that updates dynamically as playback state changes.
 */
@Singleton
class MediaStyleNotificationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerHolder: PlayerHolder,
    private val imageLoader: ImageLoader,
) : MediaNotification.Provider {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentCallback: MediaNotification.Provider.Callback? = null
    private var currentSession: MediaSession? = null

    /** Player listener that triggers notification refresh on relevant state changes. */
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = scheduleNotificationUpdate()
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) = scheduleNotificationUpdate()
        override fun onPlaybackStateChanged(playbackState: Int) = scheduleNotificationUpdate()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "music_playback_v4"
        const val ACTION_PAUSE = "com.musicplayer.ACTION_PAUSE"
        const val ACTION_PLAY = "com.musicplayer.ACTION_PLAY"
        const val ACTION_PREVIOUS = "com.musicplayer.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.musicplayer.ACTION_NEXT"
        const val ACTION_STOP = "com.musicplayer.ACTION_STOP"
        const val ACTION_DISMISS = "com.musicplayer.ACTION_DISMISS"
        private const val MAX_ARTWORK_SIZE = 512
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        // Store callback and session; register listener if session changed
        currentCallback = onNotificationChangedCallback
        if (currentSession != mediaSession) {
            currentSession?.player?.removeListener(playerListener)
            currentSession = mediaSession
            mediaSession.player.addListener(playerListener)
        }

        val notification = buildNotification(mediaSession)
        return MediaNotification(NOTIFICATION_ID, notification)
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle,
    ): Boolean = false

    private fun scheduleNotificationUpdate() {
        val session = currentSession ?: return
        val callback = currentCallback ?: return
        scope.launch(Dispatchers.Main) {
            val notification = buildNotification(session)
            callback.onNotificationChanged(MediaNotification(NOTIFICATION_ID, notification))
        }
    }

    private fun buildNotification(mediaSession: MediaSession): android.app.Notification {
        val player = mediaSession.player
        val metadata = player.currentMediaItem?.mediaMetadata

        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val dismissIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_DISMISS).apply { setPackage(context.packageName) },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isPlaying = player.isPlaying
        val pausePlayIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val pausePlayAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val pausePlayLabel = if (isPlaying) "Pause" else "Play"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(metadata?.title ?: "Music")
            .setContentText(metadata?.artist ?: "")
            .setSubText(metadata?.albumTitle)
            .setContentIntent(contentIntent)
            .setDeleteIntent(dismissIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(android.R.drawable.ic_media_previous, "Previous", createActionIntent(ACTION_PREVIOUS))
            .addAction(pausePlayIcon, pausePlayLabel, createActionIntent(pausePlayAction))
            .addAction(android.R.drawable.ic_media_next, "Next", createActionIntent(ACTION_NEXT))

        // Load artwork synchronously (this runs on IO thread from scheduleNotificationUpdate)
        metadata?.artworkUri?.let { uri ->
            loadArtwork(uri.toString())?.let { builder.setLargeIcon(it) }
        }

        return builder.build()
    }

    private fun loadArtwork(uri: String): Bitmap? {
        return try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .size(MAX_ARTWORK_SIZE)
                .build()
            // execute() is a suspend function; call it on IO dispatcher via runBlocking
            val result = kotlinx.coroutines.runBlocking { imageLoader.execute(request) }
            if (result is SuccessResult) (result.drawable as? BitmapDrawable)?.bitmap else null
        } catch (e: Exception) {
            Timber.w(e, "Failed to load artwork for notification")
            null
        }
    }

    private fun createActionIntent(action: String): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(action).apply { setPackage(context.packageName) },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

