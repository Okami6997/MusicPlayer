package com.musicplayer.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE sourceId = :sourceId ORDER BY artist, album, discNumber, trackNumber")
    fun getTracksBySource(sourceId: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY artist, album, discNumber, trackNumber")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("""
        SELECT * FROM tracks 
        WHERE lower(title) LIKE '%' || lower(:query) || '%'
        OR lower(artist) LIKE '%' || lower(:query) || '%'
        OR lower(album) LIKE '%' || lower(:query) || '%'
        ORDER BY title
    """)
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isDownloaded = 1")
    fun getDownloadedTracks(): Flow<List<TrackEntity>>

    @Upsert
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Upsert
    suspend fun upsertTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE sourceId = :sourceId")
    suspend fun deleteTracksBySource(sourceId: String)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)

    @Query("SELECT DISTINCT album FROM tracks WHERE lower(album) LIKE '%' || lower(:query) || '%' ORDER BY album")
    fun searchAlbums(query: String): Flow<List<String>>

    // ── Delta sync helpers ─────────────────────────────────────────────────────

    /** Returns all track IDs belonging to a source. Used for diffing during delta sync. */
    @Query("SELECT id FROM tracks WHERE sourceId = :sourceId")
    suspend fun getTrackIdsBySource(sourceId: String): List<String>

    /** Returns all track IDs together with their remoteUpdatedAt timestamp. */
    @Query("SELECT id, remoteUpdatedAt FROM tracks WHERE sourceId = :sourceId")
    suspend fun getTrackTimestampsBySource(sourceId: String): List<TrackTimestampRow>

    /** Upserts a single track (used by delta sync to insert new / update modified tracks). */
    @Upsert
    suspend fun upsertTrackEntity(track: TrackEntity)

    /** Deletes a batch of track IDs that no longer exist on the remote server. */
    @Query("DELETE FROM tracks WHERE sourceId = :sourceId AND id IN (:ids)")
    suspend fun deleteTracksByIds(sourceId: String, ids: List<String>)
}

/** Simple row used by TrackDao.getTrackTimestampsBySource. */
data class TrackTimestampRow(
    val id: String,
    val remoteUpdatedAt: Long
)
