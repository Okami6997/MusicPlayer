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
            sourceType = source.type,
            bitrate = mediaSource?.bitrate ?: 0,
            fileSize = mediaSource?.size ?: 0L,
            codec = mediaSource?.container ?: ""
        )
    }
}
