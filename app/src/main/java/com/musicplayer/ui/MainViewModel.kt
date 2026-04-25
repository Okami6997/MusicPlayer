package com.musicplayer.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.ui.settings.SettingsKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainUiState(
    val themeMode: String = "system",
    val dynamicColorEnabled: Boolean = true
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = dataStore.data
        .map { prefs ->
            MainUiState(
                themeMode = prefs[SettingsKeys.THEME_MODE] ?: "system",
                dynamicColorEnabled = prefs[SettingsKeys.DYNAMIC_COLOR] ?: true
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())
}
