package com.musicplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentTracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerHolder: PlayerHolder
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = MutableStateFlow(HomeUiState()).also { state ->
        viewModelScope.launch {
            repository.getAllTracks().collect { tracks ->
                state.value = state.value.copy(
                    recentTracks = tracks.takeLast(20).reversed(),
                    isLoading = false
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(isLoading = true))

    fun playTrack(track: Track) {
        val tracks = uiState.value.recentTracks
        val index = tracks.indexOf(track).coerceAtLeast(0)
        playerHolder.playTracks(tracks, index)
    }

    fun refreshLocalLibrary() {
        viewModelScope.launch {
            try {
                repository.scanLocalLibrary()
            } catch (e: Exception) {
                // Scanning errors are non-fatal; permissions may not be granted yet
            }
        }
    }
}
