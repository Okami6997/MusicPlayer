package com.musicplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.ProfileMusicRepository
import com.musicplayer.data.repository.DownloadRepository
import com.musicplayer.domain.model.Track
import com.musicplayer.profile.ProfileManager
import com.musicplayer.service.PlayerHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentTracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val profileMusicRepository: ProfileMusicRepository,
    private val profileManager: ProfileManager,
    private val playerHolder: PlayerHolder,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    // Use profile-based tracks when a profile is selected, otherwise fall back to old track table
    private val tracksFlow = profileManager.selectedProfile.flatMapLatest { profile ->
        if (profile != null) {
            profileMusicRepository.getTracksForCurrentProfile()
        } else {
            repository.getAllTracks()
        }
    }

    val uiState: StateFlow<HomeUiState> = tracksFlow
        .map { tracks ->
            HomeUiState(
                recentTracks = tracks.takeLast(20).reversed(),
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(isLoading = true))

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

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
        }
    }
}