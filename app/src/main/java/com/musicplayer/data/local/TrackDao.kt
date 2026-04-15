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
}
