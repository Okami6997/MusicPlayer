package com.musicplayer.data.repository

import com.musicplayer.data.local.LocalMediaScanner
import com.musicplayer.data.local.MediaSourceDao
import com.musicplayer.data.local.PlaylistDao
import com.musicplayer.data.local.TrackDao
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.MediaSourceType
import com.musicplayer.domain.model.Playlist
import com.musicplayer.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository for all music data.
 * Aggregates local and remote sources, persists everything in Room.
 */
@Singleton
class MusicRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val mediaSourceDao: MediaSourceDao,
    private val playlistDao: PlaylistDao,
    private val localMediaScanner: LocalMediaScanner
) {
    // ── Tracks ────────────────────────────────────────────────────────────────

    fun getAllTracks(): Flow<List<Track>> =
        trackDao.getAllTracks().map { list -> list.map { it.toDomain() } }

    fun getTracksBySource(sourceId: String): Flow<List<Track>> =
        trackDao.getTracksBySource(sourceId).map { list -> list.map { it.toDomain() } }

    fun searchTracks(query: String): Flow<List<Track>> =
        trackDao.searchTracks(query).map { list -> list.map { it.toDomain() } }

    fun getDownloadedTracks(): Flow<List<Track>> =
        trackDao.getDownloadedTracks().map { list -> list.map { it.toDomain() } }

    suspend fun getTrackById(id: String): Track? =
        trackDao.getTrackById(id)?.toDomain()

    suspend fun saveTracks(tracks: List<Track>) {
        trackDao.upsertTracks(tracks.map { it.toEntity() })
    }

    suspend fun markTrackDownloaded(trackId: String, localUri: String) {
        trackDao.getTrackById(trackId)?.let { entity ->
            trackDao.upsertTrack(entity.copy(isDownloaded = true, downloadedUri = localUri))
        }
    }

    // ── Local scanning ────────────────────────────────────────────────────────

    suspend fun scanLocalLibrary(): List<Track> {
        val tracks = localMediaScanner.scan()
        if (tracks.isNotEmpty()) {
            trackDao.deleteTracksBySource("local")
            trackDao.upsertTracks(tracks.map { it.toEntity() })
        }
        Timber.d("scanLocalLibrary: persisted ${tracks.size} tracks")
        return tracks
    }

    // ── Media sources ─────────────────────────────────────────────────────────

    fun getAllSources(): Flow<List<MediaSource>> =
        mediaSourceDao.getAllSources().map { list -> list.map { it.toDomain() } }

    suspend fun getSourceById(id: String): MediaSource? =
        mediaSourceDao.getSourceById(id)?.toDomain()

    suspend fun saveSource(source: MediaSource) {
        mediaSourceDao.upsertSource(source.toEntity())
    }

    suspend fun deleteSource(id: String) {
        trackDao.deleteTracksBySource(id)
        mediaSourceDao.deleteSource(id)
    }

    // ── Playlists ─────────────────────────────────────────────────────────────

    fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { list -> list.map { it.toDomain() } }

    suspend fun getPlaylistWithTracks(playlistId: String): Playlist? {
        val entity = playlistDao.getPlaylistById(playlistId) ?: return null
        // We'll collect once from the flow to get the current state
        return entity.toDomain()
    }

    suspend fun savePlaylist(playlist: Playlist) {
        playlistDao.upsertPlaylist(playlist.toEntity())
    }

    suspend fun deletePlaylist(id: String) {
        playlistDao.deletePlaylistTracks(id)
        playlistDao.deletePlaylist(id)
    }
}
