package com.musicplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val albumId: String,
    val duration: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val genre: String,
    val uri: String,
    val artworkUri: String?,
    val sourceId: String,
    val sourceName: String,
    val sourceType: String,
    val isDownloaded: Boolean,
    val downloadedUri: String?,
    val bitrate: Int,
    val sampleRate: Int,
    val fileSize: Long,
    val codec: String,
    val extension: String,
    // Timestamp (epoch millis) reported by the remote server for when this track was last modified.
    // 0 means "unknown / never synced via delta". Used by the delta sync algorithm to skip
    // tracks that haven't changed since the last sync.
    val remoteUpdatedAt: Long = 0L
)
