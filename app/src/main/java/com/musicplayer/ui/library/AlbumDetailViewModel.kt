package com.musicplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.DownloadRepository
import com.musicplayer.domain.model.Album
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true
)

@androidx.media3.common.util.UnstableApi
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerHolder: PlayerHolder,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            repository.getAllTracks().collect { allTracks ->
                val albumTracks = allTracks.filter { it.albumId == albumId || it.album == albumId }
                if (albumTracks.isNotEmpty()) {
                    val first = albumTracks.first()
                    val album = Album(
                        id = first.albumId.ifEmpty { first.album },
                        title = first.album,
                        artist = first.albumArtist.ifEmpty { first.artist },
                        artworkUri = first.artworkUri,
                        trackCount = albumTracks.size,
                        sourceId = first.sourceId,
                        sourceName = first.sourceName,
                        sourceType = first.sourceType
                    )
                    _uiState.value = AlbumDetailUiState(
                        album = album,
                        tracks = albumTracks.sortedBy { it.trackNumber },
                        isLoading = false
                    )
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun playTrack(track: Track) {
        val tracks = _uiState.value.tracks
        val index = tracks.indexOf(track).coerceAtLeast(0)
        playerHolder.playTracks(tracks, index)
    }

    fun playAlbum() {
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
}
