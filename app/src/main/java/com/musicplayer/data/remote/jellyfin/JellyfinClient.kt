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
