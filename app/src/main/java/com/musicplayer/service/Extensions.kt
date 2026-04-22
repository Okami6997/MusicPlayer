package com.musicplayer.service

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player

/**
 * Ensures that the given [action] is executed on the player's application thread.
 */
fun Player.runOnPlayerThread(action: Player.() -> Unit) {
    if (Looper.myLooper() == applicationLooper) {
        action()
    } else {
        Handler(applicationLooper).post { this.action() }
    }
}
