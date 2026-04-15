package com.musicplayer.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val dynamicColorEnabled: Boolean = true,
    val gaplessPlayback: Boolean = true,
    val crossfadeDurationMs: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
    }

    val uiState: StateFlow<SettingsUiState> = dataStore.data
        .map { prefs ->
            SettingsUiState(
                dynamicColorEnabled = prefs[KEY_DYNAMIC_COLOR] ?: true,
                gaplessPlayback = prefs[KEY_GAPLESS_PLAYBACK] ?: true
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
        }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_GAPLESS_PLAYBACK] = enabled }
        }
    }
}
