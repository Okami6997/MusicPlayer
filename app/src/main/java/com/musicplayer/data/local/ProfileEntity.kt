package com.musicplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting Profile data.
 * Profiles are stored separately from the Old UI's MediaSource data.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val serviceType: String, // Stores the MediaServiceType name (e.g., "Jellyfin", "Plex")
    val ipAddress: String,
    val portOverride: Int?, // Null means use the service's default port
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
)
