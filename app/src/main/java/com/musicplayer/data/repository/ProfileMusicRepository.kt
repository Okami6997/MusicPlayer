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
    suspend fun saveTracks(profileId: String, tracks: List<Track>) {
        val entities = tracks.map { track ->
            ProfileTrackEntity(
                id = "${profileId}_${track.id}",
                profileId = profileId,
                remoteId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                albumArtist = track.albumArtist,
                albumId = track.albumId,
                duration = track.duration,
                trackNumber = track.trackNumber,
                discNumber = track.discNumber,
                year = track.year,
                genre = track.genre,
                artworkUri = track.artworkUri,
                bitrate = track.bitrate,
                sampleRate = track.sampleRate,
                fileSize = track.fileSize,
                codec = track.codec,
                streamUri = track.uri
            )
        }
        profileDao.insertTracks(entities)
        Timber.d("Saved ${entities.size} tracks for profile $profileId")
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
            sourceType = MediaSourceType.USER
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