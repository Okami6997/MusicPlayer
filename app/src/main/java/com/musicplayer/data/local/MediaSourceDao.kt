package com.musicplayer.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaSourceDao {
    @Query("SELECT * FROM media_sources ORDER BY name")
    fun getAllSources(): Flow<List<MediaSourceEntity>>

    @Query("SELECT * FROM media_sources WHERE id = :id")
    suspend fun getSourceById(id: String): MediaSourceEntity?

    @Upsert
    suspend fun upsertSource(source: MediaSourceEntity)

    @Query("DELETE FROM media_sources WHERE id = :id")
    suspend fun deleteSource(id: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Upsert
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Upsert
    suspend fun upsertPlaylistTrack(playlistTrack: PlaylistTrackEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: String)

    @Query("""
        SELECT t.* FROM tracks t 
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId 
        WHERE pt.playlistId = :playlistId 
        ORDER BY pt.position
    """)
    fun getPlaylistTracks(playlistId: String): Flow<List<TrackEntity>>
}
