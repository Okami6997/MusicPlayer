package com.musicplayer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.musicplayer.data.repository.MusicRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Background worker that performs a **delta sync** for a single old-UI
 * [com.musicplayer.domain.model.MediaSource]. Fetches only the tracks that
 * have changed since the source's `lastDeltaSyncAt` timestamp, diffs them
 * against the local cache, and applies inserts/updates/deletes.
 *
 * Falls back to a full sync if the source has never been fully synced.
 */
class DeltaSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DeltaSyncWorkerEntryPoint {
        fun repository(): MusicRepository
    }

    override suspend fun doWork(): Result {
        val sourceId = inputData.getString(KEY_SOURCE_ID) ?: return Result.failure()
        val sourceName = inputData.getString(KEY_SOURCE_NAME) ?: "Unknown"

        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                DeltaSyncWorkerEntryPoint::class.java
            )
            val repository = entryPoint.repository()

            val source = repository.getSourceById(sourceId)
                ?: return Result.failure()

            val result = repository.deltaSyncSource(source)

            Result.success(
                androidx.work.Data.Builder()
                    .putInt(KEY_ADDED, result.added)
                    .putInt(KEY_UPDATED, result.updated)
                    .putInt(KEY_REMOVED, result.removed)
                    .putInt(KEY_TOTAL_AFTER, result.totalAfter)
                    .putString(KEY_SOURCE_NAME, sourceName)
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
        const val KEY_SOURCE_ID = "source_id"
        const val KEY_SOURCE_NAME = "source_name"
        const val KEY_ADDED = "added"
        const val KEY_UPDATED = "updated"
        const val KEY_REMOVED = "removed"
        const val KEY_TOTAL_AFTER = "total_after"
        const val KEY_ERROR_MESSAGE = "error_message"
    }
}
