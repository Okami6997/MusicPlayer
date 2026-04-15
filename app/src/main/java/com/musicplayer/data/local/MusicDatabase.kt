package com.musicplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackEntity::class,
        MediaSourceEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun mediaSourceDao(): MediaSourceDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val DATABASE_NAME = "music_player.db"
    }
}
