package com.musicplayer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.musicplayer.data.repository.MusicRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun repository(): MusicRepository
    }

    override suspend fun doWork(): Result {
        val sourceId = inputData.getString(KEY_SOURCE_ID) ?: return Result.failure()
        val sourceName = inputData.getString(KEY_SOURCE_NAME) ?: "Unknown"

        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
            val repository = entryPoint.repository()

            val source = repository.getSourceById(sourceId)
                ?: return Result.failure()

            val tracks = repository.syncSource(source)

            Result.success(
                androidx.work.Data.Builder()
                    .putInt(KEY_TRACK_COUNT, tracks.size)
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
        const val KEY_TRACK_COUNT = "track_count"
        const val KEY_ERROR_MESSAGE = "error_message"
    }
}