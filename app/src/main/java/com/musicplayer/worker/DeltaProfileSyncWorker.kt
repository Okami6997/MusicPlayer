package com.musicplayer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.ProfileMusicRepository
import com.musicplayer.data.repository.ProfileRepository
import com.musicplayer.profile.toMediaSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Background worker that performs a **delta sync** for a single New-UI
 * [com.musicplayer.profile.Profile]. Fetches only the tracks that have
 * changed since the profile's `lastDeltaSyncAt` timestamp, diffs them
 * against the local cache, and applies inserts/updates/deletes.
 *
 * Falls back to a full sync if the profile has never been fully synced.
 */
class DeltaProfileSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DeltaProfileSyncWorkerEntryPoint {
        fun musicRepository(): MusicRepository
        fun profileMusicRepository(): ProfileMusicRepository
        fun profileRepository(): ProfileRepository
    }

    override suspend fun doWork(): Result {
        val profileId = inputData.getString(KEY_PROFILE_ID) ?: return Result.failure()
        val profileName = inputData.getString(KEY_PROFILE_NAME) ?: "Unknown"

        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                DeltaProfileSyncWorkerEntryPoint::class.java
            )
            val musicRepository = entryPoint.musicRepository()
            val profileMusicRepository = entryPoint.profileMusicRepository()
            val profileRepository = entryPoint.profileRepository()

            val profile = profileRepository.getProfileById(profileId)
                ?: return Result.failure(
                    androidx.work.Data.Builder()
                        .putString(KEY_ERROR_MESSAGE, "Profile not found")
                        .build()
                )

            val source = profile.toMediaSource()

            // If we've never done a full sync, fall back to one.
            if (profile.lastFullSyncAt <= 0L) {
                val tracks = musicRepository.fetchTracksFromSource(source)
                profileMusicRepository.clearTracksForProfile(profileId)
                profileMusicRepository.saveTracks(profileId, source.type, tracks)
                val now = System.currentTimeMillis()
                val fresh = profile.copy(lastDeltaSyncAt = now, lastFullSyncAt = now)
                profileRepository.updateProfile(fresh)
                return Result.success(
                    androidx.work.Data.Builder()
                        .putInt(KEY_ADDED, tracks.size)
                        .putInt(KEY_UPDATED, 0)
                        .putInt(KEY_REMOVED, 0)
                        .putInt(KEY_TOTAL_AFTER, tracks.size)
                        .putString(KEY_PROFILE_NAME, profileName)
                        .build()
                )
            }

            // Otherwise, fetch only changed tracks and apply a diff.
            val changed = musicRepository.fetchChangedTracksFromSource(
                source = source,
                sinceEpochMillis = profile.lastDeltaSyncAt
            )
            val remoteIdsSnapshot = musicRepository.fetchAllTrackIdsFromSource(source)
            val freshProfile = profileRepository.getProfileById(profileId) ?: profile
            val result = profileMusicRepository.deltaSyncProfile(
                freshProfile,
                changed,
                remoteIdsSnapshot
            )
            val updated = freshProfile.copy(lastDeltaSyncAt = System.currentTimeMillis())
            profileRepository.updateProfile(updated)

            Result.success(
                androidx.work.Data.Builder()
                    .putInt(KEY_ADDED, result.added)
                    .putInt(KEY_UPDATED, result.updated)
                    .putInt(KEY_REMOVED, result.removed)
                    .putInt(KEY_TOTAL_AFTER, result.totalAfter)
                    .putString(KEY_PROFILE_NAME, profileName)
                    .build()
            )
        } catch (e: Exception) {
            Result.failure(
                androidx.work.Data.Builder()
                    .putString(KEY_ERROR_MESSAGE, e.message)
                    .build()
            )
        }
    }

    companion object {
        const val KEY_PROFILE_ID = "profile_id"
        const val KEY_PROFILE_NAME = "profile_name"
        const val KEY_ADDED = "added"
        const val KEY_UPDATED = "updated"
        const val KEY_REMOVED = "removed"
        const val KEY_TOTAL_AFTER = "total_after"
        const val KEY_ERROR_MESSAGE = "error_message"
    }
}
