package com.musicplayer.ui

import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.musicplayer.service.PlayerHolder
import com.musicplayer.ui.settings.SettingsKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val themeMode: String = "system",
    val dynamicColorEnabled: Boolean = true,
    val currentArtworkUri: String? = null
)

@HiltViewModel
class MainViewModel @OptIn(UnstableApi::class) @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val playerHolder: PlayerHolder
) : ViewModel() {

    private val _currentArtworkUri = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        dataStore.data,
        _currentArtworkUri
    ) { prefs, artworkUri ->
        MainUiState(
            themeMode = prefs[SettingsKeys.THEME_MODE] ?: "system",
            dynamicColorEnabled = prefs[SettingsKeys.DYNAMIC_COLOR] ?: true,
            currentArtworkUri = artworkUri
        )
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            playerHolder.currentPlayerFlow.collect { player ->
                // Initial artwork
                _currentArtworkUri.value = player.currentMediaItem?.mediaMetadata?.artworkUri?.toString()
                
                // Observe changes
                val listener = object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _currentArtworkUri.value = mediaItem?.mediaMetadata?.artworkUri?.toString()
                    }
                    
                    override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                        _currentArtworkUri.value = metadata.artworkUri?.toString()
                    }
                }
                player.addListener(listener)
            }
        }
    }
}
