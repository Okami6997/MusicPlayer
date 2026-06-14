package com.musicplayer.domain.model

/**
 * Supported media source back-ends.
 */
enum class MediaSourceType {
    LOCAL,
    PLEX,
    EMBY,
    JELLYFIN,
    SUBSONIC,
    OPEN_SUBSONIC,
    NAVIDROME,
    AUDIOBOOKSHELF,
    CLOUD_DRIVE,
    USER
}

/**
 * A configured media source (server connection or local path).
 */
data class MediaSource(
    val id: String,
    val name: String,
    val type: MediaSourceType,
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val token: String = "",
    val localPath: String = "",
    val isEnabled: Boolean = true,
    val isOnline: Boolean = false,
    val lastSyncTime: Long = 0L,
    val lastDeltaSyncAt: Long = 0L,
    val lastFullSyncAt: Long = 0L,
    val artworkUri: String? = null
)
