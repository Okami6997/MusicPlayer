package com.musicplayer.domain.model

data class LyricsWord(
    val word: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

data class LyricsLine(
    val timeMs: Long,   // timestamp in milliseconds; -1 for unsynced lines
    val text: String,
    val words: List<LyricsWord> = emptyList()  // Word-level timing for wavy flow
)

data class Lyrics(
    val lines: List<LyricsLine>,
    val isSynced: Boolean
)
