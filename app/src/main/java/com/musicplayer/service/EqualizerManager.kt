package com.musicplayer.service

import android.media.audiofx.Equalizer
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.musicplayer.ui.settings.SettingsKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private var equalizer: Equalizer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun onAudioSessionIdChanged(audioSessionId: Int) {
        if (audioSessionId == 0) return
        
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = false
            }
            
            // Start observing settings when we have a session
            observeSettings()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Equalizer")
        }
    }

    private var settingsJob: kotlinx.coroutines.Job? = null

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = scope.launch {
            dataStore.data.collect { applySettings() }
        }
    }

    fun applySettings() {
        val eq = equalizer ?: return
        scope.launch {
            val prefs = dataStore.data.first()
            val isEnabled = prefs[SettingsKeys.EQUALIZER_ENABLED] ?: false
            val bandsStr = prefs[SettingsKeys.EQUALIZER_BANDS] ?: ""
            
            try {
                eq.enabled = isEnabled
                if (isEnabled && bandsStr.isNotEmpty()) {
                    val bandLevels = bandsStr.split(",").associate {
                        val parts = it.split(":")
                        parts[0].toInt() to parts[1].toShort()
                    }
                    
                    for (i in 0 until eq.numberOfBands) {
                        bandLevels[i.toInt()]?.let { level ->
                            eq.setBandLevel(i.toShort(), level)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error applying equalizer settings")
            }
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }
}
