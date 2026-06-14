package com.musicplayer.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Profile and ProfileTrack operations.
 * Provides isolated data access for New UI profiles and their associated tracks.
 */
@Dao
interface ProfileDao {

    // Profile CRUD operations

    @Query("SELECT * FROM profiles ORDER BY lastUsed DESC, name ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isEnabled = 1 ORDER BY lastUsed DESC, name ASC")
    fun getEnabledProfiles(): Flow<List<ProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)

    @Query("UPDATE profiles SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)

    // ProfileTrack CRUD operations

    @Query("SELECT * FROM profile_tracks WHERE profileId = :profileId ORDER BY album, discNumber, trackNumber")
    fun getTracksByProfileId(profileId: String): Flow<List<ProfileTrackEntity>>

    @Query("SELECT * FROM profile_tracks WHERE id = :id")
    suspend fun getTrackById(id: String): ProfileTrackEntity?

    @Query("SELECT * FROM profile_tracks WHERE profileId = :profileId AND remoteId = :remoteId")
    suspend fun getTrackByRemoteId(profileId: String, remoteId: String): ProfileTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: ProfileTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<ProfileTrackEntity>)

    @Update
    suspend fun updateTrack(track: ProfileTrackEntity)

    @Delete
    suspend fun deleteTrack(track: ProfileTrackEntity)

    @Query("DELETE FROM profile_tracks WHERE profileId = :profileId")
    suspend fun deleteTracksByProfileId(profileId: String)

    @Query("DELETE FROM profile_tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)

    // Batch operations

    @Transaction
    suspend fun deleteProfileAndTracks(profileId: String) {
        deleteTracksByProfileId(profileId)
        deleteProfileById(profileId)
    }

    @Query("SELECT COUNT(*) FROM profile_tracks WHERE profileId = :profileId")
    suspend fun getTrackCountForProfile(profileId: String): Int

    // ── Delta sync helpers ─────────────────────────────────────────────────────

    /** Returns all track remote IDs (without the profile prefix) for a profile. */
    @Query("SELECT remoteId FROM profile_tracks WHERE profileId = :profileId")
    suspend fun getRemoteIdsByProfile(profileId: String): List<String>

    /** Returns (remoteId, remoteUpdatedAt) for every track in a profile. */
    @Query("SELECT remoteId, remoteUpdatedAt FROM profile_tracks WHERE profileId = :profileId")
    suspend fun getTrackTimestampsByProfile(profileId: String): List<ProfileTrackTimestampRow>

    @Query("DELETE FROM profile_tracks WHERE profileId = :profileId AND remoteId IN (:remoteIds)")
    suspend fun deleteTracksByRemoteIds(profileId: String, remoteIds: List<String>)

    @Query("UPDATE profiles SET lastDeltaSyncAt = :timestamp WHERE id = :id")
    suspend fun updateLastDeltaSyncTime(id: String, timestamp: Long)

    @Query("UPDATE profiles SET lastFullSyncAt = :timestamp WHERE id = :id")
    suspend fun updateLastFullSyncTime(id: String, timestamp: Long)
}

/** Row used by ProfileDao.getTrackTimestampsByProfile. */
data class ProfileTrackTimestampRow(
    val remoteId: String,
    val remoteUpdatedAt: Long
)
