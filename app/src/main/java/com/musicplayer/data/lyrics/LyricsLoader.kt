package com.musicplayer.data.lyrics

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.musicplayer.data.remote.subsonic.SubsonicApi
import com.musicplayer.data.remote.subsonic.SubsonicClient
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.ProfileRepository
import com.musicplayer.domain.model.Lyrics
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.MediaSourceType
import com.musicplayer.domain.model.Track
import com.musicplayer.profile.toMediaSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads lyrics for a [Track] by checking, in order:
 * 1. An external `.lrc` file next to the audio file (synced lyrics, local only).
 * 2. Lyrics embedded in the audio file's ID3 tags (USLT frame, local only).
 * 3. Server-side lyrics via Subsonic `getLyricsBySongId` / `getLyrics` API.
 */
@Singleton
class LyricsLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subsonicClient: SubsonicClient,
    private val musicRepository: MusicRepository,
    private val profileRepository: ProfileRepository
) {

    // Cache Retrofit API instances per baseUrl to avoid re-creating
    private val apiCache = mutableMapOf<String, SubsonicApi>()

    suspend fun loadLyrics(track: Track): Lyrics? = withContext(Dispatchers.IO) {
        // 1. External LRC file (local tracks only)
        tryLoadLrcFile(track)?.also {
            Timber.d("Loaded LRC file lyrics for: ${track.title}")
            return@withContext it
        }

        // 2. Embedded lyrics (local/file tracks only)
        tryLoadEmbeddedLyrics(track)?.also {
            Timber.d("Loaded embedded lyrics for: ${track.title}")
            return@withContext it
        }

        // 3. Subsonic API (remote Subsonic/OpenSubsonic/Navidrome tracks)
        tryLoadSubsonicLyrics(track)?.also {
            Timber.d("Loaded Subsonic lyrics for: ${track.title}")
            return@withContext it
        }

        null
    }

    // ── external .lrc ────────────────────────────────────────────────────────

    private fun tryLoadLrcFile(track: Track): Lyrics? {
        val filePath = resolveFilePath(track) ?: return null
        return try {
            val audioFile = File(filePath)
            val lrcFile = File(audioFile.parent, "${audioFile.nameWithoutExtension}.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                LrcParser.parse(lrcFile.readText(Charsets.UTF_8))
            } else null
        } catch (e: Exception) {
            Timber.w(e, "Failed to read LRC file for: ${track.title}")
            null
        }
    }

    // ── embedded (ID3 USLT) ──────────────────────────────────────────────────

    private fun tryLoadEmbeddedLyrics(track: Track): Lyrics? {
        val uri = Uri.parse(track.downloadedUri ?: track.uri)
        // Only attempt for local/file URIs the reader can open
        if (uri.scheme != "content" && uri.scheme != "file") return null

        val raw = EmbeddedLyricsReader.read(context, uri) ?: return null

        // The embedded text itself may be in LRC format
        val synced = LrcParser.parse(raw)
        if (synced != null) return synced

        return LrcParser.parsePlainText(raw)
    }

    // ── Subsonic API lyrics ──────────────────────────────────────────────────

    private suspend fun tryLoadSubsonicLyrics(track: Track): Lyrics? {
        val sourceType = track.sourceType
        if (sourceType != MediaSourceType.SUBSONIC &&
            sourceType != MediaSourceType.OPEN_SUBSONIC &&
            sourceType != MediaSourceType.NAVIDROME
        ) return null

        return try {
            // Try to get source from MediaSource table first
            var source = musicRepository.getSourceById(track.sourceId)

            // If not found, try to get from Profile table (for profile-based tracks)
            if (source == null) {
                val profile = profileRepository.getProfileById(track.sourceId)
                if (profile != null) {
                    source = profile.toMediaSource()
                }
            }

            if (source == null) {
                Timber.d("No source found for track: ${track.title}")
                return null
            }

            val api = getOrCreateSubsonicApi(source)
            val songId = subsonicClient.extractSongId(track)
            val raw = subsonicClient.fetchLyrics(api, source, songId, track.artist, track.title)
                ?: return null

            // If the server returned LRC-formatted text, parse as synced
            val synced = LrcParser.parse(raw)
            if (synced != null) return synced

            LrcParser.parsePlainText(raw)
        } catch (e: Exception) {
            Timber.w(e, "Failed to load Subsonic lyrics for: ${track.title}")
            null
        }
    }

    private fun getOrCreateSubsonicApi(source: MediaSource): SubsonicApi {
        val baseUrl = source.baseUrl.trimEnd('/') + "/"
        return apiCache.getOrPut(baseUrl) {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SubsonicApi::class.java)
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun resolveFilePath(track: Track): String? {
        if (track.sourceType != MediaSourceType.LOCAL) return null

        val uri = Uri.parse(track.downloadedUri ?: track.uri)
        if (uri.scheme == "file") return uri.path

        if (uri.scheme != "content") return null

        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Audio.Media.DATA),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    if (col >= 0) cursor.getString(col) else null
                } else null
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to resolve file path for: ${track.title}")
            null
        }
    }
}
