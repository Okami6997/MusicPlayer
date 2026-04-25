package com.musicplayer.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.documentfile.provider.DocumentFile
import com.musicplayer.data.remote.subsonic.SubsonicClient
import com.musicplayer.domain.model.MediaSourceType
import com.musicplayer.domain.model.Track
import com.musicplayer.ui.settings.DownloadsViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val subsonicClient: SubsonicClient,
    private val dataStore: DataStore<Preferences>,
    private val okHttpClient: OkHttpClient
) {
    suspend fun downloadTrack(track: Track) = withContext(Dispatchers.IO) {
        val source = musicRepository.getSourceById(track.sourceId) ?: return@withContext
        val quality = dataStore.data.first()[DownloadsViewModel.KEY_DOWNLOAD_QUALITY] ?: "Lossless"
        
        val songId = subsonicClient.extractSongId(track)
        val url = when (source.type) {
            MediaSourceType.SUBSONIC, MediaSourceType.OPEN_SUBSONIC, MediaSourceType.NAVIDROME -> {
                if (quality == "Lossless") {
                    subsonicClient.buildDownloadUrl(source.baseUrl, songId, source.username, source.password)
                } else {
                    val bitrate = quality.filter { it.isDigit() }.toIntOrNull() ?: 320
                    "${subsonicClient.buildStreamUrl(source.baseUrl, songId, source.username, source.password)}&maxBitRate=$bitrate"
                }
            }
            else -> track.uri
        }

        Timber.d("Downloading track ${track.title} from $url")
        
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                
                val body = response.body ?: throw Exception("Empty response body")
                
                val prefs = dataStore.data.first()
                val customLocation = prefs[DownloadsViewModel.KEY_DOWNLOAD_LOCATION]
                
                val extension = if (quality == "Lossless") {
                    track.extension.ifEmpty { "flac" }
                } else {
                    "mp3"
                }
                val rawFileName = "${track.artist} - ${track.title}.$extension"
                val fileName = rawFileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")

                if (customLocation != null && customLocation.startsWith("content://")) {
                    val treeUri = Uri.parse(customLocation)
                    val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                        ?: throw Exception("Failed to open custom download directory")
                    
                    val file = pickedDir.createFile("audio/*", fileName)
                        ?: throw Exception("Failed to create file in custom directory")

                    body.byteStream().use { input ->
                        context.contentResolver.openOutputStream(file.uri)?.use { output ->
                            input.copyTo(output)
                        } ?: throw Exception("Failed to open output stream for custom location")
                    }
                    
                    musicRepository.markTrackDownloaded(track.id, file.uri.toString())
                    Timber.d("Successfully downloaded ${track.title} to ${file.uri}")
                } else {
                    val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    
                    val file = File(downloadsDir, fileName)
                    
                    body.byteStream().use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    musicRepository.markTrackDownloaded(track.id, file.absolutePath)
                    Timber.d("Successfully downloaded ${track.title} to ${file.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to download track ${track.title}")
        }
    }

    suspend fun downloadAlbum(albumId: String) {
        // Find all tracks for this album
        val allTracks = musicRepository.getAllTracks().first()
        val albumTracks = allTracks.filter { it.albumId == albumId || it.album == albumId } // albumId might be name in some cases
        
        albumTracks.forEach { track ->
            downloadTrack(track)
        }
    }
}
