package com.musicplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_sources")
data class MediaSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val baseUrl: String,
    val username: String,
    val password: String,
    val token: String,
    val localPath: String,
    val isEnabled: Boolean,
    val lastSyncTime: Long,
    val artworkUri: String?
)
