package com.musicplayer.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AudioSettingsUiState(
    val gaplessPlayback: Boolean = true,
    val equalizerEnabled: Boolean = false,
    val crossfadeDurationMs: Int = 0
)

@HiltViewModel
class AudioSettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val KEY_GAPLESS_PLAYBACK = SettingsKeys.GAPLESS_PLAYBACK
        val KEY_CROSSFADE_DURATION = SettingsKeys.CROSSFADE_DURATION
    }

    val uiState: StateFlow<AudioSettingsUiState> = dataStore.data
        .map { prefs ->
            AudioSettingsUiState(
                gaplessPlayback = prefs[SettingsKeys.GAPLESS_PLAYBACK] ?: true,
                equalizerEnabled = prefs[SettingsKeys.EQUALIZER_ENABLED] ?: false,
                crossfadeDurationMs = prefs[SettingsKeys.CROSSFADE_DURATION] ?: 0
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AudioSettingsUiState())

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[SettingsKeys.GAPLESS_PLAYBACK] = enabled }
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[SettingsKeys.EQUALIZER_ENABLED] = enabled }
        }
    }

    fun setCrossfadeDuration(durationMs: Int) {
        viewModelScope.launch {
            dataStore.edit { it[SettingsKeys.CROSSFADE_DURATION] = durationMs }
        }
    }
}
