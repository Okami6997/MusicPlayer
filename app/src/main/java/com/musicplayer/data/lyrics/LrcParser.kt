package com.musicplayer.data.lyrics

import com.musicplayer.domain.model.Lyrics
import com.musicplayer.domain.model.LyricsLine
import com.musicplayer.domain.model.LyricsWord
import java.util.regex.MatchResult
import java.util.regex.Pattern

object LrcParser {

    // Matches [mm:ss.xx], [mm:ss.xxx], [mm:ss]
    private val TIMESTAMP_PATTERN: Pattern =
        Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{2,3}))?]")

    // Enhanced pattern for word-level timestamps (e.g., <00:12.34>word)
    private val WORD_TIMESTAMP_PATTERN: Pattern =
        Pattern.compile("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{2,3}))?>")

    /**
     * Parses an LRC-formatted string into synced [Lyrics].
     * Supports multiple timestamps per line, e.g. `[00:12.00][00:24.00]text`.
     * Also supports word-level timestamps for wavy flow effect.
     */
    fun parse(lrcContent: String): Lyrics? {
        val lines = mutableListOf<LyricsLine>()

        for (rawLine in lrcContent.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            // First, try to parse word-level timestamps
            val words = parseWordTimestamps(trimmed)
            if (words.isNotEmpty()) {
                // Has word-level timing - use the first word's time as line time
                lines.add(LyricsLine(timeMs = words.first().startTimeMs, text = trimmed, words = words))
                continue
            }

            // Fall back to line-level timestamps
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

        // Interpolate word timing for lines that don't have explicit word timestamps
        val interpolatedLines = interpolateWordTiming(lines)

        return Lyrics(lines = interpolatedLines, isSynced = true)
    }

    /**
     * Parse word-level timestamps from a line (e.g., <00:12.34>Hello <00:12.56>World)
     */
    private fun parseWordTimestamps(line: String): List<LyricsWord> {
        val words = mutableListOf<LyricsWord>()
        val matcher = WORD_TIMESTAMP_PATTERN.matcher(line)
        val matches = mutableListOf<MatchResult>()

        while (matcher.find()) {
            matches.add(matcher.toMatchResult())
        }

        if (matches.isEmpty()) return emptyList()

        for (i in matches.indices) {
            val match = matches[i]
            val minutes = match.group(1)!!.toLong()
            val seconds = match.group(2)!!.toLong()
            val fraction = match.group(3)
            val startMs = when {
                fraction == null -> minutes * 60_000 + seconds * 1_000
                fraction.length == 3 -> minutes * 60_000 + seconds * 1_000 + fraction.toLong()
                else -> minutes * 60_000 + seconds * 1_000 + fraction.toLong() * 10
            }

            // Get the word between the timestamp and next timestamp or end
            val wordStart = match.end()
            val wordEnd = if (i < matches.size - 1) matches[i + 1].start() else line.length
            val wordText = line.substring(wordStart, wordEnd).trim()

            if (wordText.isNotEmpty()) {
                words.add(LyricsWord(word = wordText, startTimeMs = startMs, endTimeMs = 0))
            }
        }

        // Calculate end times
        if (words.isNotEmpty()) {
            for (i in words.indices) {
                words[i] = if (i < words.size - 1) {
                    words[i].copy(endTimeMs = words[i + 1].startTimeMs)
                } else {
                    // Last word - estimate based on typical speaking rate (80ms per char)
                    words[i].copy(endTimeMs = words[i].startTimeMs + words[i].word.length * 80L)
                }
            }
        }

        return words
    }

    /**
     * Interpolate word timing for lines that only have line-level timestamps.
     * Creates a wavy flow effect by estimating word timing based on character count.
     */
    private fun interpolateWordTiming(lines: List<LyricsLine>): List<LyricsLine> {
        if (lines.size < 2) return lines

        val result = mutableListOf<LyricsLine>()

        for (i in lines.indices) {
            val line = lines[i]
            if (line.words.isNotEmpty()) {
                result.add(line)
                continue
            }

            // Calculate end time from next line's start time
            val endTime = if (i < lines.size - 1) {
                lines[i + 1].timeMs
            } else {
                // Last line - estimate duration
                line.timeMs + line.text.length * 100L
            }

            // Interpolate words
            val words = interpolateWordsForLine(line.text, line.timeMs, endTime)
            result.add(line.copy(words = words))
        }

        return result
    }

    /**
     * Interpolate words for a single line based on character positions
     */
    private fun interpolateWordsForLine(text: String, startTime: Long, endTime: Long): List<LyricsWord> {
        if (text.isBlank()) return emptyList()

        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        // Calculate time per character for smooth distribution
        val totalDuration = (endTime - startTime).coerceAtLeast(1L)
        val textLength = text.length.coerceAtLeast(1)

        var currentTime = startTime
        val result = mutableListOf<LyricsWord>()

        for (word in words) {
            val wordStart = currentTime
            // Distribute duration based on word length relative to total text length
            val wordDuration = (word.length.toFloat() / textLength * totalDuration).toLong().coerceAtLeast(50L)
            val wordEnd = (wordStart + wordDuration).coerceAtMost(endTime)

            result.add(LyricsWord(word = word, startTimeMs = wordStart, endTimeMs = wordEnd))
            currentTime = wordEnd
        }

        return result
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
