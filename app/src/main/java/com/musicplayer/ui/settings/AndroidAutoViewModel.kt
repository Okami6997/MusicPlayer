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

data class AndroidAutoUiState(
    val androidAutoEnabled: Boolean = true,
    val autoPlay: Boolean = true,
    val shuffleMode: Boolean = false
)

@HiltViewModel
class AndroidAutoViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val uiState: StateFlow<AndroidAutoUiState> = dataStore.data
        .map { prefs ->
            AndroidAutoUiState(
                androidAutoEnabled = prefs[SettingsKeys.ANDROID_AUTO_ENABLED] ?: true,
                autoPlay = prefs[SettingsKeys.ANDROID_AUTO_AUTO_PLAY] ?: true,
                shuffleMode = prefs[SettingsKeys.ANDROID_AUTO_SHUFFLE_MODE] ?: false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AndroidAutoUiState())

    fun setAndroidAutoEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[SettingsKeys.ANDROID_AUTO_ENABLED] = enabled }
        }
    }

    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[SettingsKeys.ANDROID_AUTO_AUTO_PLAY] = enabled }
        }
    }

    fun setShuffleMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[SettingsKeys.ANDROID_AUTO_SHUFFLE_MODE] = enabled }
        }
    }
}
