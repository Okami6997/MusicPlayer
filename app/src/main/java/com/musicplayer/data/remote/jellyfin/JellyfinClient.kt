package com.musicplayer.data.remote.jellyfin

import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.Track
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds Jellyfin/Emby compatible URLs and converts API responses to domain models.
 * Both Jellyfin and Emby share the same API structure.
 */
@Singleton
class JellyfinClient @Inject constructor() {

    /** Authorization header value for API calls */
    fun buildAuthHeader(token: String, deviceId: String = "MusicPlayer"): String =
        "MediaBrowser Client=\"MusicPlayer\", Device=\"Android\", DeviceId=\"$deviceId\", Version=\"1.0.0\", Token=\"$token\""

    /** Stream URL for a given [itemId] */
    fun buildStreamUrl(baseUrl: String, itemId: String, token: String, container: String = "mp3"): String =
        "$baseUrl/Audio/$itemId/universal?UserId=dummy&DeviceId=MusicPlayer&MaxStreamingBitrate=140000000&api_key=$token&PlaySessionId=MusicPlayer&Container=opus,mp3,aac,flac,webma,webm,wav,ogg&TranscodingContainer=aac&TranscodingProtocol=hls&AudioCodec=aac&EnableRedirectOnDirectStreamUrl=true"

    /** Artwork URL for a given [itemId] */
    fun buildArtworkUrl(baseUrl: String, itemId: String, imageType: String = "Primary"): String =
        "$baseUrl/Items/$itemId/Images/$imageType?maxWidth=300&quality=90"

    /**
     * Fetches all audio tracks from a Jellyfin/Emby server.
     */
    suspend fun fetchAllTracks(api: JellyfinApi, source: MediaSource): List<Track> {
        val tracks = mutableListOf<Track>()
        var startIndex = 0
        val pageSize = 500

        while (true) {
            Timber.d("Fetching Jellyfin items startIndex=$startIndex limit=$pageSize")
            val response = api.getItems(
                token = source.token,
                startIndex = startIndex,
                limit = pageSize
            )
            val items = response.items
            if (items.isEmpty()) break

            Timber.d("Got ${items.size} items (total: ${response.totalRecordCount})")
            for (item in items) {
                tracks.add(item.toTrack(source))
            }

            if (items.size < pageSize) break
            startIndex += pageSize
        }

        Timber.d("fetchAllTracks complete: ${tracks.size} total tracks from Jellyfin")
        return tracks
    }

    /**
     * Tests connectivity to a Jellyfin/Emby server by hitting the public info endpoint.
     */
    suspend fun testConnection(api: JellyfinApi, source: MediaSource): Boolean {
        return try {
            val info = api.getPublicInfo()
            Timber.d("Jellyfin server: ${info.serverName} v${info.version}")

            // Also verify API key by fetching items with limit 1
            val items = api.getItems(token = source.token, limit = 1)
            Timber.d("Jellyfin auth OK, total items: ${items.totalRecordCount}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Jellyfin connection test failed")
            false
        }
    }

    /**
     * Convert a [JellyfinItem] (of type "Audio") to a [Track].
     */
    fun JellyfinItem.toTrack(source: MediaSource): Track {
        val artworkUri = if (imageTags.containsKey("Primary")) {
            buildArtworkUrl(source.baseUrl, id)
        } else if (albumId.isNotEmpty()) {
            buildArtworkUrl(source.baseUrl, albumId)
        } else {
            null
        }

        val mediaSource = mediaSources.firstOrNull()
        return Track(
            id = "${source.id}_$id",
            title = name,
            artist = artistItems.firstOrNull()?.name ?: albumArtist,
            albumArtist = albumArtist,
            album = album,
            albumId = albumId,
            duration = runTimeTicks / 10_000L, // 100ns ticks -> ms
            trackNumber = indexNumber,
            discNumber = parentIndexNumber,
            year = productionYear,
            genre = genres.firstOrNull() ?: "",
            uri = buildStreamUrl(source.baseUrl, id, source.token),
            artworkUri = artworkUri,
            sourceId = source.id,
            sourceName = source.name,
            sourceType = source.type,
            bitrate = mediaSource?.bitrate ?: 0,
            fileSize = mediaSource?.size ?: 0L,
            codec = mediaSource?.container ?: "",
            remoteUpdatedAt = parseJellyfinDate(dateModified)
        )
    }

    /**
     * Parses a Jellyfin/ISO-8601 date string into epoch millis.
     * Returns 0 if the string is null or unparseable.
     */
    private fun parseJellyfinDate(date: String?): Long {
        if (date.isNullOrBlank()) return 0L
        return try {
            java.time.Instant.parse(date).toEpochMilli()
        } catch (e: Exception) {
            try {
                java.time.OffsetDateTime.parse(date).toInstant().toEpochMilli()
            } catch (e: Exception) {
                try {
                    java.time.LocalDateTime.parse(date)
                        .atZone(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                } catch (_: Exception) {
                    0L
                }
            }
        }
    }

    /**
     * Fetches only the items that were created or modified since [sinceEpochMillis].
     * Uses Jellyfin's `SortBy=DateModified,SortOrder=Descending` and walks pagination.
     *
     * Returns the fetched items as [Track]s. Each track carries its server-side
     * [Track.remoteUpdatedAt] so the repository can diff against the local cache.
     */
    suspend fun fetchChangedTracks(
        api: JellyfinApi,
        source: MediaSource,
        sinceEpochMillis: Long
    ): List<Track> {
        val tracks = mutableListOf<Track>()
        var startIndex = 0
        val pageSize = 250
        val minDateLastSaved = if (sinceEpochMillis > 0L) {
            java.time.Instant.ofEpochMilli(sinceEpochMillis)
                .toString()
        } else {
            null
        }

        // Use server-side min-date filtering for correctness. Some servers/plugins may
        // not guarantee strict DateModified ordering, so avoid client-side cutoff breaks.
        while (true) {
            Timber.d("Delta Jellyfin: fetching startIndex=$startIndex limit=$pageSize")
            val response = api.getItems(
                token = source.token,
                startIndex = startIndex,
                limit = pageSize,
                sortBy = "DateModified",
                sortOrder = "Descending",
                minDateLastSaved = minDateLastSaved
            )
            val items = response.items
            if (items.isEmpty()) break

            for (item in items) {
                tracks.add(item.toTrack(source))
            }
            if (items.size < pageSize) break
            startIndex += pageSize
        }

        Timber.d("Delta Jellyfin: fetched ${tracks.size} changed tracks")
        return tracks
    }

    /**
     * Fetches the full set of remote track IDs for reconciliation (deletion detection).
     * Keeps payload lighter by avoiding extra fields.
     */
    suspend fun fetchAllTrackIds(api: JellyfinApi, source: MediaSource): Set<String> {
        val ids = mutableSetOf<String>()
        var startIndex = 0
        val pageSize = 500

        while (true) {
            val response = api.getItems(
                token = source.token,
                fields = "",
                startIndex = startIndex,
                limit = pageSize,
                sortBy = "SortName",
                sortOrder = "Ascending"
            )
            val items = response.items
            if (items.isEmpty()) break

            items.forEach { item ->
                ids.add("${source.id}_${item.id}")
            }

            if (items.size < pageSize) break
            startIndex += pageSize
        }

        return ids
    }
}
