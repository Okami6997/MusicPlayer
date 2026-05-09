package com.musicplayer.ui.newui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.ProfileRepository
import com.musicplayer.profile.ProfileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewUiHomeUiState(
    val availableProfiles: List<com.musicplayer.profile.Profile> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class NewUiHomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewUiHomeUiState())
    val uiState: StateFlow<NewUiHomeUiState> = _uiState.asStateFlow()

    val selectedProfile: StateFlow<com.musicplayer.profile.Profile?> = profileManager.selectedProfile

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            profileRepository.getEnabledProfiles().collect { profiles ->
                _uiState.update { it.copy(availableProfiles = profiles, isLoading = false) }
            }
        }
    }

    fun selectProfile(profileId: String) {
        viewModelScope.launch {
            profileManager.selectProfile(profileId)
        }
    }
}
