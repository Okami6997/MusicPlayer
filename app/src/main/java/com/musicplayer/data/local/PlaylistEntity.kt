package com.musicplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artworkUri: String?,
    val sourceId: String,
    val sourceType: String,
    val isLocal: Boolean,
    val description: String,
    val trackCount: Int,
    val duration: Long
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val position: Int
)
