package com.musicplayer.data.repository

import android.net.Uri
import com.musicplayer.data.local.ProfileDao
import com.musicplayer.data.local.ProfileTrackEntity
import com.musicplayer.domain.model.MediaSourceType
import com.musicplayer.domain.model.Track
import com.musicplayer.profile.ProfileManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that provides profile-isolated music data access.
 * All data returned by this repository is filtered by the currently selected profile.
 * This ensures that data is mutually exclusive across profiles.
 */
@Singleton
class ProfileMusicRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val profileManager: ProfileManager
) {
    /**
     * Get all tracks for the currently selected profile.
     * Returns an empty flow if no profile is selected.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTracksForCurrentProfile(): Flow<List<Track>> {
        return profileManager.selectedProfile.flatMapLatest { profile ->
            if (profile == null) {
                Timber.d("No profile selected, returning empty track list")
                flowOf(emptyList())
            } else {
                Timber.d("Fetching tracks for profile: ${profile.id}")
                profileDao.getTracksByProfileId(profile.id).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }
    }

    /**
     * Get all albums for the currently selected profile.
     * Returns distinct album names and their first track's info.
     */
    fun getAlbumsForCurrentProfile(): Flow<List<ProfileAlbum>> {
        return getTracksForCurrentProfile().map { tracks ->
            tracks.groupBy { it.album }
                .map { (albumName, albumTracks) ->
                    val firstTrack = albumTracks.firstOrNull()
                    ProfileAlbum(
                        name = albumName,
                        artworkUri = firstTrack?.artworkUri?.let { Uri.parse(it) },
                        artist = firstTrack?.artist ?: "Unknown Artist",
                        trackCount = albumTracks.size,
                        totalDuration = albumTracks.sumOf { it.duration }
                    )
                }
                .sortedBy { it.name.lowercase() }
        }
    }

    /**
     * Get all artists for the currently selected profile.
     */
    fun getArtistsForCurrentProfile(): Flow<List<ProfileArtist>> {
        return getTracksForCurrentProfile().map { tracks ->
            tracks.groupBy { it.artist }
                .map { (artistName, artistTracks) ->
                    val firstTrack = artistTracks.firstOrNull()
                    ProfileArtist(
                        name = artistName,
                        imageUri = firstTrack?.artworkUri?.let { Uri.parse(it) },
                        albumCount = artistTracks.groupBy { it.album }.size,
                        trackCount = artistTracks.size
                    )
                }
                .sortedBy { it.name.lowercase() }
        }
    }

    /**
     * Search tracks for the currently selected profile.
     */
    fun searchTracks(query: String): Flow<List<Track>> {
        return getTracksForCurrentProfile().map { tracks ->
            if (query.isBlank()) {
                tracks
            } else {
                val lowerQuery = query.lowercase()
                tracks.filter { track ->
                    track.title.lowercase().contains(lowerQuery) ||
                    track.artist.lowercase().contains(lowerQuery) ||
                    track.album.lowercase().contains(lowerQuery)
                }
            }
        }
    }

    /**
     * Search albums for the currently selected profile.
     */
    fun searchAlbums(query: String): Flow<List<ProfileAlbum>> {
        return getAlbumsForCurrentProfile().map { albums ->
            if (query.isBlank()) {
                albums
            } else {
                val lowerQuery = query.lowercase()
                albums.filter { album ->
                    album.name.lowercase().contains(lowerQuery) ||
                    album.artist.lowercase().contains(lowerQuery)
                }
            }
        }
    }

    /**
     * Search artists for the currently selected profile.
     */
    fun searchArtists(query: String): Flow<List<ProfileArtist>> {
        return getArtistsForCurrentProfile().map { artists ->
            if (query.isBlank()) {
                artists
            } else {
                val lowerQuery = query.lowercase()
                artists.filter { artist ->
                    artist.name.lowercase().contains(lowerQuery)
                }
            }
        }
    }

    /**
     * Get tracks for a specific album in the current profile.
     */
    fun getTracksForAlbum(albumName: String): Flow<List<Track>> {
        return getTracksForCurrentProfile().map { tracks ->
            tracks.filter { it.album == albumName }
                .sortedWith(compareBy({ it.discNumber }, { it.trackNumber }))
        }
    }

    /**
     * Get tracks for a specific artist in the current profile.
     */
    fun getTracksForArtist(artistName: String): Flow<List<Track>> {
        return getTracksForCurrentProfile().map { tracks ->
            tracks.filter { it.artist == artistName }
                .sortedWith(compareBy({ it.album }, { it.discNumber }, { it.trackNumber }))
        }
    }

    /**
     * Get a specific track by ID.
     */
    fun getTrackById(trackId: String): Flow<Track?> {
        return getTracksForCurrentProfile().map { tracks ->
            tracks.find { it.id == trackId }
        }
    }

    /**
     * Get track count for the current profile.
     */
    fun getTrackCount(): Flow<Int> {
        return getTracksForCurrentProfile().map { tracks -> tracks.size }
    }

    /**
     * Save tracks to the current profile.
     */
    suspend fun saveTracks(profileId: String, sourceType: MediaSourceType, tracks: List<Track>) {
        val sourceTypeStr = sourceType.name
        val entities = tracks.map { track ->
            val existing = profileDao.getTrackByRemoteId(profileId, track.id)
            ProfileTrackEntity(
                id = "${profileId}_${track.id}",
                profileId = profileId,
                remoteId = track.id,
                title = track.title.ifBlank { existing?.title ?: "" },
                artist = track.artist.ifBlank { existing?.artist ?: "" },
                album = track.album.ifBlank { existing?.album ?: "" },
                albumArtist = track.albumArtist.ifBlank { existing?.albumArtist ?: "" },
                albumId = track.albumId.ifBlank { existing?.albumId ?: "" },
                duration = if (track.duration != 0L) track.duration else (existing?.duration ?: 0L),
                trackNumber = if (track.trackNumber != 0) track.trackNumber else (existing?.trackNumber ?: 0),
                discNumber = if (track.discNumber != 0) track.discNumber else (existing?.discNumber ?: 1),
                year = if (track.year != 0) track.year else (existing?.year ?: 0),
                genre = track.genre.ifBlank { existing?.genre ?: "" },
                artworkUri = track.artworkUri ?: existing?.artworkUri,
                bitrate = if (track.bitrate != 0) track.bitrate else (existing?.bitrate ?: 0),
                sampleRate = if (track.sampleRate != 0) track.sampleRate else (existing?.sampleRate ?: 0),
                fileSize = if (track.fileSize != 0L) track.fileSize else (existing?.fileSize ?: 0L),
                codec = track.codec.ifBlank { existing?.codec ?: "" },
                streamUri = track.uri.ifBlank { existing?.streamUri ?: "" },
                sourceType = sourceTypeStr,
                remoteUpdatedAt = if (track.remoteUpdatedAt != 0L) track.remoteUpdatedAt else (existing?.remoteUpdatedAt ?: 0L)
            )
        }
        profileDao.insertTracks(entities)
        Timber.d("Saved ${entities.size} tracks for profile $profileId")
    }

    // ── Delta sync ─────────────────────────────────────────────────────────────

    /**
     * Summary of a profile delta sync run.
     */
    data class DeltaSyncResult(
        val added: Int,
        val updated: Int,
        val removed: Int,
        val totalAfter: Int
    )

    /**
     * Performs a delta sync for the given [profile]: fetches only the tracks
     * that have changed since [Profile.lastDeltaSyncAt], diffs them against
     * the local cache, and applies the changes (insert / update / delete).
     *
     * Falls back to a full sync if:
     * - A full sync has never been completed for this profile.
     * - The remote client can't return changed items.
     *
     * Updates [Profile.lastDeltaSyncAt] (and `lastFullSyncAt` when a fallback
     * occurs) via the [profileRepository].
     */
    suspend fun deltaSyncProfile(
        profile: com.musicplayer.profile.Profile,
        changedTracks: List<Track>,
        remoteIdsSnapshot: Set<String>? = null
    ): DeltaSyncResult {
        val profileId = profile.id
        val now = System.currentTimeMillis()

        // If we have never done a full sync, we have nothing to diff against — fall back.
        if (profile.lastFullSyncAt <= 0L) {
            Timber.d("Delta sync: no full sync on record for profile ${profile.name}, falling back to full sync")
            // The caller is expected to perform a full sync separately when this happens.
            profileDao.updateLastDeltaSyncTime(profileId, now)
            profileDao.updateLastFullSyncTime(profileId, now)
            return DeltaSyncResult(added = changedTracks.size, updated = 0, removed = 0, totalAfter = changedTracks.size)
        }

        val changedIds = changedTracks.map { it.id }.toSet()
        var added = 0
        var updated = 0

        if (changedTracks.isNotEmpty()) {
            val existing = profileDao.getTrackTimestampsByProfile(profileId)
                .associate { it.remoteId to it.remoteUpdatedAt }
            changedTracks.forEach { track ->
                if (existing.containsKey(track.id)) updated++ else added++
            }
            saveTracks(profileId, profile.serviceType.toMediaSourceType(), changedTracks)
        }

        val totalAfter = profileDao.getTrackCountForProfile(profileId)

        // Removal detection:
        // - Subsonic/OpenSubsonic: changedTracks is a full inventory when changed.
        // - Navidrome/Jellyfin/Emby: use a full remote ID snapshot from worker.
        val removed = when (profile.serviceType) {
            com.musicplayer.profile.MediaServiceType.Subsonic,
            com.musicplayer.profile.MediaServiceType.OpenSubsonic -> {
                val localIds = profileDao.getRemoteIdsByProfile(profileId).toSet()
                val toRemove = localIds - changedIds
                if (toRemove.isNotEmpty()) {
                    profileDao.deleteTracksByRemoteIds(profileId, toRemove.toList())
                }
                toRemove.size
            }
            com.musicplayer.profile.MediaServiceType.Navidrome,
            com.musicplayer.profile.MediaServiceType.Jellyfin,
            com.musicplayer.profile.MediaServiceType.Emby -> {
                val snapshot = remoteIdsSnapshot ?: emptySet()
                if (snapshot.isEmpty()) {
                    0
                } else {
                    val localIds = profileDao.getRemoteIdsByProfile(profileId).toSet()
                    val toRemove = localIds - snapshot
                    if (toRemove.isNotEmpty()) {
                        profileDao.deleteTracksByRemoteIds(profileId, toRemove.toList())
                    }
                    toRemove.size
                }
            }
            else -> 0
        }

        profileDao.updateLastDeltaSyncTime(profileId, now)
        Timber.d("Delta sync for profile ${profile.name}: +$added ~$updated -$removed")
        return DeltaSyncResult(added = added, updated = updated, removed = removed, totalAfter = totalAfter)
    }

    /**
     * Convert [MediaServiceType] to [MediaSourceType] for the [saveTracks] call.
     */
    private fun com.musicplayer.profile.MediaServiceType.toMediaSourceType(): MediaSourceType {
        return when (this) {
            com.musicplayer.profile.MediaServiceType.Jellyfin -> MediaSourceType.JELLYFIN
            com.musicplayer.profile.MediaServiceType.Emby -> MediaSourceType.EMBY
            com.musicplayer.profile.MediaServiceType.Plex -> MediaSourceType.PLEX
            com.musicplayer.profile.MediaServiceType.Subsonic -> MediaSourceType.SUBSONIC
            com.musicplayer.profile.MediaServiceType.OpenSubsonic -> MediaSourceType.OPEN_SUBSONIC
            com.musicplayer.profile.MediaServiceType.Navidrome -> MediaSourceType.NAVIDROME
        }
    }

    /**
     * Clear all tracks for a profile.
     */
    suspend fun clearTracksForProfile(profileId: String) {
        profileDao.deleteTracksByProfileId(profileId)
        Timber.d("Cleared all tracks for profile $profileId")
    }

    /**
     * Get tracks with a specific genre.
     */
    fun getTracksForGenre(genre: String): Flow<List<Track>> {
        return getTracksForCurrentProfile().map { tracks ->
            tracks.filter { it.genre.equals(genre, ignoreCase = true) }
        }
    }

    /**
     * Convert ProfileTrackEntity to domain Track model.
     */
    private fun ProfileTrackEntity.toDomain(): Track {
        val parsedSourceType = try {
            MediaSourceType.valueOf(sourceType)
        } catch (e: Exception) {
            MediaSourceType.USER
        }
        return Track(
            id = remoteId,
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            albumId = albumId,
            duration = duration,
            trackNumber = trackNumber,
            discNumber = discNumber,
            year = year,
            genre = genre,
            artworkUri = artworkUri,
            bitrate = bitrate,
            sampleRate = sampleRate,
            fileSize = fileSize,
            codec = codec,
            uri = streamUri,
            sourceId = profileId,
            sourceName = "Profile",
            sourceType = parsedSourceType,
            remoteUpdatedAt = remoteUpdatedAt
        )
    }
}

/**
 * Domain model for a profile-specific album.
 */
data class ProfileAlbum(
    val name: String,
    val artworkUri: Uri?,
    val artist: String,
    val trackCount: Int,
    val totalDuration: Long
)

/**
 * Domain model for a profile-specific artist.
 */
data class ProfileArtist(
    val name: String,
    val imageUri: Uri?,
    val albumCount: Int,
    val trackCount: Int
)