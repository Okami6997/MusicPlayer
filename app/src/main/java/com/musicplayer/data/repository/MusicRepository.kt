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

sealed class ConnectionTestResult {
    data object Success : ConnectionTestResult()
    data class Error(val message: String) : ConnectionTestResult()
}

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
     * Also persists them to the old-UI tracks table (for old UI sources).
     */
    private suspend fun syncSubsonicSource(source: MediaSource): List<Track> {
        return try {
            val tracks = fetchSubsonicTracks(source)
            persistSyncedTracks(source.id, tracks, source.name)
            tracks
        } catch (e: Exception) {
            Timber.e(e, "Error syncing Subsonic source: ${source.name}")
            throw e
        }
    }

    private suspend fun fetchSubsonicTracks(source: MediaSource): List<Track> {
        Timber.d("Fetching tracks from Subsonic source: ${source.name}")
        val api = createApi<SubsonicApi>(source)
        return subsonicClient.fetchAllTracks(api, source)
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
     * Also persists them to the old-UI tracks table (for old UI sources).
     */
    private suspend fun syncJellyfinSource(source: MediaSource): List<Track> {
        return try {
            val tracks = fetchJellyfinTracks(source)
            persistSyncedTracks(source.id, tracks, source.name)
            tracks
        } catch (e: Exception) {
            Timber.e(e, "Error syncing Jellyfin/Emby source: ${source.name}")
            throw e
        }
    }

    private suspend fun fetchJellyfinTracks(source: MediaSource): List<Track> {
        Timber.d("Fetching tracks from Jellyfin/Emby source: ${source.name}")
        val api = createApi<JellyfinApi>(source)
        return jellyfinClient.fetchAllTracks(api, source)
    }

    /**
     * Fetches all tracks from a Plex Media Server.
     * Also persists them to the old-UI tracks table (for old UI sources).
     */
    private suspend fun syncPlexSource(source: MediaSource): List<Track> {
        return try {
            val tracks = fetchPlexTracks(source)
            persistSyncedTracks(source.id, tracks, source.name)
            tracks
        } catch (e: Exception) {
            Timber.e(e, "Error syncing Plex source: ${source.name}")
            throw e
        }
    }

    private suspend fun fetchPlexTracks(source: MediaSource): List<Track> {
        Timber.d("Fetching tracks from Plex source: ${source.name}")
        val api = createApi<PlexApi>(source)
        return plexClient.fetchAllTracks(api, source)
    }

    private suspend fun persistSyncedTracks(sourceId: String, tracks: List<Track>, sourceName: String) {
        if (tracks.isNotEmpty()) {
            trackDao.deleteTracksBySource(sourceId)
            trackDao.upsertTracks(tracks.map { it.toEntity() })
            Timber.d("Synced ${tracks.size} tracks from $sourceName")
        }
    }

    /**
     * Fetches tracks from a remote source WITHOUT writing to the old-UI tracks table.
     * Use this from the new UI (profile-based) so that data stays mutually exclusive.
     */
    suspend fun fetchTracksFromSource(source: MediaSource): List<Track> {
        return when (source.type) {
            MediaSourceType.SUBSONIC,
            MediaSourceType.OPEN_SUBSONIC -> fetchSubsonicTracks(source)
            MediaSourceType.NAVIDROME -> {
                val api = createApi<NavidromeApi>(source)
                navidromeClient.fetchAllTracks(api, source)
            }
            MediaSourceType.JELLYFIN,
            MediaSourceType.EMBY -> fetchJellyfinTracks(source)
            MediaSourceType.PLEX -> fetchPlexTracks(source)
            else -> {
                Timber.w("fetchTracksFromSource: unsupported type ${source.type}")
                emptyList()
            }
        }
    }

    /**
     * Fetches only the tracks that have changed since [sinceEpochMillis] for
     * the given [source]. Mirrors [fetchTracksFromSource] but uses each
     * client's delta-sync endpoint. Does NOT write to the DB — the caller
     * (typically [ProfileMusicRepository.deltaSyncProfile]) is responsible
     * for diffing and persisting.
     */
    suspend fun fetchChangedTracksFromSource(source: MediaSource, sinceEpochMillis: Long): List<Track> {
        return try {
            fetchChangedTracksForSource(source, sinceEpochMillis)
        } catch (e: Exception) {
            Timber.e(e, "fetchChangedTracksFromSource failed for ${source.name}")
            emptyList()
        }
    }

    /**
     * Fetches the full remote track ID set for a source. Used by profile delta
     * sync to detect deletions when the backend returns only changed items.
     */
    suspend fun fetchAllTrackIdsFromSource(source: MediaSource): Set<String> {
        return try {
            when (source.type) {
                MediaSourceType.JELLYFIN,
                MediaSourceType.EMBY -> {
                    val api = createApi<JellyfinApi>(source)
                    jellyfinClient.fetchAllTrackIds(api, source)
                }
                MediaSourceType.NAVIDROME -> {
                    val api = createApi<NavidromeApi>(source)
                    navidromeClient.fetchAllTrackIds(api, source)
                }
                else -> emptySet()
            }
        } catch (e: Exception) {
            Timber.e(e, "fetchAllTrackIdsFromSource failed for ${source.name}")
            emptySet()
        }
    }

    // ── Delta sync ─────────────────────────────────────────────────────────────

    /**
     * Summary of a delta sync run — what was added, updated, and removed.
     */
    data class DeltaSyncResult(
        val added: Int,
        val updated: Int,
        val removed: Int,
        val totalAfter: Int
    )

    /**
     * Performs a delta sync for the given [source]: fetches only the tracks
     * that have changed since [MediaSource.lastDeltaSyncAt], diffs them against
     * the local cache, and applies the changes (insert / update / delete).
     *
     * Falls back to a full sync if:
     * - A full sync has never been completed for this source.
     * - The source type doesn't support incremental fetching.
     * - The remote client can't return changed items.
     *
     * Updates [MediaSource.lastDeltaSyncAt] on success.
     */
    suspend fun deltaSyncSource(source: MediaSource): DeltaSyncResult {
        // If we have never done a full sync, we have nothing to diff against — fall back.
        if (source.lastFullSyncAt <= 0L) {
            Timber.d("Delta sync: no full sync on record for ${source.name}, falling back to full sync")
            val tracks = syncSource(source)
            mediaSourceDao.updateLastDeltaSyncTime(source.id, System.currentTimeMillis())
            mediaSourceDao.updateLastFullSyncTime(source.id, System.currentTimeMillis())
            return DeltaSyncResult(added = tracks.size, updated = 0, removed = 0, totalAfter = tracks.size)
        }

        val sinceEpochMillis = source.lastDeltaSyncAt
        val changed = try {
            fetchChangedTracksForSource(source, sinceEpochMillis)
        } catch (e: Exception) {
            Timber.e(e, "Delta sync failed for ${source.name}, falling back to full sync")
            val tracks = syncSource(source)
            mediaSourceDao.updateLastDeltaSyncTime(source.id, System.currentTimeMillis())
            mediaSourceDao.updateLastFullSyncTime(source.id, System.currentTimeMillis())
            return DeltaSyncResult(added = tracks.size, updated = 0, removed = 0, totalAfter = tracks.size)
        }

        // Subsonic clients may return an empty list when the cheap lastModified
        // check reports no changes. Don't touch the DB in that case.
        if (changed.isEmpty() && source.type in listOf(
                MediaSourceType.SUBSONIC,
                MediaSourceType.OPEN_SUBSONIC
            )
        ) {
            Timber.d("Delta sync (Subsonic/Navidrome): no changes reported for ${source.name}")
            mediaSourceDao.updateLastDeltaSyncTime(source.id, System.currentTimeMillis())
            return DeltaSyncResult(added = 0, updated = 0, removed = 0, totalAfter = trackDao.getTrackIdsBySource(source.id).size)
        }

        val result = applyDeltaChanges(source, changed)
        mediaSourceDao.updateLastDeltaSyncTime(source.id, System.currentTimeMillis())
        Timber.d("Delta sync for ${source.name}: +${result.added} ~${result.updated} -${result.removed}")
        return result
    }

    /**
     * Fetches the changed tracks (added or modified since the cutoff) from the
     * appropriate remote client for the given source.
     */
    private suspend fun fetchChangedTracksForSource(source: MediaSource, sinceEpochMillis: Long): List<Track> {
        return when (source.type) {
            MediaSourceType.SUBSONIC,
            MediaSourceType.OPEN_SUBSONIC -> {
                val api = createApi<SubsonicApi>(source)
                // Subsonic: use the cheap getIndexes.lastModified check. If unchanged,
                // return an empty list so the caller can short-circuit. If the
                // server doesn't support it, fall back to a full fetch.
                val lastModified = subsonicClient.fetchLibraryLastModified(api, source)
                if (lastModified != null && sinceEpochMillis > 0L && lastModified <= sinceEpochMillis) {
                    emptyList()
                } else {
                    // Library changed (or we don't have a baseline) — fetch everything.
                    subsonicClient.fetchAllTracks(api, source)
                }
            }
            MediaSourceType.NAVIDROME -> {
                val navidromeApi = createApi<NavidromeApi>(source)
                navidromeClient.fetchChangedTracks(navidromeApi, source, sinceEpochMillis)
            }
            MediaSourceType.JELLYFIN,
            MediaSourceType.EMBY -> {
                val api = createApi<JellyfinApi>(source)
                jellyfinClient.fetchChangedTracks(api, source, sinceEpochMillis)
            }
            MediaSourceType.PLEX -> {
                val api = createApi<PlexApi>(source)
                plexClient.fetchChangedTracks(api, source, sinceEpochMillis)
            }
            else -> {
                Timber.w("Delta sync not supported for source type: ${source.type}")
                emptyList()
            }
        }
    }

    /**
     * Applies a set of [changed] tracks to the local cache: upserts each one
     * and removes any local tracks belonging to [source] whose IDs are not in
     * the changed set and that are older than the cutoff.
     *
     * For sources that don't return a complete list of every server track
     * (e.g. Subsonic's "full fetch" path), we do a simpler strategy: upsert
     * the changed rows and leave the existing local rows alone for removal
     * detection. Removal detection is only performed when the server returns
     * a complete inventory (the Subsonic "library changed" path).
     */
    private suspend fun applyDeltaChanges(source: MediaSource, changed: List<Track>): DeltaSyncResult {
        val changedIds = changed.map { it.id }.toSet()
        var added = 0
        var updated = 0

        if (changed.isNotEmpty()) {
            val existing = trackDao.getTrackTimestampsBySource(source.id)
                .associate { it.id to it.remoteUpdatedAt }
            val toUpsert = changed.map { track ->
                val existingEntity = trackDao.getTrackById(track.id)
                val mergedTrack = mergeTrackMetadata(track, existingEntity)
                val entity = mergedTrack.toEntity()
                if (existing.containsKey(track.id)) updated++ else added++
                entity
            }
            trackDao.upsertTracks(toUpsert)
        }

        // Removal detection:
        // - Subsonic/OpenSubsonic: changed list is a full inventory when library changed.
        // - Jellyfin/Emby: run an ID reconciliation pass to detect deletions.
        val removed = when (source.type) {
            MediaSourceType.SUBSONIC,
            MediaSourceType.OPEN_SUBSONIC -> {
                val localIds = trackDao.getTrackIdsBySource(source.id).toSet()
                val toRemove = localIds - changedIds
                if (toRemove.isNotEmpty()) {
                    trackDao.deleteTracksByIds(source.id, toRemove.toList())
                }
                toRemove.size
            }
            MediaSourceType.JELLYFIN,
            MediaSourceType.EMBY -> {
                val api = createApi<JellyfinApi>(source)
                val remoteIds = jellyfinClient.fetchAllTrackIds(api, source)
                val localIds = trackDao.getTrackIdsBySource(source.id).toSet()
                val toRemove = localIds - remoteIds
                if (toRemove.isNotEmpty()) {
                    trackDao.deleteTracksByIds(source.id, toRemove.toList())
                }
                toRemove.size
            }
            MediaSourceType.NAVIDROME -> {
                val api = createApi<NavidromeApi>(source)
                val remoteIds = navidromeClient.fetchAllTrackIds(api, source)
                val localIds = trackDao.getTrackIdsBySource(source.id).toSet()
                val toRemove = localIds - remoteIds
                if (toRemove.isNotEmpty()) {
                    trackDao.deleteTracksByIds(source.id, toRemove.toList())
                }
                toRemove.size
            }
            else -> 0
        }

        val totalAfter = trackDao.getTrackIdsBySource(source.id).size
        return DeltaSyncResult(added = added, updated = updated, removed = removed, totalAfter = totalAfter)
    }

    /**
     * Delta payloads can be partial on some providers; preserve local metadata
     * (especially artwork/album fields) when incoming values are empty.
     */
    private fun mergeTrackMetadata(
        incoming: Track,
        existing: com.musicplayer.data.local.TrackEntity?
    ): Track {
        if (existing == null) return incoming

        return incoming.copy(
            title = incoming.title.ifBlank { existing.title },
            artist = incoming.artist.ifBlank { existing.artist },
            albumArtist = incoming.albumArtist.ifBlank { existing.albumArtist },
            album = incoming.album.ifBlank { existing.album },
            albumId = incoming.albumId.ifBlank { existing.albumId },
            genre = incoming.genre.ifBlank { existing.genre },
            uri = incoming.uri.ifBlank { existing.uri },
            artworkUri = incoming.artworkUri ?: existing.artworkUri,
            trackNumber = if (incoming.trackNumber != 0) incoming.trackNumber else existing.trackNumber,
            discNumber = if (incoming.discNumber != 0) incoming.discNumber else existing.discNumber,
            year = if (incoming.year != 0) incoming.year else existing.year,
            duration = if (incoming.duration != 0L) incoming.duration else existing.duration,
            bitrate = if (incoming.bitrate != 0) incoming.bitrate else existing.bitrate,
            sampleRate = if (incoming.sampleRate != 0) incoming.sampleRate else existing.sampleRate,
            fileSize = if (incoming.fileSize != 0L) incoming.fileSize else existing.fileSize,
            codec = incoming.codec.ifBlank { existing.codec },
            extension = incoming.extension.ifBlank { existing.extension },
            // Keep the best-known timestamp if server omitted it in delta payload.
            remoteUpdatedAt = if (incoming.remoteUpdatedAt != 0L) incoming.remoteUpdatedAt else existing.remoteUpdatedAt
        )
    }

    // ── Connection testing ────────────────────────────────────────────────────

    suspend fun testConnection(source: MediaSource): ConnectionTestResult {
        return when (source.type) {
            MediaSourceType.LOCAL -> ConnectionTestResult.Success
            MediaSourceType.JELLYFIN, MediaSourceType.EMBY -> testJellyfinConnection(source)
            MediaSourceType.PLEX -> testPlexConnection(source)
            MediaSourceType.SUBSONIC, MediaSourceType.OPEN_SUBSONIC, MediaSourceType.NAVIDROME -> testSubsonicConnection(source)
            else -> ConnectionTestResult.Error("Connection testing not supported for this source type")
        }
    }

    private suspend fun testJellyfinConnection(source: MediaSource): ConnectionTestResult {
        return try {
            val api = createApi<JellyfinApi>(source)
            val success = jellyfinClient.testConnection(api, source)
            if (success) ConnectionTestResult.Success
            else ConnectionTestResult.Error("Failed to authenticate with Jellyfin/Emby server")
        } catch (e: Exception) {
            Timber.e(e, "Jellyfin/Emby connection test failed for ${source.name}")
            ConnectionTestResult.Error(formatConnectionError(e))
        }
    }

    private suspend fun testPlexConnection(source: MediaSource): ConnectionTestResult {
        return try {
            val api = createApi<PlexApi>(source)
            val success = plexClient.testConnection(api, source)
            if (success) ConnectionTestResult.Success
            else ConnectionTestResult.Error("Failed to authenticate with Plex server")
        } catch (e: Exception) {
            Timber.e(e, "Plex connection test failed for ${source.name}")
            ConnectionTestResult.Error(formatConnectionError(e))
        }
    }

    private suspend fun testSubsonicConnection(source: MediaSource): ConnectionTestResult {
        return try {
            val api = createApi<SubsonicApi>(source)
            val (token, salt) = subsonicClient.generateToken(source.password)
            val response = api.ping(username = source.username, token = token, salt = salt)
            val body = response?.response
                ?: return ConnectionTestResult.Error("Empty response from server")
            body.error?.let { err ->
                return ConnectionTestResult.Error("${err.message} (code: ${err.code})")
            }
            if (body.status == "ok") ConnectionTestResult.Success
            else ConnectionTestResult.Error("Unexpected status: ${body.status}")
        } catch (e: Exception) {
            Timber.e(e, "Subsonic connection test failed for ${source.name}")
            ConnectionTestResult.Error(formatConnectionError(e))
        }
    }

    private fun formatConnectionError(e: Exception): String {
        val msg = e.message ?: "Connection failed"
        return when {
            msg.contains("Unable to resolve host") -> "Cannot reach server. Check the URL."
            msg.contains("connection refused", ignoreCase = true) -> "Connection refused. Is the server running?"
            msg.contains("401") -> "Authentication failed. Check your credentials."
            msg.contains("403") -> "Access denied. Check your credentials."
            msg.contains("cleartext") -> "HTTPS required. Use an HTTPS URL."
            msg.contains("JSON") -> "Invalid response from server."
            msg.contains("timeout", ignoreCase = true) -> "Connection timed out."
            else -> "Connection failed: $msg"
        }
    }
}
