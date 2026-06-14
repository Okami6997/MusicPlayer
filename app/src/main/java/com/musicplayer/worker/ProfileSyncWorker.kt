package com.musicplayer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.ProfileMusicRepository
import com.musicplayer.data.repository.ProfileRepository
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.profile.ProfileManager
import com.musicplayer.profile.toMediaSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class ProfileSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProfileSyncWorkerEntryPoint {
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
                ProfileSyncWorkerEntryPoint::class.java
            )
            val musicRepository = entryPoint.musicRepository()
            val profileMusicRepository = entryPoint.profileMusicRepository()
            val profileRepository = entryPoint.profileRepository()

            // Get the profile and convert to MediaSource
            val profile = profileRepository.getProfileById(profileId)
            if (profile == null) {
                return Result.failure(
                    androidx.work.Data.Builder()
                        .putString(KEY_ERROR_MESSAGE, "Profile not found")
                        .build()
                )
            }

            val source: MediaSource = profile.toMediaSource()

            val tracks = musicRepository.fetchTracksFromSource(source)
            profileMusicRepository.clearTracksForProfile(profileId)
            profileMusicRepository.saveTracks(profileId, source.type, tracks)
            val now = System.currentTimeMillis()
            profileRepository.updateProfile(
                profile.copy(
                    lastFullSyncAt = now,
                    lastDeltaSyncAt = if (profile.lastDeltaSyncAt > 0L) profile.lastDeltaSyncAt else now
                )
            )

            Result.success(
                androidx.work.Data.Builder()
                    .putInt(KEY_TRACK_COUNT, tracks.size)
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
        const val KEY_TRACK_COUNT = "track_count"
        const val KEY_ERROR_MESSAGE = "error_message"
    }
}