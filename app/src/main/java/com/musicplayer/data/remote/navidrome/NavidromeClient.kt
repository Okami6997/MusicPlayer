package com.musicplayer.data.remote.navidrome

import com.musicplayer.data.remote.subsonic.SubsonicClient
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.Track
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for the Navidrome native REST API.
 * Uses JWT Bearer token auth and paginated endpoints for efficient sync.
 */
@Singleton
class NavidromeClient @Inject constructor(
    private val subsonicClient: SubsonicClient
) {

    suspend fun fetchAllTracks(api: NavidromeApi, source: MediaSource): List<Track> {
        val token = authenticate(api, source)
        val bearer = "Bearer $token"

        val allTracks = mutableListOf<Track>()
        var start = 0
        val pageSize = 500

        Timber.d("Navidrome sync: fetching songs...")
        while (true) {
            val response = api.getSongs(
                authorization = bearer,
                start = start,
                end = start + pageSize
            )
            val totalCount = response.headers()["X-Total-Count"]?.toIntOrNull() ?: 0
            val songs = response.body() ?: emptyList()

            if (songs.isEmpty()) break

            songs.forEach { song ->
                allTracks.add(song.toTrack(source))
            }

            start += songs.size
            Timber.d("Navidrome sync: fetched $start / $totalCount songs")

            if (start >= totalCount) break
        }

        Timber.d("Navidrome sync: complete, ${allTracks.size} total tracks")
        return allTracks
    }

    private suspend fun authenticate(api: NavidromeApi, source: MediaSource): String {
        Timber.d("Navidrome: authenticating as ${source.username}")
        val loginResponse = api.login(
            NavidromeLoginRequest(
                username = source.username,
                password = source.password
            )
        )
        return loginResponse.token
    }

    private fun NavidromeSong.toTrack(source: MediaSource): Track {
        return Track(
            id = "${source.id}_$id",
            title = title ?: "Unknown Title",
            artist = artist ?: "Unknown Artist",
            albumArtist = albumArtist ?: artist ?: "Unknown Artist",
            album = album ?: "Unknown Album",
            albumId = albumId ?: "",
            duration = ((duration ?: 0.0) * 1000).toLong(),
            trackNumber = trackNumber ?: 0,
            discNumber = discNumber ?: 1,
            year = year ?: 0,
            genre = genre ?: genres?.firstOrNull()?.name ?: "Unknown",
            uri = subsonicClient.buildStreamUrl(source.baseUrl, id, source.username, source.password),
            artworkUri = coverArtId?.let {
                subsonicClient.buildCoverArtUrl(source.baseUrl, it, source.username, source.password)
            },
            sourceId = source.id,
            sourceName = source.name,
            sourceType = source.type,
            bitrate = bitRate ?: 0,
            sampleRate = sampleRate ?: 0,
            fileSize = size ?: 0L,
            codec = suffix ?: ""
        )
    }
}
