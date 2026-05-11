package com.musicplayer.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing tracks associated with profiles.
 * This provides isolated storage for New UI tracks, separate from Old UI's TrackEntity.
 */
@Entity(
    tableName = "profile_tracks",
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["profileId", "remoteId"], unique = true)
    ]
)
data class ProfileTrackEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val remoteId: String, // The track's ID on the remote server
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String = "",
    val albumId: String = "",
    val duration: Long = 0L,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val year: Int = 0,
    val genre: String = "",
    val artworkUri: String? = null,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val fileSize: Long = 0L,
    val codec: String = "",
    val streamUri: String = "",
    val sourceType: String = "USER", // The actual source type (SUBSONIC, NAVIDROME, JELLYFIN, etc.)
    val createdAt: Long = System.currentTimeMillis()
)
