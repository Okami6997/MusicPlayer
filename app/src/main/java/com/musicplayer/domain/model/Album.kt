package com.musicplayer.domain.model

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int = 0,
    val trackCount: Int = 0,
    val artworkUri: String? = null,
    val sourceId: String,
    val sourceName: String = "",
    val sourceType: MediaSourceType,
    val genres: List<String> = emptyList()
)
