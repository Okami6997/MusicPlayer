package com.musicplayer.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.domain.model.MediaSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourcesUiState(
    val sources: List<MediaSource> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    val uiState: StateFlow<SourcesUiState> = repository.getAllSources()
        .map { SourcesUiState(sources = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SourcesUiState())

    fun addSource(source: MediaSource) {
        viewModelScope.launch {
            repository.saveSource(source)
        }
    }

    fun deleteSource(id: String) {
        viewModelScope.launch {
            repository.deleteSource(id)
        }
    }
}
