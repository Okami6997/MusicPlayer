package com.musicplayer.data.remote.plex

import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.Track
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for Plex Media Server audio library.
 */
@Singleton
class PlexClient @Inject constructor() {

    /**
     * Fetches all audio tracks from all music libraries on a Plex server.
     */
    suspend fun fetchAllTracks(api: PlexApi, source: MediaSource): List<Track> {
        val tracks = mutableListOf<Track>()

        // 1. Find music library sections
        val sections = api.getLibrarySections(token = source.token)
        val musicSections = sections.mediaContainer.directories.filter {
            it.type == "artist"
        }

        if (musicSections.isEmpty()) {
            Timber.w("No music libraries found on Plex server: ${source.name}")
            return emptyList()
        }

        // 2. Paginate through each music library
        for (section in musicSections) {
            var start = 0
            val pageSize = 500

            while (true) {
                Timber.d("Fetching Plex section '${section.title}' start=$start size=$pageSize")
                val response = api.getSectionItems(
                    token = source.token,
                    sectionKey = section.key,
                    start = start,
                    size = pageSize
                )
                val items = response.mediaContainer.metadata
                if (items.isEmpty()) break

                Timber.d("Got ${items.size} items from Plex section '${section.title}'")
                for (item in items) {
                    tracks.add(item.toTrack(source))
                }

                if (items.size < pageSize) break
                start += pageSize
            }
        }

        Timber.d("fetchAllTracks complete: ${tracks.size} total tracks from Plex")
        return tracks
    }

    /**
     * Tests connectivity by hitting the server root.
     */
    suspend fun testConnection(api: PlexApi, source: MediaSource): Boolean {
        return try {
            val info = api.getServerInfo(token = source.token)
            Timber.d("Plex server: ${info.mediaContainer.machineIdentifier} v${info.mediaContainer.version}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Plex connection test failed")
            false
        }
    }

    fun buildStreamUrl(baseUrl: String, partKey: String, token: String): String =
        "$baseUrl$partKey?X-Plex-Token=$token"

    fun buildArtworkUrl(baseUrl: String, thumb: String, token: String): String =
        "$baseUrl$thumb?X-Plex-Token=$token"

    private fun PlexMetadata.toTrack(source: MediaSource): Track {
        val partKey = media.firstOrNull()?.parts?.firstOrNull()?.key ?: ""
        val thumbPath = thumb.ifEmpty { parentThumb.ifEmpty { grandparentThumb } }
        val artworkUri = if (thumbPath.isNotEmpty()) {
            buildArtworkUrl(source.baseUrl, thumbPath, source.token)
        } else null

        val mediaInfo = media.firstOrNull()
        return Track(
            id = "${source.id}_$ratingKey",
            title = title,
            artist = grandparentTitle,
            albumArtist = grandparentTitle,
            album = parentTitle,
            albumId = parentRatingKey,
            duration = duration,  // Plex duration is already in ms
            trackNumber = index,
            discNumber = parentIndex,
            year = year,
            genre = "",
            uri = buildStreamUrl(source.baseUrl, partKey, source.token),
            artworkUri = artworkUri,
            sourceId = source.id,
            sourceName = source.name,
            sourceType = source.type,
            bitrate = mediaInfo?.bitrate ?: 0,
            fileSize = mediaInfo?.parts?.firstOrNull()?.size ?: 0L,
            codec = mediaInfo?.container ?: "",
            // Plex's `updatedAt` is epoch seconds, convert to millis.
            remoteUpdatedAt = if (updatedAt > 0L) updatedAt * 1000L else 0L
        )
    }

    /**
     * Fetches only the items that were updated after [sinceEpochMillis].
     * Walks Plex's paginated section endpoint and stops paging as soon as the
     * server returns items older than the cutoff (the API returns items in
     * newest-first order when `sort` is provided — Plex does not, so we just
     * fetch a single recent page and filter in memory).
     */
    suspend fun fetchChangedTracks(
        api: PlexApi,
        source: MediaSource,
        sinceEpochMillis: Long
    ): List<Track> {
        val tracks = mutableListOf<Track>()

        val sections = api.getLibrarySections(token = source.token)
        val musicSections = sections.mediaContainer.directories.filter { it.type == "artist" }
        if (musicSections.isEmpty()) return emptyList()

        for (section in musicSections) {
            var start = 0
            val pageSize = 500
            // Walk the most recent N items only — Plex doesn't expose a date filter,
            // so we cap the work at a reasonable page count to keep delta sync cheap.
            val maxPages = 4

            var page = 0
            while (page < maxPages) {
                val response = api.getSectionItems(
                    token = source.token,
                    sectionKey = section.key,
                    start = start,
                    size = pageSize
                )
                val items = response.mediaContainer.metadata
                if (items.isEmpty()) break

                for (item in items) {
                    val itemMillis = if (item.updatedAt > 0L) item.updatedAt * 1000L else 0L
                    if (sinceEpochMillis > 0L && itemMillis in 1..sinceEpochMillis) {
                        // Stop paging this section — we hit the cutoff.
                        return tracks
                    }
                    tracks.add(item.toTrack(source))
                }
                if (items.size < pageSize) break
                start += pageSize
                page++
            }
        }

        Timber.d("Delta Plex: fetched ${tracks.size} changed tracks")
        return tracks
    }
}
