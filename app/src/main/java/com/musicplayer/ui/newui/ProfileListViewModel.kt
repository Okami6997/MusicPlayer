package com.musicplayer.ui.newui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.ProfileMusicRepository
import com.musicplayer.data.repository.ProfileRepository
import com.musicplayer.profile.MediaServiceType
import com.musicplayer.profile.Profile
import com.musicplayer.profile.toMediaSource
import com.musicplayer.worker.DeltaProfileSyncWorker
import com.musicplayer.worker.ProfileSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProfileListUiState(
    val profiles: List<Profile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val syncingProfileId: String? = null,
    val syncMessage: String? = null
)

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val musicRepository: MusicRepository,
    private val profileMusicRepository: ProfileMusicRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileListUiState())
    val uiState: StateFlow<ProfileListUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            profileRepository.getAllProfiles().collect { profiles ->
                _uiState.update { it.copy(profiles = profiles, isLoading = false) }
            }
        }
    }

    fun createProfile(
        name: String,
        serviceType: MediaServiceType,
        ipAddress: String,
        portOverride: Int?
    ) {
        viewModelScope.launch {
            try {
                val profile = Profile(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    serviceType = serviceType,
                    ipAddress = ipAddress,
                    portOverride = portOverride,
                    isEnabled = true,
                    lastUsed = System.currentTimeMillis()
                )
                profileRepository.saveProfile(profile)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create profile: ${e.message}") }
            }
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            try {
                profileRepository.updateProfile(profile)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update profile: ${e.message}") }
            }
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            try {
                profileRepository.deleteProfile(profile)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete profile: ${e.message}") }
            }
        }
    }

    fun toggleProfileEnabled(profile: Profile) {
        viewModelScope.launch {
            try {
                profileRepository.updateProfile(profile.copy(isEnabled = !profile.isEnabled))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to toggle profile: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun syncProfile(profile: Profile) {
        // Default behaviour is now a delta sync (fast, fetches only changes).
        deltaSyncProfile(profile)
    }

    /**
     * Enqueues a [DeltaProfileSyncWorker] for the given [profile] so only the
     * changes since the last sync are downloaded. Falls back to a full sync
     * internally if the profile has never been fully synced.
     */
    fun deltaSyncProfile(profile: Profile) {
        if (_uiState.value.syncingProfileId != null) return

        val inputData = workDataOf(
            DeltaProfileSyncWorker.KEY_PROFILE_ID to profile.id,
            DeltaProfileSyncWorker.KEY_PROFILE_NAME to profile.name
        )

        val syncWork = OneTimeWorkRequestBuilder<DeltaProfileSyncWorker>()
            .setInputData(inputData)
            .addTag("delta_sync_profile_${profile.id}")
            .build()

        workManager.enqueueUniqueWork(
            "delta_sync_profile_${profile.id}",
            ExistingWorkPolicy.REPLACE,
            syncWork
        )

        _uiState.update { it.copy(syncingProfileId = profile.id, syncMessage = null) }

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(syncWork.id).collect { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val added = workInfo.outputData.getInt(DeltaProfileSyncWorker.KEY_ADDED, 0)
                        val updated = workInfo.outputData.getInt(DeltaProfileSyncWorker.KEY_UPDATED, 0)
                        val removed = workInfo.outputData.getInt(DeltaProfileSyncWorker.KEY_REMOVED, 0)
                        val total = workInfo.outputData.getInt(DeltaProfileSyncWorker.KEY_TOTAL_AFTER, 0)
                        val msg = if (added == 0 && updated == 0 && removed == 0) {
                            "Up to date ($total tracks) for \"${profile.name}\""
                        } else {
                            "Synced \"${profile.name}\" +$added ~$updated -$removed ($total total)"
                        }
                        _uiState.update { it.copy(syncingProfileId = null, syncMessage = msg) }
                    }
                    WorkInfo.State.FAILED -> {
                        val errorMessage = workInfo.outputData.getString(DeltaProfileSyncWorker.KEY_ERROR_MESSAGE) ?: "Unknown error"
                        _uiState.update { it.copy(syncingProfileId = null, syncMessage = "Sync failed: $errorMessage") }
                    }
                    WorkInfo.State.RUNNING -> {
                        _uiState.update { it.copy(syncingProfileId = profile.id, syncMessage = "Syncing ${profile.name}...") }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Enqueues a [ProfileSyncWorker] for the given [profile], performing a
     * full re-sync that rebuilds the local cache from scratch.
     */
    fun fullSyncProfile(profile: Profile) {
        if (_uiState.value.syncingProfileId != null) return

        val inputData = workDataOf(
            ProfileSyncWorker.KEY_PROFILE_ID to profile.id,
            ProfileSyncWorker.KEY_PROFILE_NAME to profile.name
        )

        val syncWork = OneTimeWorkRequestBuilder<ProfileSyncWorker>()
            .setInputData(inputData)
            .addTag("full_sync_profile_${profile.id}")
            .build()

        workManager.enqueueUniqueWork(
            "full_sync_profile_${profile.id}",
            ExistingWorkPolicy.REPLACE,
            syncWork
        )

        _uiState.update { it.copy(syncingProfileId = profile.id, syncMessage = null) }

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(syncWork.id).collect { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val trackCount = workInfo.outputData.getInt(ProfileSyncWorker.KEY_TRACK_COUNT, 0)
                        _uiState.update { it.copy(syncingProfileId = null, syncMessage = "Full sync done: $trackCount tracks from \"${profile.name}\"") }
                    }
                    WorkInfo.State.FAILED -> {
                        val errorMessage = workInfo.outputData.getString(ProfileSyncWorker.KEY_ERROR_MESSAGE) ?: "Unknown error"
                        _uiState.update { it.copy(syncingProfileId = null, syncMessage = "Sync failed: $errorMessage") }
                    }
                    WorkInfo.State.RUNNING -> {
                        _uiState.update { it.copy(syncingProfileId = profile.id, syncMessage = "Full sync of ${profile.name}...") }
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }
}
