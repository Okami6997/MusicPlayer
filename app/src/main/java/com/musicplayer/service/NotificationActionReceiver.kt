package com.musicplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var playerHolder: PlayerHolder

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == null) {
            Timber.w("NotificationActionReceiver received null action")
            return
        }

        val player = playerHolder.currentPlayer
        when (action) {
            MediaStyleNotificationProvider.ACTION_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
            }
            MediaStyleNotificationProvider.ACTION_PLAY -> player.play()
            MediaStyleNotificationProvider.ACTION_PREVIOUS -> player.seekToPrevious()
            MediaStyleNotificationProvider.ACTION_NEXT -> player.seekToNext()
            MediaStyleNotificationProvider.ACTION_STOP -> {
                playerHolder.saveCurrentPosition()
                player.stop()
            }
            MediaStyleNotificationProvider.ACTION_DISMISS -> {
                playerHolder.saveCurrentPosition()
                player.stop()
            }
            else -> Timber.w("Unknown notification action: $action")
        }
    }
}
