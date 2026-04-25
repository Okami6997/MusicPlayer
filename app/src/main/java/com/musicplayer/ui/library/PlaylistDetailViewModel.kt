package com.musicplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.DownloadRepository
import com.musicplayer.domain.model.Playlist
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true
)

@androidx.media3.common.util.UnstableApi
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerHolder: PlayerHolder,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    fun loadPlaylist(playlistId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Get playlist info from the list of all playlists
            repository.getAllPlaylists().collect { playlists ->
                val playlist = playlists.find { it.id == playlistId }
                
                // Get tracks for this playlist
                repository.getPlaylistTracks(playlistId).collect { tracks ->
                    _uiState.value = PlaylistDetailUiState(
                        playlist = playlist,
                        tracks = tracks,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun playTrack(track: Track) {
        val tracks = _uiState.value.tracks
        val index = tracks.indexOf(track).coerceAtLeast(0)
        playerHolder.playTracks(tracks, index)
    }

    fun playPlaylist() {
        val tracks = _uiState.value.tracks
        if (tracks.isNotEmpty()) {
            playerHolder.playTracks(tracks, 0)
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
        }
    }

    fun removeTrackFromPlaylist(track: Track) {
        val playlistId = _uiState.value.playlist?.id ?: return
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, track.id)
        }
    }
    
    fun deletePlaylist() {
        val playlistId = _uiState.value.playlist?.id ?: return
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun getAllPlaylists() = repository.getAllPlaylists()

    fun addTrackToPlaylist(track: Track, playlistId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track.id)
        }
    }

    fun createPlaylistAndAddTrack(name: String, track: Track) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(name)
            repository.addTrackToPlaylist(playlistId, track.id)
        }
    }
}
