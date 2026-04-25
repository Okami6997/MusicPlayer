package com.musicplayer.ui.library

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.DownloadRepository
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistDetailUiState(
    val artistName: String? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true
)

@androidx.media3.common.util.UnstableApi
@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerHolder: PlayerHolder,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    fun loadArtist(artistName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            repository.getAllTracks().collect { allTracks ->
                val artistTracks = allTracks.filter { it.artist == artistName }
                _uiState.value = ArtistDetailUiState(
                    artistName = artistName,
                    tracks = artistTracks.sortedBy { it.album },
                    isLoading = false
                )
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun playTrack(track: Track) {
        val tracks = _uiState.value.tracks
        val index = tracks.indexOf(track).coerceAtLeast(0)
        playerHolder.playTracks(tracks, index)
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
        }
    }
}
