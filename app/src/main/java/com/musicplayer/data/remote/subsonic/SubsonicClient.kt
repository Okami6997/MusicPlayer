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
     * Fetches all songs for a [source] by crawling the artist -> album -> song hierarchy.
     */
    suspend fun fetchAllTracks(api: SubsonicApi, source: MediaSource): List<Track> {
        val (token, salt) = generateToken(source.password)
        val allTracks = mutableMapOf<String, Track>()

        try {
            // 1. Aggressive Search Fallback (including search2/3)
            Timber.d("Sync: Attempting aggressive wildcard search...")
            val searchTerms = mutableListOf("*", "")
            // If we still have few tracks, we'll try more specific queries later
            
            for (query in searchTerms) {
                try {
                    val searchResp = api.search(
                        query = query,
                        username = source.username,
                        token = token,
                        salt = salt,
                        songCount = 10000
                    )
                    
                    // Check searchResult3, searchResult2, and searchResult
                    searchResp.response.searchResult3?.song?.forEach { addSong(it, source, allTracks) }
                    searchResp.response.searchResult2?.song?.forEach { addSong(it, source, allTracks) }
                    searchResp.response.searchResult?.song?.forEach { addSong(it, source, allTracks) }
                } catch (e: Exception) {
                    Timber.w(e, "Sync: Wildcard search query '$query' failed")
                }
            }
            Timber.d("Sync: After wildcard search, total tracks: ${allTracks.size}")

            // 2. getIndexes (catches loose tracks at the root)
            Timber.d("Sync: Attempting getIndexes...")
            try {
                val indexesResp = api.getIndexes(source.username, token, salt)
                indexesResp.response.indexes?.child?.forEach { child ->
                    if (!child.isDir) addSong(child, source, allTracks)
                }
            } catch (e: Exception) {
                Timber.w(e, "Sync: getIndexes failed")
            }
            Timber.d("Sync: After getIndexes, total tracks: ${allTracks.size}")

            // 3. getRandomSongs (High volume fetch)
            Timber.d("Sync: Attempting getRandomSongs (multiple passes)...")
            repeat(3) { pass ->
                try {
                    val randomResp = api.getRandomSongs(source.username, token, salt, size = 500)
                    randomResp.response.randomSongs?.song?.forEach { addSong(it, source, allTracks) }
                } catch (e: Exception) {
                    Timber.w(e, "Sync: getRandomSongs pass $pass failed")
                }
            }
            Timber.d("Sync: After random songs, total tracks: ${allTracks.size}")

            // 4. getSongsByGenre
            Timber.d("Sync: Crawling by genre...")
            try {
                val genresResp = api.getGenres(source.username, token, salt)
                genresResp.response.genres?.genre?.forEach { genre ->
                    try {
                        val genreSongs = api.getSongsByGenre(genre.value, source.username, token, salt, count = 1000)
                        genreSongs.response.songsByGenre?.song?.forEach { addSong(it, source, allTracks) }
                    } catch (e: Exception) {
                        Timber.w("Sync: Failed to fetch songs for genre ${genre.value}")
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Sync: Genre crawl failed")
            }
            Timber.d("Sync: After genre crawl, total tracks: ${allTracks.size}")

            // 5. Traditional Artist -> Album -> Song Crawl
            Timber.d("Sync: Fetching artist list...")
            val artistsResp = try { api.getArtists(source.username, token, salt) } catch (e: Exception) { null }
            val artists = artistsResp?.response?.artists?.index?.flatMap { it.artist ?: emptyList() } ?: emptyList()
            for (artist in artists) {
                try {
                    val artistDetails = api.getArtist(artist.id, source.username, token, salt)
                    artistDetails.response.artist?.album?.forEach { album ->
                        fetchAndAddSongsFromAlbum(api, source, album.id, token, salt, allTracks)
                    }
                } catch (e: Exception) {
                    Timber.w("Sync: Error crawling artist ${artist.name}")
                }
            }
            Timber.d("Sync: After artist crawl, total tracks: ${allTracks.size}")

            // 6. Album List Crawl (Paginating correctly)
            Timber.d("Sync: Crawling getAlbumList2...")
            listOf("alphabeticalByArtist", "newest", "random", "alphabeticalByName").forEach { type ->
                var offset = 0
                while (true) {
                    val resp = try { api.getAlbumList(source.username, token, salt, type, 500, offset) } catch (e: Exception) { null }
                    val albums = resp?.response?.albumList2?.album ?: emptyList()
                    if (albums.isEmpty()) break
                    albums.forEach { fetchAndAddSongsFromAlbum(api, source, it.id, token, salt, allTracks) }
                    offset += albums.size
                    if (albums.size < 10) break
                    if (offset > 10000) break
                }
            }
            Timber.d("Sync: After album list crawl, total tracks: ${allTracks.size}")

            // 7. Directory Crawl (Deepest/Slowest)
            Timber.d("Sync: Performing directory crawl...")
            val foldersResp = try { api.getMusicFolders(source.username, token, salt) } catch (e: Exception) { null }
            val folders = foldersResp?.response?.musicFolders?.musicFolder ?: emptyList()
            if (folders.isEmpty()) {
                // Some servers don't return folders but have songs. 
                // We've already tried search/random/artists/albums.
            } else {
                for (folder in folders) {
                    crawlDirectory(api, source, folder.id, token, salt, allTracks)
                }
            }
            Timber.d("Sync: After directory crawl, total tracks: ${allTracks.size}")

            // 8. Alphabetical Search Fallback (Only if we still have fewer tracks than expected)
            // Assuming 215 is a small library, we can afford a few more calls if we're under 200.
            if (allTracks.size < 200) {
                Timber.d("Sync: Count still low (${allTracks.size}), attempting alphabetical search...")
                for (char in 'a'..'z') {
                    try {
                        val res = api.search("${char}*", source.username, token, salt, songCount = 500)
                        res.response.searchResult3?.song?.forEach { addSong(it, source, allTracks) }
                        res.response.searchResult2?.song?.forEach { addSong(it, source, allTracks) }
                    } catch (e: Exception) {}
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Sync: Critical error during Subsonic sync")
            if (allTracks.isEmpty()) throw e
        }

        val result = allTracks.values.toList()
        Timber.d("Sync: fetchAllTracks complete: ${result.size} total tracks")
        return result
    }

    private fun addSong(song: SubsonicSong, source: MediaSource, map: MutableMap<String, Track>) {
        val track = song.toTrack(source)
        map[track.id] = track
    }

    private fun addSong(child: SubsonicChild, source: MediaSource, map: MutableMap<String, Track>) {
        if (!child.isDir) {
            val track = child.toTrack(source)
            map[track.id] = track
        }
    }

    private suspend fun crawlDirectory(api: SubsonicApi, source: MediaSource, dirId: String, token: String, salt: String, map: MutableMap<String, Track>) {
        try {
            val resp = api.getMusicDirectory(dirId, source.username, token, salt)
            resp.response.directory?.child?.forEach { child ->
                if (child.isDir) crawlDirectory(api, source, child.id, token, salt, map)
                else addSong(child, source, map)
            }
        } catch (e: Exception) {
            Timber.w("Sync: Error crawling directory $dirId")
        }
    }

    private fun SubsonicChild.toTrack(source: MediaSource): Track {
        return Track(
            id = "${source.id}_$id",
            title = title ?: "Unknown Title",
            artist = artist ?: "Unknown Artist",
            albumArtist = artist ?: "Unknown Artist",
            album = album ?: "Unknown Album",
            albumId = albumId ?: "",
            duration = (duration ?: 0) * 1000L,
            trackNumber = track ?: 0,
            discNumber = discNumber ?: 1,
            year = year ?: 0,
            genre = genre ?: "Unknown",
            uri = buildStreamUrl(source.baseUrl, id, source.username, source.password),
            artworkUri = coverArt?.let { buildCoverArtUrl(source.baseUrl, it, source.username, source.password) },
            sourceId = source.id,
            sourceType = source.type,
            bitrate = bitRate ?: 0,
            fileSize = size ?: 0L
        )
    }

    private suspend fun fetchAndAddSongsFromAlbum(api: SubsonicApi, source: MediaSource, albumId: String, token: String, salt: String, map: MutableMap<String, Track>) {
        try {
            val resp = api.getAlbum(albumId, source.username, token, salt)
            resp.response.album?.song?.forEach { song ->
                addSong(song, source, map)
            }
        } catch (e: Exception) {
            Timber.w("Sync: Error fetching album $albumId")
        }
    }

    fun buildStreamUrl(baseUrl: String, songId: String, u: String, p: String): String {
        val (t, s) = generateToken(p)
        return "${baseUrl.trimEnd('/')}/rest/stream?id=$songId&u=$u&t=$t&s=$s&v=1.16.1&c=MusicPlayer"
    }

    fun buildCoverArtUrl(baseUrl: String, id: String, u: String, p: String): String {
        val (t, s) = generateToken(p)
        return "${baseUrl.trimEnd('/')}/rest/getCoverArt?id=$id&u=$u&t=$t&s=$s&v=1.16.1&c=MusicPlayer&size=300"
    }

    fun generateToken(password: String): Pair<String, String> {
        val salt = UUID.randomUUID().toString().replace("-", "").take(16)
        val md5 = MessageDigest.getInstance("MD5")
        val token = BigInteger(1, md5.digest("$password$salt".toByteArray())).toString(16).padStart(32, '0')
        return token to salt
    }

    /**
     * Fetches lyrics for a song. Tries OpenSubsonic getLyricsBySongId first,
     * then falls back to classic Subsonic getLyrics by artist/title.
     */
    suspend fun fetchLyrics(api: SubsonicApi, source: MediaSource, songId: String, artist: String, title: String): String? {
        val (token, salt) = generateToken(source.password)

        // 1. OpenSubsonic / Navidrome: getLyricsBySongId (returns structured/synced lyrics)
        try {
            val resp = api.getLyricsBySongId(songId, source.username, token, salt)
            val lyricsList = resp.response.lyricsList?.structuredLyrics
            if (!lyricsList.isNullOrEmpty()) {
                // Prefer synced lyrics, otherwise take the first available
                val best = lyricsList.firstOrNull { it.synced } ?: lyricsList.first()
                val lines = best.line ?: emptyList()
                if (lines.isNotEmpty()) {
                    return if (best.synced) {
                        // Convert to LRC format
                        lines.joinToString("\n") { line ->
                            val ms = line.start ?: 0L
                            val min = ms / 60_000
                            val sec = (ms % 60_000) / 1_000
                            val cs = (ms % 1_000) / 10
                            "[%02d:%02d.%02d]%s".format(min, sec, cs, line.value ?: "")
                        }
                    } else {
                        lines.joinToString("\n") { it.value ?: "" }
                    }
                }
            }
        } catch (_: Exception) {
            // Not supported by this server — try classic endpoint
        }

        // 2. Classic Subsonic: getLyrics by artist + title
        try {
            val resp = api.getLyrics(artist, title, source.username, token, salt)
            val text = resp.response.lyrics?.value
            if (!text.isNullOrBlank()) return text
        } catch (_: Exception) {
            // Not supported
        }

        return null
    }

    /**
     * Extracts the bare song ID from a composite track ID (e.g. "source1_42" → "42").
     */
    fun extractSongId(track: Track): String {
        val prefix = "${track.sourceId}_"
        return if (track.id.startsWith(prefix)) track.id.removePrefix(prefix) else track.id
    }

    private fun SubsonicSong.toTrack(source: MediaSource): Track {
        return Track(
            id = "${source.id}_$id",
            title = title ?: "Unknown Title",
            artist = artist ?: "Unknown Artist",
            albumArtist = artist ?: "Unknown Artist",
            album = album ?: "Unknown Album",
            albumId = albumId ?: "",
            duration = (duration ?: 0) * 1000L,
            trackNumber = track ?: 0,
            discNumber = discNumber ?: 1,
            year = year ?: 0,
            genre = genre ?: "Unknown",
            uri = buildStreamUrl(source.baseUrl, id, source.username, source.password),
            artworkUri = coverArt?.let { buildCoverArtUrl(source.baseUrl, it, source.username, source.password) },
            sourceId = source.id,
            sourceType = source.type,
            bitrate = bitRate ?: 0
        )
    }
}
