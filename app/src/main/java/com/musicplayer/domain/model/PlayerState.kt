package com.musicplayer.domain.model

/**
 * Current state of the player.
 */
enum class PlayerState {
    IDLE,
    LOADING,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR
}

/**
 * Repeat mode for the queue.
 */
enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

/**
 * Snapshot of everything the UI needs about the current playback session.
 */
data class PlayerUiState(
    val currentTrack: Track? = null,
    val playerState: PlayerState = PlayerState.IDLE,
    val playWhenReady: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Track> = emptyList(),
    val currentQueueIndex: Int = 0,
    val volume: Float = 1f,
    val castDevice: String? = null,    // non-null means cast is active
    val lyrics: Lyrics? = null,
    val currentLyricsLineIndex: Int = -1,
    val showLyrics: Boolean = false,
    val showQueue: Boolean = false
)
