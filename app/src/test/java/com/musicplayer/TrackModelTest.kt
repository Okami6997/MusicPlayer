package com.musicplayer

import com.musicplayer.domain.model.MediaSourceType
import com.musicplayer.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackModelTest {

    private fun sampleTrack() = Track(
        id = "local_1",
        title = "Test Song",
        artist = "Test Artist",
        album = "Test Album",
        duration = 210_000L,
        uri = "content://media/external/audio/media/1",
        sourceId = "local",
        sourceType = MediaSourceType.LOCAL
    )

    @Test
    fun `track duration is stored correctly in milliseconds`() {
        val track = sampleTrack()
        assertEquals(210_000L, track.duration)
    }

    @Test
    fun `track defaults to disc 1`() {
        val track = sampleTrack()
        assertEquals(1, track.discNumber)
    }

    @Test
    fun `track isDownloaded defaults to false`() {
        val track = sampleTrack()
        assertEquals(false, track.isDownloaded)
    }

    @Test
    fun `track copy preserves unmodified fields`() {
        val original = sampleTrack()
        val downloaded = original.copy(isDownloaded = true, downloadedUri = "/storage/emulated/0/Music/test.mp3")
        assertEquals(original.title, downloaded.title)
        assertEquals(true, downloaded.isDownloaded)
    }
}
