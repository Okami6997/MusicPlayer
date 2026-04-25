package com.musicplayer.data.local

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.musicplayer.domain.model.MediaSourceType
import com.musicplayer.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans the device's MediaStore for audio files and converts them to [Track] objects.
 */
@Singleton
class LocalMediaScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val LOCAL_SOURCE_ID = "local"

        private val AUDIO_COLLECTION: Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        private val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.BITRATE
        )
    }

    /**
     * Returns all audio tracks found in the device MediaStore.
     */
    fun scan(): List<Track> {
        val tracks = mutableListOf<Track>()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.ARTIST} ASC, ${MediaStore.Audio.Media.ALBUM} ASC, ${MediaStore.Audio.Media.TRACK} ASC"

        try {
            context.contentResolver.query(
                AUDIO_COLLECTION,
                PROJECTION,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumArtistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val genreCol = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val bitrateCol = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = Uri.withAppendedPath(AUDIO_COLLECTION, id.toString()).toString()
                    val albumId = cursor.getLong(albumIdCol)
                    val artworkUri = Uri.parse("content://media/external/audio/albumart/$albumId").toString()
                    val trackNum = cursor.getInt(trackCol)
                    // MediaStore encodes disc/track as discNumber * 1000 + trackNumber
                    val discNumber = if (trackNum > 1000) trackNum / 1000 else 1
                    val trackNumber = if (trackNum > 1000) trackNum % 1000 else trackNum

                    tracks.add(
                        Track(
                            id = "local_${id}",
                            title = cursor.getString(titleCol) ?: "Unknown Title",
                            artist = cursor.getString(artistCol) ?: "Unknown Artist",
                            albumArtist = if (albumArtistCol >= 0) cursor.getString(albumArtistCol) ?: "" else "",
                            album = cursor.getString(albumCol) ?: "Unknown Album",
                            albumId = albumId.toString(),
                            duration = cursor.getLong(durationCol),
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            year = cursor.getInt(yearCol),
                            genre = if (genreCol >= 0) cursor.getString(genreCol) ?: "" else "",
                            uri = contentUri,
                            artworkUri = artworkUri,
                            sourceId = LOCAL_SOURCE_ID,
                            sourceName = "Local Device",
                            sourceType = MediaSourceType.LOCAL,
                            fileSize = cursor.getLong(sizeCol),
                            bitrate = if (bitrateCol >= 0) cursor.getInt(bitrateCol) else 0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scanning local media")
        }

        Timber.d("LocalMediaScanner: found ${tracks.size} tracks")
        return tracks
    }
}
