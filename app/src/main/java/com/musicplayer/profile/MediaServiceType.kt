package com.musicplayer.profile

/**
 * Supported media service types with their default ports.
 * Each service has a default port that can be overridden in the profile.
 */
sealed class MediaServiceType(
    val displayName: String,
    val defaultPort: Int
) {
    data object Jellyfin : MediaServiceType("Jellyfin", 8096)
    data object Plex : MediaServiceType("Plex", 32400)
    data object Emby : MediaServiceType("Emby", 8096)
    data object Subsonic : MediaServiceType("Subsonic", 4040)
    data object OpenSubsonic : MediaServiceType("OpenSubsonic", 4040)
    data object Navidrome : MediaServiceType("Navidrome", 4533)

    val serializedName: String
        get() = when (this) {
            is Jellyfin -> "Jellyfin"
            is Plex -> "Plex"
            is Emby -> "Emby"
            is Subsonic -> "Subsonic"
            is OpenSubsonic -> "OpenSubsonic"
            is Navidrome -> "Navidrome"
        }

    companion object {
        val allTypes: List<MediaServiceType> by lazy { listOf(Jellyfin, Plex, Emby, Subsonic, OpenSubsonic, Navidrome) }

        fun fromString(value: String): MediaServiceType? =
            allTypes.find { it.serializedName.equals(value, ignoreCase = true) }

        fun fromDisplayName(value: String): MediaServiceType? =
            allTypes.find { it.displayName.equals(value, ignoreCase = true) }
    }
}
