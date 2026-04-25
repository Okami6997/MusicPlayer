package com.musicplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.DownloadRepository
import com.musicplayer.domain.model.Album
import com.musicplayer.domain.model.Artist
import com.musicplayer.domain.model.Playlist
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerHolder: PlayerHolder,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = repository.getAllTracks()
        .combine(repository.getAllPlaylists()) { tracks, playlists ->
            val albums = tracks
                .groupBy { it.albumId.ifEmpty { it.album } }
                .map { (_, albumTracks) ->
                    val first = albumTracks.first()
                    Album(
                        id = first.albumId.ifEmpty { first.album },
                        title = first.album,
                        artist = first.albumArtist.ifEmpty { first.artist },
                        artworkUri = first.artworkUri,
                        trackCount = albumTracks.size,
                        sourceId = first.sourceId,
                        sourceName = first.sourceName,
                        sourceType = first.sourceType
                    )
                }.sortedBy { it.title }

            val artists = tracks
                .groupBy { it.artist }
                .map { (artistName, artistTracks) ->
                    val albumCount = artistTracks.distinctBy { it.albumId }.size
                    Artist(
                        id = artistName,
                        name = artistName,
                        albumCount = albumCount,
                        trackCount = artistTracks.size,
                        sourceId = artistTracks.first().sourceId,
                        sourceType = artistTracks.first().sourceType
                    )
                }.sortedBy { it.name }

            LibraryUiState(
                tracks = tracks,
                albums = albums,
                artists = artists,
                playlists = playlists,
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun playTrack(track: Track) {
        val tracks = uiState.value.tracks
        val index = tracks.indexOf(track).coerceAtLeast(0)
        playerHolder.playTracks(tracks, index)
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
        }
    }

    fun downloadAlbum(album: Album) {
        viewModelScope.launch {
            downloadRepository.downloadAlbum(album.id)
        }
    }

    fun scanLocalLibrary() {
        viewModelScope.launch {
            try {
                repository.scanLocalLibrary()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun addTrackToPlaylist(track: Track, playlistId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track.id)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun createPlaylistAndAddTrack(name: String, track: Track) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(name)
            repository.addTrackToPlaylist(playlistId, track.id)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun playPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            // Need to fetch tracks for this playlist
            repository.getPlaylistTracks(playlist.id).firstOrNull()?.let { tracks ->
                if (tracks.isNotEmpty()) {
                    playerHolder.playTracks(tracks, 0)
                }
            }
        }
    }
}
