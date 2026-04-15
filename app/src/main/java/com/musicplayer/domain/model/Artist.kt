package com.musicplayer.domain.model

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val trackCount: Int = 0,
    val artworkUri: String? = null,
    val sourceId: String,
    val sourceType: MediaSourceType,
    val genres: List<String> = emptyList(),
    val biography: String = ""
)
