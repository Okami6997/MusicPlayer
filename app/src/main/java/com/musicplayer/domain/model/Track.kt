package com.musicplayer.domain.model

/**
 * Represents a single audio track that can be played.
 */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtist: String = artist,
    val album: String,
    val albumId: String = "",
    val duration: Long,           // milliseconds
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val year: Int = 0,
    val genre: String = "",
    val uri: String,              // content:// or http(s)://
    val artworkUri: String? = null,
    val sourceId: String,         // which MediaSource this belongs to
    val sourceName: String = "",  // name of the source (e.g. "My Nas", "Local")
    val sourceType: MediaSourceType,
    val isDownloaded: Boolean = false,
    val downloadedUri: String? = null,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val fileSize: Long = 0L,
    val codec: String = "",
    val extension: String = ""
)
