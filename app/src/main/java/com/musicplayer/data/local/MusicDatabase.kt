package com.musicplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackEntity::class,
        MediaSourceEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        ProfileEntity::class,
        ProfileTrackEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun mediaSourceDao(): MediaSourceDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun profileDao(): ProfileDao

    companion object {
        const val DATABASE_NAME = "music_player.db"

        // v7 → v8: add columns needed for delta sync support
        val MIGRATION_7_8 = androidx.room.migration.Migration(7, 8) { db ->
            // tracks: remoteUpdatedAt (epoch millis of last remote change)
            db.execSQL("ALTER TABLE tracks ADD COLUMN remoteUpdatedAt INTEGER NOT NULL DEFAULT 0")
            // media_sources: lastDeltaSyncAt + lastFullSyncAt
            db.execSQL("ALTER TABLE media_sources ADD COLUMN lastDeltaSyncAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE media_sources ADD COLUMN lastFullSyncAt INTEGER NOT NULL DEFAULT 0")
            // profile_tracks: remoteUpdatedAt
            db.execSQL("ALTER TABLE profile_tracks ADD COLUMN remoteUpdatedAt INTEGER NOT NULL DEFAULT 0")
            // profiles: lastDeltaSyncAt + lastFullSyncAt
            db.execSQL("ALTER TABLE profiles ADD COLUMN lastDeltaSyncAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE profiles ADD COLUMN lastFullSyncAt INTEGER NOT NULL DEFAULT 0")
        }
    }
}
