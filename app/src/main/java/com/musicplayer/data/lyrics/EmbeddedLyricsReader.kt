package com.musicplayer.data.lyrics

import android.content.Context
import android.net.Uri
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.InputStream

/**
 * Reads embedded lyrics from audio files by parsing ID3v2 USLT
 * (Unsynchronized Lyrics/Text) frames directly from the byte stream.
 */
object EmbeddedLyricsReader {

    private const val MAX_FRAME_SIZE = 1_000_000 // 1 MB safety limit per frame

    /**
     * Attempts to read lyrics embedded in the audio file at [uri].
     * Returns the raw lyrics text, or `null` if none found.
     */
    fun read(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                readId3Lyrics(BufferedInputStream(stream, 8192))
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to read embedded lyrics from $uri")
            null
        }
    }

    private fun readId3Lyrics(input: BufferedInputStream): String? {
        val header = ByteArray(10)
        if (input.readExactly(header) != 10) return null

        // Verify "ID3" magic bytes
        if (header[0] != 0x49.toByte() ||  // 'I'
            header[1] != 0x44.toByte() ||  // 'D'
            header[2] != 0x33.toByte()     // '3'
        ) return null

        val majorVersion = header[3].toInt() and 0xFF
        if (majorVersion !in 2..4) return null

        val tagSize = decodeSynchsafe(header, 6)
        if (tagSize <= 0) return null

        var remaining = tagSize
        while (remaining > 10) {
            val frameHeader = ByteArray(10)
            if (input.readExactly(frameHeader) != 10) break
            remaining -= 10

            val frameId = String(frameHeader, 0, 4, Charsets.ISO_8859_1)

            // Null byte means we've hit padding — stop scanning
            if (frameId[0] == '\u0000') break

            val frameSize = if (majorVersion >= 4) {
                decodeSynchsafe(frameHeader, 4)
            } else {
                decodeInt(frameHeader, 4)
            }

            if (frameSize <= 0 || frameSize > remaining) break

            if (frameId == "USLT" && frameSize < MAX_FRAME_SIZE) {
                val frameData = ByteArray(frameSize)
                if (input.readExactly(frameData) == frameSize) {
                    val text = parseUsltFrame(frameData)
                    if (!text.isNullOrBlank()) return text
                }
                break
            }

            // Skip frames we don't care about
            val skipped = input.skip(frameSize.toLong())
            remaining -= frameSize
            if (skipped != frameSize.toLong()) break
        }
        return null
    }

    private fun parseUsltFrame(data: ByteArray): String? {
        if (data.size < 5) return null

        val encoding = data[0].toInt() and 0xFF
        // bytes 1-3: language (skip)
        var pos = 4

        val charset = when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }

        // Skip content descriptor (null-terminated)
        if (encoding == 1 || encoding == 2) {
            // UTF-16: null terminator is two zero bytes
            while (pos < data.size - 1) {
                if (data[pos].toInt() == 0 && data[pos + 1].toInt() == 0) {
                    pos += 2
                    break
                }
                pos++
            }
        } else {
            while (pos < data.size) {
                if (data[pos].toInt() == 0) {
                    pos++
                    break
                }
                pos++
            }
        }

        val textLength = data.size - pos
        if (textLength <= 0) return null

        return String(data, pos, textLength, charset).trim()
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun decodeSynchsafe(buf: ByteArray, offset: Int): Int =
        (buf[offset].toInt() and 0x7F shl 21) or
        (buf[offset + 1].toInt() and 0x7F shl 14) or
        (buf[offset + 2].toInt() and 0x7F shl 7) or
        (buf[offset + 3].toInt() and 0x7F)

    private fun decodeInt(buf: ByteArray, offset: Int): Int =
        (buf[offset].toInt() and 0xFF shl 24) or
        (buf[offset + 1].toInt() and 0xFF shl 16) or
        (buf[offset + 2].toInt() and 0xFF shl 8) or
        (buf[offset + 3].toInt() and 0xFF)

    private fun InputStream.readExactly(buf: ByteArray): Int {
        var total = 0
        while (total < buf.size) {
            val n = read(buf, total, buf.size - total)
            if (n == -1) break
            total += n
        }
        return total
    }
}
