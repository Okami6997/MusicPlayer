package com.musicplayer.service

import android.os.CountDownTimer
import timber.log.Timber

private const val DEFAULT_SLEEP_TIMER_MS = 30L * 60L * 1000L

private var sleepTimer: CountDownTimer? = null
private var isSleepTimerActive = false

internal fun MusicPlaybackService.toggleSleepTimer() {
    if (isSleepTimerActive) {
        stopSleepTimer()
    } else {
        startSleepTimer(DEFAULT_SLEEP_TIMER_MS)
    }
}

internal fun MusicPlaybackService.startSleepTimer(durationMs: Long = DEFAULT_SLEEP_TIMER_MS) {
    sleepTimer?.cancel()
    sleepTimer = object : CountDownTimer(durationMs, 1000L) {
        override fun onTick(millisUntilFinished: Long) = Unit

        override fun onFinish() {
            isSleepTimerActive = false
            sleepTimer = null
            Timber.d("Sleep timer finished; stopping playback service")
            stopService()
        }
    }

    sleepTimer?.start()
    isSleepTimerActive = true
    Timber.d("Sleep timer started for ${durationMs}ms")
}

internal fun MusicPlaybackService.stopSleepTimer() {
    sleepTimer?.cancel()
    sleepTimer = null
    isSleepTimerActive = false
    Timber.d("Sleep timer stopped")
}
