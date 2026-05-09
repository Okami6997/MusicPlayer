package com.musicplayer.ui.download

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.remote.download.*
import com.musicplayer.profile.ProfileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject

data class DownloadMusicUiState(
    val isLoading: Boolean = false,
    val tracks: List<DownloadTrack> = emptyList(),
    val albums: List<AlbumResult> = emptyList(),
    val artists: List<ArtistResult> = emptyList(),
    val error: String? = null,
    val downloadStatus: String? = null,
    val isTestingUrl: Boolean = false,
    val testUrlResult: String? = null,
    val selectedFilters: Set<String> = emptySet()
)

@HiltViewModel
class DownloadMusicViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val downloadClient: DownloadClient,
    private val okHttpClient: OkHttpClient,
    private val profileManager: ProfileManager
) : ViewModel() {

    companion object {
        val KEY_DOWNLOAD_SOURCE_URL = stringPreferencesKey("download_source_url")

        // Available filters
        val AVAILABLE_FILTERS = listOf(
            "Spotify", "YouTube", "SoundCloud", "Apple Music", "Amazon", "Tidal", "Deezer"
        )
    }

    private val _isTesting = MutableStateFlow(false)
    private val _testResult = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(DownloadMusicUiState())
    val uiState: StateFlow<DownloadMusicUiState> = combine(
        _uiState,
        _isTesting,
        _testResult
    ) { state, isTesting, testResult ->
        state.copy(
            isTestingUrl = isTesting,
            testUrlResult = testResult
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadMusicUiState())

    val downloadSourceUrl: StateFlow<String> = combine(
        dataStore.data.map { prefs -> prefs[KEY_DOWNLOAD_SOURCE_URL] ?: "" },
        profileManager.selectedProfile
    ) { manualUrl, profile ->
        // Use profile's dedicated download URL (separate port from music source), otherwise fall back to manual URL
        profile?.downloadUrl ?: manualUrl
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val selectedProfile = profileManager.selectedProfile

    fun setDownloadSourceUrl(url: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_DOWNLOAD_SOURCE_URL] = url }
        }
    }

    fun testUrl(url: String) {
        if (url.isBlank()) return

        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null

            try {
                val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "http://$url"
                } else {
                    url
                }

                val result = withContext(Dispatchers.IO) {
                    try {
                        val request = Request.Builder()
                            .url(formattedUrl)
                            .head()
                            .build()

                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                "Success: URL is reachable"
                            } else {
                                "Error: Server returned ${response.code}"
                            }
                        }
                    } catch (e: Exception) {
                        "Error: ${e.localizedMessage ?: "Connection failed"}"
                    }
                }
                _testResult.value = result
            } catch (e: Exception) {
                _testResult.value = "Invalid URL format"
            } finally {
                _isTesting.value = false
            }
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    fun toggleFilter(filter: String) {
        val currentFilters = _uiState.value.selectedFilters.toMutableSet()
        if (currentFilters.contains(filter)) {
            currentFilters.remove(filter)
        } else {
            currentFilters.add(filter)
        }
        _uiState.update { it.copy(selectedFilters = currentFilters) }
    }

    fun clearFilters() {
        _uiState.update { it.copy(selectedFilters = emptySet()) }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(tracks = emptyList(), albums = emptyList(), artists = emptyList(), error = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val url = downloadSourceUrl.value
            if (url.isBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Please select a profile or set download source URL in settings"
                    )
                }
                return@launch
            }

            try {
                val api = downloadClient.createApi(url)
                val response = api.search(query)

                Timber.d("Search response code: ${response.code()}")
                Timber.d("Search response successful: ${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val searchResponse = response.body()!!
                    Timber.d("Search query: ${searchResponse.query}")
                    Timber.d("Search has more: ${searchResponse.hasMore}")
                    Timber.d("Search source: ${searchResponse.source}")

                    // Combine all tracks from different sources
                    val allTracks = mutableListOf<DownloadTrack>()
                    searchResponse.tracks?.let { allTracks.addAll(it) }
                    searchResponse.spotifyTracks?.let { allTracks.addAll(it) }
                    searchResponse.youtubeTracks?.let { allTracks.addAll(it) }
                    searchResponse.soundcloudTracks?.let { allTracks.addAll(it) }
                    searchResponse.amazonTracks?.let { allTracks.addAll(it) }
                    searchResponse.itunesTracks?.let { allTracks.addAll(it) }
                    searchResponse.tidalTracks?.let { allTracks.addAll(it) }
                    searchResponse.deezerTracks?.let { allTracks.addAll(it) }

                    // Combine all albums
                    val allAlbums = mutableListOf<AlbumResult>()
                    searchResponse.albums?.let { allAlbums.addAll(it) }
                    searchResponse.itunesAlbums?.let { allAlbums.addAll(it) }
                    searchResponse.deezerAlbums?.let { allAlbums.addAll(it) }

                    // Get artists
                    val allArtists = searchResponse.artists ?: emptyList()

                    // Apply filters if any are selected
                    val selectedFilters = _uiState.value.selectedFilters
                    val filteredTracks = if (selectedFilters.isNotEmpty()) {
                        allTracks.filter { track ->
                            track.service?.let { service ->
                                selectedFilters.contains(service)
                            } ?: false
                        }
                    } else {
                        allTracks
                    }

                    val filteredAlbums = if (selectedFilters.isNotEmpty()) {
                        allAlbums.filter { album ->
                            album.service?.let { service ->
                                selectedFilters.contains(service)
                            } ?: false
                        }
                    } else {
                        allAlbums
                    }

                    val filteredArtists = if (selectedFilters.isNotEmpty()) {
                        allArtists.filter { artist ->
                            artist.service?.let { service ->
                                selectedFilters.contains(service)
                            } ?: false
                        }
                    } else {
                        allArtists
                    }

                    Timber.d("Total tracks found: ${filteredTracks.size}")
                    Timber.d("Total albums found: ${filteredAlbums.size}")
                    Timber.d("Total artists found: ${filteredArtists.size}")

                    if (filteredTracks.isNotEmpty() || filteredAlbums.isNotEmpty() || filteredArtists.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                tracks = filteredTracks,
                                albums = filteredAlbums,
                                artists = filteredArtists
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No results found for \"$query\""
                            )
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Timber.e("Search failed with code ${response.code()}: $errorBody")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to search: ${response.message()} - $errorBody"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Search failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Search failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun download(track: DownloadTrack) {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadStatus = "Downloading ${track.title}...") }

            val url = downloadSourceUrl.value
            if (url.isBlank()) {
                _uiState.update {
                    it.copy(downloadStatus = "Please select a profile or set download source URL")
                }
                return@launch
            }

            try {
                val api = downloadClient.createApi(url)
                val request = DownloadTrackRequest(
                    url = track.url,
                    isrc = track.isrc
                )
                val response = api.downloadTrack(request)

                Timber.d("Download response code: ${response.code()}")
                Timber.d("Download response successful: ${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val downloadResponse = response.body()!!
                    Timber.d("Download success: ${downloadResponse.success}")
                    Timber.d("Download taskId: ${downloadResponse.taskId}")
                    Timber.d("Download message: ${downloadResponse.message}")

                    if (downloadResponse.isSuccess) {
                        _uiState.update {
                            it.copy(downloadStatus = "Download started: ${track.title}")
                        }
                    } else {
                        _uiState.update {
                            it.copy(downloadStatus = "Download failed: ${downloadResponse.message}")
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Timber.e("Download failed with code ${response.code()}: $errorBody")
                    _uiState.update {
                        it.copy(downloadStatus = "Download failed: ${response.message()} - $errorBody")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Download failed")
                _uiState.update {
                    it.copy(downloadStatus = "Download failed: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearDownloadStatus() {
        _uiState.update { it.copy(downloadStatus = null) }
    }
}
