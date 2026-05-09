package com.musicplayer.data.repository

import com.musicplayer.data.local.ProfileDao
import com.musicplayer.data.local.ProfileEntity
import com.musicplayer.data.local.ProfileTrackEntity
import com.musicplayer.profile.MediaServiceType
import com.musicplayer.profile.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Profile and ProfileTrack operations.
 * Provides a clean API for accessing profile data with proper isolation from Old UI data.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) {

    // Profile operations

    fun getAllProfiles(): Flow<List<Profile>> {
        return profileDao.getAllProfiles().map { entities ->
            entities.map { it.toProfile() }
        }
    }

    fun getEnabledProfiles(): Flow<List<Profile>> {
        return profileDao.getEnabledProfiles().map { entities ->
            entities.map { it.toProfile() }
        }
    }

    suspend fun getProfileById(id: String): Profile? {
        return profileDao.getProfileById(id)?.toProfile()
    }

    suspend fun saveProfile(profile: Profile) {
        profileDao.insertProfile(profile.toEntity())
    }

    suspend fun updateProfile(profile: Profile) {
        profileDao.updateProfile(profile.toEntity())
    }

    suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfileAndTracks(profile.id)
    }

    suspend fun deleteProfileById(id: String) {
        profileDao.deleteProfileAndTracks(id)
    }

    suspend fun updateLastUsed(id: String) {
        profileDao.updateLastUsed(id, System.currentTimeMillis())
    }

    // ProfileTrack operations

    fun getTracksByProfileId(profileId: String): Flow<List<ProfileTrackEntity>> {
        return profileDao.getTracksByProfileId(profileId)
    }

    suspend fun getTrackById(id: String): ProfileTrackEntity? {
        return profileDao.getTrackById(id)
    }

    suspend fun getTrackByRemoteId(profileId: String, remoteId: String): ProfileTrackEntity? {
        return profileDao.getTrackByRemoteId(profileId, remoteId)
    }

    suspend fun saveTrack(track: ProfileTrackEntity) {
        profileDao.insertTrack(track)
    }

    suspend fun saveTracks(tracks: List<ProfileTrackEntity>) {
        profileDao.insertTracks(tracks)
    }

    suspend fun deleteTrack(track: ProfileTrackEntity) {
        profileDao.deleteTrack(track)
    }

    suspend fun deleteTrackById(id: String) {
        profileDao.deleteTrackById(id)
    }

    suspend fun deleteTracksByProfileId(profileId: String) {
        profileDao.deleteTracksByProfileId(profileId)
    }

    suspend fun getTrackCountForProfile(profileId: String): Int {
        return profileDao.getTrackCountForProfile(profileId)
    }

    // Extension functions for mapping between Entity and Domain models

    private fun ProfileEntity.toProfile(): Profile {
        return Profile(
            id = id,
            name = name,
            serviceType = MediaServiceType.fromString(serviceType) ?: MediaServiceType.Jellyfin,
            ipAddress = ipAddress,
            portOverride = portOverride,
            isEnabled = isEnabled,
            lastUsed = lastUsed,
            username = username,
            password = password,
            token = token,
            downloadPort = downloadPort
        )
    }

    private fun Profile.toEntity(): ProfileEntity {
        return ProfileEntity(
            id = id,
            name = name,
            serviceType = serviceType.serializedName,
            ipAddress = ipAddress,
            portOverride = portOverride,
            isEnabled = isEnabled,
            lastUsed = lastUsed,
            username = username,
            password = password,
            token = token,
            downloadPort = downloadPort
        )
    }
}
