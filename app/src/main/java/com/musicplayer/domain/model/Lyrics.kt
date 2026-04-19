package com.musicplayer.domain.model

data class LyricsLine(
    val timeMs: Long,   // timestamp in milliseconds; -1 for unsynced lines
    val text: String
)

data class Lyrics(
    val lines: List<LyricsLine>,
    val isSynced: Boolean
)
