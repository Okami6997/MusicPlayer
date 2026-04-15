package com.musicplayer.data.remote.subsonic

import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.MediaSourceType
import com.musicplayer.domain.model.Track
import timber.log.Timber
import java.math.BigInteger
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for Subsonic-compatible APIs (Subsonic, OpenSubsonic, Navidrome).
 */
@Singleton
class SubsonicClient @Inject constructor() {

    /**
     * Fetches all songs for a [source] by iterating through all albums.
     */
    suspend fun fetchAllTracks(api: SubsonicApi, source: MediaSource): List<Track> {
        val (token, salt) = generateToken(source.password)
        val tracks = mutableListOf<Track>()
        var offset = 0
        val pageSize = 500

        try {
            while (true) {
                val response = api.getAlbumList(
                    username = source.username,
                    token = token,
                    salt = salt,
                    offset = offset,
                    size = pageSize
                )
                val albums = response.response.albumList2?.album ?: break
                if (albums.isEmpty()) break

                for (album in albums) {
                    val albumResp = api.getAlbum(
                        albumId = album.id,
                        username = source.username,
                        token = token,
                        salt = salt
                    )
                    albumResp.response.album?.song?.forEach { song ->
                        tracks.add(song.toTrack(source))
                    }
                }

                if (albums.size < pageSize) break
                offset += pageSize
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching tracks from Subsonic source: ${source.name}")
        }

        return tracks
    }

    /**
     * Builds the streaming URL for a song.
     */
    fun buildStreamUrl(baseUrl: String, songId: String, username: String, password: String): String {
        val (token, salt) = generateToken(password)
        return "$baseUrl/rest/stream?id=$songId&u=$username&t=$token&s=$salt&v=1.16.1&c=MusicPlayer"
    }

    /**
     * Builds the cover art URL for a given coverArt id.
     */
    fun buildCoverArtUrl(baseUrl: String, coverArtId: String, username: String, password: String): String {
        val (token, salt) = generateToken(password)
        return "$baseUrl/rest/getCoverArt?id=$coverArtId&u=$username&t=$token&s=$salt&v=1.16.1&c=MusicPlayer&size=300"
    }

    /**
     * Generates a Subsonic token and salt pair from a plain-text password.
     */
    fun generateToken(password: String): Pair<String, String> {
        val salt = UUID.randomUUID().toString().replace("-", "").take(16)
        val md5 = MessageDigest.getInstance("MD5")
        val token = BigInteger(1, md5.digest("$password$salt".toByteArray()))
            .toString(16).padStart(32, '0')
        return token to salt
    }

    private fun SubsonicSong.toTrack(source: MediaSource): Track {
        val artworkUri = coverArt?.let {
            buildCoverArtUrl(source.baseUrl, it, source.username, source.password)
        }
        return Track(
            id = "${source.id}_$id",
            title = title,
            artist = artist,
            albumArtist = artist,
            album = album,
            albumId = albumId,
            duration = duration * 1000L,
            trackNumber = track,
            discNumber = discNumber,
            year = year,
            genre = genre,
            uri = buildStreamUrl(source.baseUrl, id, source.username, source.password),
            artworkUri = artworkUri,
            sourceId = source.id,
            sourceType = source.type,
            bitrate = bitRate
        )
    }
}
