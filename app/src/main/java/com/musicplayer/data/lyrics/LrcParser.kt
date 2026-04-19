package com.musicplayer.data.lyrics

import com.musicplayer.domain.model.Lyrics
import com.musicplayer.domain.model.LyricsLine
import java.util.regex.Pattern

object LrcParser {

    // Matches [mm:ss.xx], [mm:ss.xxx], [mm:ss]
    private val TIMESTAMP_PATTERN: Pattern =
        Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{2,3}))?]")

    /**
     * Parses an LRC-formatted string into synced [Lyrics].
     * Supports multiple timestamps per line, e.g. `[00:12.00][00:24.00]text`.
     */
    fun parse(lrcContent: String): Lyrics? {
        val lines = mutableListOf<LyricsLine>()

        for (rawLine in lrcContent.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            val matcher = TIMESTAMP_PATTERN.matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (matcher.find()) {
                val minutes = matcher.group(1)!!.toLong()
                val seconds = matcher.group(2)!!.toLong()
                val fraction = matcher.group(3)
                val ms = when {
                    fraction == null -> 0L
                    fraction.length == 3 -> fraction.toLong()
                    else -> fraction.toLong() * 10        // centiseconds → ms
                }
                timestamps.add(minutes * 60_000 + seconds * 1_000 + ms)
                lastMatchEnd = matcher.end()
            }

            if (timestamps.isEmpty()) continue

            val text = trimmed.substring(lastMatchEnd).trim()
            for (ts in timestamps) {
                lines.add(LyricsLine(timeMs = ts, text = text))
            }
        }

        if (lines.isEmpty()) return null
        lines.sortBy { it.timeMs }
        return Lyrics(lines = lines, isSynced = true)
    }

    /**
     * Wraps plain (unsynced) text into [Lyrics] with each non-empty line as a separate entry.
     */
    fun parsePlainText(text: String): Lyrics? {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { LyricsLine(timeMs = -1, text = it) }
        return if (lines.isNotEmpty()) Lyrics(lines = lines, isSynced = false) else null
    }
}
