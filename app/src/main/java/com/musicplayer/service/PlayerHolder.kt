package com.musicplayer.service

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.cast.framework.CastContext
import dagger.hilt.android.qualifiers.ApplicationContext
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

    var currentPlayer: Player = exoPlayer
        private set

    override fun onCastSessionAvailable() {
        Timber.d("Cast session available — switching to CastPlayer")
        switchToPlayer(castPlayer ?: return)
    }

    override fun onCastSessionUnavailable() {
        Timber.d("Cast session unavailable — switching to ExoPlayer")
        switchToPlayer(exoPlayer)
    }

    private fun switchToPlayer(newPlayer: Player) {
        if (newPlayer == currentPlayer) return
        val playbackState = currentPlayer.playbackState
        val playWhenReady = currentPlayer.playWhenReady
        val currentMediaItemIndex = currentPlayer.currentMediaItemIndex
        val currentPosition = currentPlayer.currentPosition

        currentPlayer.stop()
        currentPlayer.clearMediaItems()

        newPlayer.playWhenReady = playWhenReady
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
