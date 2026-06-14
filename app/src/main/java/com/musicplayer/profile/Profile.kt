package com.musicplayer.profile

import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.MediaSourceType

/**
 * Represents a user profile containing media server connection details.
 * Profiles are used in the New UI to manage multiple media server configurations.
 */
data class Profile(
    val id: String,
    val name: String,
    val serviceType: MediaServiceType,
    val ipAddress: String,
    val portOverride: Int? = null,
    val isEnabled: Boolean = true,
    val lastUsed: Long = 0L,
    val username: String = "",
    val password: String = "",
    val token: String = "",
    val downloadPort: Int = 3000,
    // Epoch millis of the last successful delta sync for this profile. 0 means never.
    val lastDeltaSyncAt: Long = 0L,
    // Epoch millis of the last successful full sync for this profile. 0 means never.
    val lastFullSyncAt: Long = 0L
) {
    /**
     * Returns the effective port for this profile.
     * Uses the override if provided, otherwise falls back to the service's default port.
     */
    val effectivePort: Int
        get() = portOverride ?: serviceType.defaultPort

    /**
     * Constructs the base URL for this profile.
     * Format: http://<ipAddress>:<effectivePort>
     */
    val baseUrl: String
        get() = "http://$ipAddress:$effectivePort"

    /**
     * Constructs the download service URL for this profile.
     * Uses a separate port (default 3000) for the download server.
     */
    val downloadUrl: String
        get() = "http://$ipAddress:$downloadPort"

    /**
     * Returns a copy of this profile with the last used timestamp updated.
     */
    fun withLastUsed(): Profile = copy(lastUsed = System.currentTimeMillis())
}

/**
 * Converts a Profile to a MediaSource for use with sync and connection-test infrastructure.
 */
fun Profile.toMediaSource(): MediaSource {
    val type = when (serviceType) {
        MediaServiceType.Jellyfin    -> MediaSourceType.JELLYFIN
        MediaServiceType.Emby        -> MediaSourceType.EMBY
        MediaServiceType.Plex        -> MediaSourceType.PLEX
        MediaServiceType.Subsonic    -> MediaSourceType.SUBSONIC
        MediaServiceType.OpenSubsonic -> MediaSourceType.OPEN_SUBSONIC
        MediaServiceType.Navidrome   -> MediaSourceType.NAVIDROME
    }
    return MediaSource(
        id       = id,
        name     = name,
        type     = type,
        baseUrl  = baseUrl,
        username = username,
        password = password,
        token    = token
    )
}
