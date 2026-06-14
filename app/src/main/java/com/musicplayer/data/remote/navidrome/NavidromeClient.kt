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
            val end = start + pageSize - 1
            val response = api.getSongs(
                authorization = bearer,
                start = start,
                end = end,
                sort = "updated_at",
                order = "DESC"
            )
            val totalCount = response.headers()["X-Total-Count"]?.toIntOrNull() ?: 0
            val songs = response.body() ?: emptyList()

            if (songs.isEmpty()) break

            songs.forEach { song ->
                allTracks.add(song.toTrack(source))
            }

            start += songs.size
            Timber.d("Navidrome sync: fetched $start / $totalCount songs")

            // Prefer total-count when present, but still handle servers that omit it.
            if (totalCount > 0 && start >= totalCount) break
            if (songs.size < pageSize) break
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
        val artworkId = coverArtId ?: albumId ?: id
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
            artworkUri = artworkId?.let {
                subsonicClient.buildCoverArtUrl(source.baseUrl, it, source.username, source.password)
            },
            sourceId = source.id,
            sourceName = source.name,
            sourceType = source.type,
            bitrate = bitRate ?: 0,
            sampleRate = sampleRate ?: 0,
            fileSize = size ?: 0L,
            codec = suffix ?: "",
            // updatedAt is an ISO-8601 string in Navidrome; parse it to epoch millis.
            remoteUpdatedAt = parseNavidromeDate(updatedAt)
        )
    }

    /**
     * Parses a Navidrome/ISO-8601 date string into epoch millis.
     * Returns 0 if the string is null or unparseable.
     */
    private fun parseNavidromeDate(date: String?): Long {
        if (date.isNullOrBlank()) return 0L
        return try {
            val instant = java.time.Instant.parse(date)
            instant.toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Fetches only the songs that were created or updated after [sinceEpochMillis].
     * Walks the Navidrome `getSongs` endpoint in pages of [pageSize] and stops as
     * soon as it reaches a song older than the cutoff (the endpoint returns
     * songs in insertion order — we sort by `updatedAt` descending client-side
     * by relying on the server's `_sort=updated_at`).
     */
    suspend fun fetchChangedTracks(
        api: NavidromeApi,
        source: MediaSource,
        sinceEpochMillis: Long,
        pageSize: Int = 500
    ): List<Track> {
        val token = authenticate(api, source)
        val bearer = "Bearer $token"

        val allTracks = mutableListOf<Track>()
        var start = 0

        while (true) {
            val end = start + pageSize - 1
            val response = api.getSongs(
                authorization = bearer,
                start = start,
                end = end,
                sort = "updated_at",
                order = "DESC"
            )
            val totalCount = response.headers()["X-Total-Count"]?.toIntOrNull() ?: 0
            val songs = response.body() ?: emptyList()
            if (songs.isEmpty()) break

            var hitCutoff = false
            for (song in songs) {
                val updated = parseNavidromeDate(song.updatedAt)
                if (sinceEpochMillis > 0L && updated in 1..sinceEpochMillis) {
                    hitCutoff = true
                    break
                }
                allTracks.add(song.toTrack(source))
            }
            if (hitCutoff) break
            start += songs.size
            if (totalCount > 0 && start >= totalCount) break
            if (songs.size < pageSize) break
        }

        Timber.d("Delta Navidrome: fetched ${allTracks.size} changed tracks")
        return allTracks
    }

    /**
     * Fetches the full set of remote track IDs for reconciliation (deletion detection).
     */
    suspend fun fetchAllTrackIds(api: NavidromeApi, source: MediaSource): Set<String> {
        val token = authenticate(api, source)
        val bearer = "Bearer $token"

        val ids = mutableSetOf<String>()
        var start = 0
        val pageSize = 500

        while (true) {
            val end = start + pageSize - 1
            val response = api.getSongs(
                authorization = bearer,
                start = start,
                end = end,
                sort = "id",
                order = "ASC"
            )
            val totalCount = response.headers()["X-Total-Count"]?.toIntOrNull() ?: 0
            val songs = response.body() ?: emptyList()
            if (songs.isEmpty()) break

            songs.forEach { song ->
                ids.add("${source.id}_${song.id}")
            }

            start += songs.size
            if (totalCount > 0 && start >= totalCount) break
            if (songs.size < pageSize) break
        }

        return ids
    }
}
