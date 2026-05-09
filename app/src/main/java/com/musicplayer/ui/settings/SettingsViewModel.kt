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

data class SettingsUiState(
    val dynamicColorEnabled: Boolean = true,
    val themeMode: String = "system",
    val gaplessPlayback: Boolean = true,
    val crossfadeDurationMs: Int = 0,
    val useNewUi: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    // Keep for backward compatibility if needed, but using SettingsKeys preferred
    companion object {
        val KEY_DYNAMIC_COLOR = SettingsKeys.DYNAMIC_COLOR
        val KEY_THEME_MODE = SettingsKeys.THEME_MODE
        val KEY_GAPLESS_PLAYBACK = SettingsKeys.GAPLESS_PLAYBACK
    }

    val uiState: StateFlow<SettingsUiState> = dataStore.data
        .map { prefs ->
            SettingsUiState(
                dynamicColorEnabled = prefs[SettingsKeys.DYNAMIC_COLOR] ?: true,
                themeMode = prefs[SettingsKeys.THEME_MODE] ?: "system",
                gaplessPlayback = prefs[SettingsKeys.GAPLESS_PLAYBACK] ?: true,
                crossfadeDurationMs = prefs[SettingsKeys.CROSSFADE_DURATION] ?: 0,
                useNewUi = prefs[SettingsKeys.USE_NEW_UI] ?: false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[SettingsKeys.DYNAMIC_COLOR] = enabled }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            dataStore.edit { it[SettingsKeys.THEME_MODE] = mode }
        }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[SettingsKeys.GAPLESS_PLAYBACK] = enabled }
        }
    }

    fun setUseNewUi(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[SettingsKeys.USE_NEW_UI] = enabled }
        }
    }
}
