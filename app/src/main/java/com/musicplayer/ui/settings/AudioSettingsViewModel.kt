package com.musicplayer.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
        val KEY_GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val KEY_EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val KEY_CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
    }

    val uiState: StateFlow<AudioSettingsUiState> = dataStore.data
        .map { prefs ->
            AudioSettingsUiState(
                gaplessPlayback = prefs[KEY_GAPLESS_PLAYBACK] ?: true,
                equalizerEnabled = prefs[KEY_EQUALIZER_ENABLED] ?: false,
                crossfadeDurationMs = prefs[KEY_CROSSFADE_DURATION] ?: 0
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AudioSettingsUiState())

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_GAPLESS_PLAYBACK] = enabled }
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_EQUALIZER_ENABLED] = enabled }
        }
    }

    fun setCrossfadeDuration(durationMs: Int) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_CROSSFADE_DURATION] = durationMs }
        }
    }
}
