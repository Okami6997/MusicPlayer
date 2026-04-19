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

        while (true) {
            Timber.d("Fetching album list offset=$offset size=$pageSize")
            val response = api.getAlbumList(
                username = source.username,
                token = token,
                salt = salt,
                offset = offset,
                size = pageSize
            )

            val body = response.response
            body.error?.let { error ->
                throw RuntimeException("Subsonic API error ${error.code}: ${error.message}")
            }

            val albums = body.albumList2?.album
            if (albums.isNullOrEmpty()) {
                Timber.d("No more albums at offset=$offset")
                break
            }

            Timber.d("Got ${albums.size} albums at offset=$offset")

            for (album in albums) {
                try {
                    val albumResp = api.getAlbum(
                        albumId = album.id,
                        username = source.username,
                        token = token,
                        salt = salt
                    )
                    albumResp.response.error?.let { error ->
                        Timber.w("Error fetching album ${album.id}: ${error.message}")
                        return@let
                    }
                    val songs = albumResp.response.album?.song ?: emptyList()
                    Timber.d("Album '${album.name}': ${songs.size} songs")
                    songs.forEach { song -> tracks.add(song.toTrack(source)) }
                } catch (e: Exception) {
                    Timber.w(e, "Skipping album ${album.id} (${album.name}): ${e.message}")
                }
            }

            if (albums.size < pageSize) break
            offset += pageSize
        }

        Timber.d("fetchAllTracks complete: ${tracks.size} total tracks")
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
