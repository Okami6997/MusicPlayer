package com.musicplayer.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val tracks: List<Track> = emptyList(),
    val artworkUri: String? = null,
    val sourceId: String,
    val sourceType: MediaSourceType,
    val isLocal: Boolean = true,
    val description: String = "",
    val duration: Long = 0L,       // total duration in ms
    val trackCount: Int = tracks.size
)
