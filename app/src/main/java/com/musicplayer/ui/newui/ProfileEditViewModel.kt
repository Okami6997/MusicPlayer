package com.musicplayer.ui.newui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.ConnectionTestResult
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.ProfileRepository
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.profile.MediaServiceType
import com.musicplayer.profile.Profile
import com.musicplayer.profile.toMediaSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProfileEditUiState(
    val profile: Profile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val isTestingConnection: Boolean = false,
    val testConnectionResult: ConnectionTestResult? = null
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    fun loadProfile(profileId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, saved = false) }
            if (profileId != null) {
                try {
                    val profile = profileRepository.getProfileById(profileId)
                    _uiState.update { it.copy(profile = profile, isLoading = false) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            error = "Failed to load profile: ${e.message}",
                            isLoading = false
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun saveProfile(
        name: String,
        serviceType: MediaServiceType,
        ipAddress: String,
        portOverride: Int?,
        username: String,
        password: String,
        token: String,
        downloadPort: Int
    ) {
        viewModelScope.launch {
            try {
                val existingProfile = _uiState.value.profile
                val profile = if (existingProfile != null) {
                    existingProfile.copy(
                        name = name,
                        serviceType = serviceType,
                        ipAddress = ipAddress,
                        portOverride = portOverride,
                        username = username,
                        password = password,
                        token = token,
                        downloadPort = downloadPort
                    )
                } else {
                    Profile(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        serviceType = serviceType,
                        ipAddress = ipAddress,
                        portOverride = portOverride,
                        isEnabled = true,
                        lastUsed = System.currentTimeMillis(),
                        username = username,
                        password = password,
                        token = token,
                        downloadPort = downloadPort
                    )
                }

                if (existingProfile != null) {
                    profileRepository.updateProfile(profile)
                } else {
                    profileRepository.saveProfile(profile)
                }
                _uiState.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save profile: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun testConnection(
        name: String,
        serviceType: MediaServiceType,
        ipAddress: String,
        portOverride: Int?,
        username: String,
        password: String,
        token: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testConnectionResult = null) }
            val tempProfile = Profile(
                id = "_test_",
                name = name.ifBlank { "Test" },
                serviceType = serviceType,
                ipAddress = ipAddress,
                portOverride = portOverride,
                username = username,
                password = password,
                token = token
            )
            val result = musicRepository.testConnection(tempProfile.toMediaSource())
            _uiState.update { it.copy(isTestingConnection = false, testConnectionResult = result) }
        }
    }

    fun clearTestConnectionResult() {
        _uiState.update { it.copy(testConnectionResult = null) }
    }
}
