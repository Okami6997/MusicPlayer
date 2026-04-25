package com.musicplayer.data.repository

import com.musicplayer.data.local.LocalMediaScanner
import com.musicplayer.data.local.MediaSourceDao
import com.musicplayer.data.local.PlaylistDao
import com.musicplayer.data.local.TrackDao
import com.musicplayer.data.remote.jellyfin.JellyfinApi
import com.musicplayer.data.remote.jellyfin.JellyfinClient
import com.musicplayer.data.remote.navidrome.NavidromeApi
import com.musicplayer.data.remote.navidrome.NavidromeClient
import com.musicplayer.data.remote.plex.PlexApi
import com.musicplayer.data.remote.plex.PlexClient
import com.musicplayer.data.remote.subsonic.SubsonicApi
import com.musicplayer.data.remote.subsonic.SubsonicClient
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.MediaSourceType
import com.musicplayer.domain.model.Playlist
import com.musicplayer.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
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
    private val localMediaScanner: LocalMediaScanner,
    private val subsonicClient: SubsonicClient,
    private val navidromeClient: NavidromeClient,
    private val jellyfinClient: JellyfinClient,
    private val plexClient: PlexClient
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

    fun getPlaylistTracks(playlistId: String): Flow<List<Track>> =
        playlistDao.getPlaylistTracks(playlistId).map { list -> list.map { it.toDomain() } }

    suspend fun getPlaylistWithTracks(playlistId: String): Playlist? {
        val entity = playlistDao.getPlaylistById(playlistId) ?: return null
        // We'll collect once from the flow to get the current state
        return entity.toDomain()
    }

    suspend fun savePlaylist(playlist: Playlist) {
        playlistDao.upsertPlaylist(playlist.toEntity())
    }

    suspend fun createPlaylist(name: String): String {
        val id = java.util.UUID.randomUUID().toString()
        val playlist = Playlist(
            id = id,
            name = name,
            artworkUri = null,
            sourceId = "local_user",
            sourceType = MediaSourceType.USER,
            isLocal = true,
            description = "",
            trackCount = 0,
            duration = 0
        )
        playlistDao.upsertPlaylist(playlist.toEntity())
        return id
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) {
        val track = trackDao.getTrackById(trackId) ?: return
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return

        val playlistTrack = com.musicplayer.data.local.PlaylistTrackEntity(
            playlistId = playlistId,
            trackId = trackId,
            position = playlist.trackCount
        )
        playlistDao.upsertPlaylistTrack(playlistTrack)

        // Update playlist metadata (increment count and total duration)
        playlistDao.upsertPlaylist(
            playlist.copy(
                trackCount = playlist.trackCount + 1,
                duration = playlist.duration + track.duration
            )
        )
    }

    suspend fun deletePlaylist(id: String) {
        playlistDao.deletePlaylistTracks(id)
        playlistDao.deletePlaylist(id)
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.deleteTrackFromPlaylist(playlistId, trackId)
    }

    // ── Source synchronization ─────────────────────────────────────────────

    private fun createHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private inline fun <reified T> createApi(source: MediaSource): T {
        return Retrofit.Builder()
            .baseUrl(source.baseUrl.trimEnd('/') + "/")
            .client(createHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(T::class.java)
    }

    /**
     * Syncs/fetches all tracks from a media source.
     */
    suspend fun syncSource(source: MediaSource): List<Track> {
        return when (source.type) {
            MediaSourceType.LOCAL -> {
                scanLocalLibrary()
            }
            MediaSourceType.SUBSONIC,
            MediaSourceType.OPEN_SUBSONIC -> {
                syncSubsonicSource(source)
            }
            MediaSourceType.NAVIDROME -> {
                syncNavidromeSource(source)
            }
            MediaSourceType.JELLYFIN,
            MediaSourceType.EMBY -> {
                syncJellyfinSource(source)
            }
            MediaSourceType.PLEX -> {
                syncPlexSource(source)
            }
            MediaSourceType.AUDIOBOOKSHELF,
            MediaSourceType.CLOUD_DRIVE,
            MediaSourceType.USER -> {
                Timber.w("Sync not supported for source type: ${source.type}")
                emptyList()
            }
        }
    }

    /**
     * Fetches all tracks from a Subsonic-compatible server.
     */
    private suspend fun syncSubsonicSource(source: MediaSource): List<Track> {
        return try {
            Timber.d("Starting sync for Subsonic source: ${source.name}")
            val api = createApi<SubsonicApi>(source)
            val tracks = subsonicClient.fetchAllTracks(api, source)
            persistSyncedTracks(source.id, tracks, source.name)
            tracks
        } catch (e: Exception) {
            Timber.e(e, "Error syncing Subsonic source: ${source.name}")
            throw e
        }
    }

    /**
     * Fetches all tracks from a Navidrome server using the native API.
     */
    private suspend fun syncNavidromeSource(source: MediaSource): List<Track> {
        return try {
            Timber.d("Starting sync for Navidrome source: ${source.name}")
            val api = createApi<NavidromeApi>(source)
            val tracks = navidromeClient.fetchAllTracks(api, source)
            persistSyncedTracks(source.id, tracks, source.name)
            tracks
        } catch (e: Exception) {
            Timber.e(e, "Error syncing Navidrome source: ${source.name}")
            throw e
        }
    }

    /**
     * Fetches all tracks from a Jellyfin/Emby server.
     */
    private suspend fun syncJellyfinSource(source: MediaSource): List<Track> {
        return try {
            Timber.d("Starting sync for Jellyfin/Emby source: ${source.name}")
            val api = createApi<JellyfinApi>(source)
            val tracks = jellyfinClient.fetchAllTracks(api, source)
            persistSyncedTracks(source.id, tracks, source.name)
            tracks
        } catch (e: Exception) {
            Timber.e(e, "Error syncing Jellyfin/Emby source: ${source.name}")
            throw e
        }
    }

    /**
     * Fetches all tracks from a Plex Media Server.
     */
    private suspend fun syncPlexSource(source: MediaSource): List<Track> {
        return try {
            Timber.d("Starting sync for Plex source: ${source.name}")
            val api = createApi<PlexApi>(source)
            val tracks = plexClient.fetchAllTracks(api, source)
            persistSyncedTracks(source.id, tracks, source.name)
            tracks
        } catch (e: Exception) {
            Timber.e(e, "Error syncing Plex source: ${source.name}")
            throw e
        }
    }

    private suspend fun persistSyncedTracks(sourceId: String, tracks: List<Track>, sourceName: String) {
        if (tracks.isNotEmpty()) {
            trackDao.deleteTracksBySource(sourceId)
            trackDao.upsertTracks(tracks.map { it.toEntity() })
            Timber.d("Synced ${tracks.size} tracks from $sourceName")
        }
    }
}
