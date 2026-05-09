package com.musicplayer.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.musicplayer.data.repository.ProfileRepository
import com.musicplayer.di.ProfileDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for global profile state.
 * Manages the currently selected profile and provides reactive access across the app.
 */
@Singleton
class ProfileManager @Inject constructor(
    private val profileRepository: ProfileRepository,
    @ProfileDataStore private val profileDataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_SELECTED_PROFILE_ID = stringPreferencesKey("selected_profile_id")
    }

    private val _selectedProfileId = MutableStateFlow<String?>(null)

    /**
     * Flow of the currently selected profile ID.
     */
    val selectedProfileId: StateFlow<String?> = _selectedProfileId.asStateFlow()

    /**
     * Flow of the currently selected Profile.
     * Emits null if no profile is selected.
     */
    val selectedProfile: StateFlow<Profile?> = combine(
        _selectedProfileId,
        profileRepository.getAllProfiles()
    ) { profileId, profiles ->
        if (profileId == null) {
            null
        } else {
            profiles.find { it.id == profileId && it.isEnabled }
        }
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    /**
     * All available profiles.
     */
    val allProfiles: StateFlow<List<Profile>> = profileRepository.getAllProfiles()
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        // Load the selected profile ID from DataStore on initialization
        loadSelectedProfileId()
    }

    private fun loadSelectedProfileId() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            profileDataStore.data.collect { prefs ->
                val savedId = prefs[KEY_SELECTED_PROFILE_ID]
                _selectedProfileId.value = savedId
                Timber.d("ProfileManager: Loaded selected profile ID: $savedId")
            }
        }
    }

    /**
     * Selects a profile as the current active profile.
     * Updates the last used timestamp and persists the selection.
     */
    suspend fun selectProfile(profileId: String) {
        Timber.d("ProfileManager: Selecting profile: $profileId")
        profileRepository.updateLastUsed(profileId)
        profileDataStore.edit { it[KEY_SELECTED_PROFILE_ID] = profileId }
        _selectedProfileId.value = profileId
    }

    /**
     * Clears the selected profile.
     */
    suspend fun clearSelectedProfile() {
        Timber.d("ProfileManager: Clearing selected profile")
        profileDataStore.edit { it.remove(KEY_SELECTED_PROFILE_ID) }
        _selectedProfileId.value = null
    }

    /**
     * Returns the base URL for the currently selected profile.
     * Returns null if no profile is selected.
     */
    fun getBaseUrl(): String? {
        return selectedProfile.value?.baseUrl
    }

    /**
     * Returns the IP address of the currently selected profile.
     * Returns null if no profile is selected.
     */
    fun getIpAddress(): String? {
        return selectedProfile.value?.ipAddress
    }

    /**
     * Returns the effective port of the currently selected profile.
     * Returns null if no profile is selected.
     */
    fun getPort(): Int? {
        return selectedProfile.value?.effectivePort
    }

    /**
     * Returns the service type of the currently selected profile.
     * Returns null if no profile is selected.
     */
    fun getServiceType(): MediaServiceType? {
        return selectedProfile.value?.serviceType
    }
}
