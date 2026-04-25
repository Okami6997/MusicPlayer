package com.musicplayer.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.DownloadRepository
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Track> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerHolder: PlayerHolder,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchResults = _query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) {
                _isLoading.value = false
                flowOf(emptyList<Track>())
            } else {
                repository.searchTracks(q)
                    .onStart { _isLoading.value = true }
                    .onEach { _isLoading.value = false }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _searchResults,
        _isLoading
    ) { query, results, loading ->
        SearchUiState(
            query = query,
            results = results,
            isLoading = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun playTrack(track: Track) {
        val results = _searchResults.value
        val index = results.indexOf(track).coerceAtLeast(0)
        playerHolder.playTracks(results, index)
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
        }
    }
}
