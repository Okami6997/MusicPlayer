package com.musicplayer.auto

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import dagger.hilt.android.AndroidEntryPoint

/**
 * Android Auto CarAppService for Music Player.
 * Exposes the music library as a browsable media app in the car head unit.
 */
@AndroidEntryPoint
class MusicCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = MusicCarSession()
}
