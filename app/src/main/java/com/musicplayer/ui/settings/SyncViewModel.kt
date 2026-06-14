package com.musicplayer.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.di.dataStore
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.worker.DeltaSyncWorker
import com.musicplayer.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SyncUiState(
    val sources: List<MediaSource> = emptyList(),
    val activeSourceId: String? = null,
    val activeSourceName: String? = null,
    val message: String? = null,
    val isSyncing: Boolean = false
)

/**
 * ViewModel for the [SyncScreen]. Exposes one-click delta and full sync
 * for all configured sources, with per-source progress tracking.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        observeSources()
    }

    private fun observeSources() {
        viewModelScope.launch {
            repository.getAllSources().collect { sources ->
                _uiState.update { it.copy(sources = sources) }
            }
        }
    }

    private suspend fun persistLastDeltaSyncTime() {
        context.dataStore.edit { it[SettingsKeys.LAST_DELTA_SYNC_TIME] = System.currentTimeMillis() }
    }

    private suspend fun persistLastFullSyncTime() {
        context.dataStore.edit { it[SettingsKeys.LAST_FULL_SYNC_TIME] = System.currentTimeMillis() }
    }

    /**
     * Enqueues a delta sync for every enabled source. Sources are processed
     * sequentially (one WorkRequest per source) so that progress can be
     * reported in the UI.
     */
    fun syncAllDelta() {
        val sources = _uiState.value.sources.filter { it.isEnabled }
        if (sources.isEmpty()) {
            _uiState.update { it.copy(message = "No enabled sources to sync") }
            return
        }

        for (source in sources) {
            enqueueDeltaSync(source)
        }
        _uiState.update { it.copy(message = "Delta sync started for ${sources.size} source(s)", isSyncing = true) }
    }

    /**
     * Enqueues a full sync for every enabled source.
     */
    fun syncAllFull() {
        val sources = _uiState.value.sources.filter { it.isEnabled }
        if (sources.isEmpty()) {
            _uiState.update { it.copy(message = "No enabled sources to sync") }
            return
        }

        for (source in sources) {
            enqueueFullSync(source)
        }
        _uiState.update { it.copy(message = "Full sync started for ${sources.size} source(s)", isSyncing = true) }
    }

    /**
     * Delta sync for a single source.
     */
    fun deltaSyncSource(source: MediaSource) {
        enqueueDeltaSync(source)
        _uiState.update { it.copy(message = "Delta sync started for \"${source.name}\"", isSyncing = true, activeSourceId = source.id, activeSourceName = source.name) }
    }

    /**
     * Full sync for a single source.
     */
    fun fullSyncSource(source: MediaSource) {
        enqueueFullSync(source)
        _uiState.update { it.copy(message = "Full sync started for \"${source.name}\"", isSyncing = true, activeSourceId = source.id, activeSourceName = source.name) }
    }

    private fun enqueueDeltaSync(source: MediaSource) {
        val inputData = workDataOf(
            DeltaSyncWorker.KEY_SOURCE_ID to source.id,
            DeltaSyncWorker.KEY_SOURCE_NAME to source.name
        )
        val work = OneTimeWorkRequestBuilder<DeltaSyncWorker>()
            .setInputData(inputData)
            .addTag("delta_sync_${source.id}")
            .build()
        workManager.enqueueUniqueWork(
            "delta_sync_${source.id}",
            ExistingWorkPolicy.REPLACE,
            work
        )
        observeWork(work.id, source, isDelta = true)
    }

    private fun enqueueFullSync(source: MediaSource) {
        val inputData = workDataOf(
            SyncWorker.KEY_SOURCE_ID to source.id,
            SyncWorker.KEY_SOURCE_NAME to source.name
        )
        val work = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(inputData)
            .addTag("full_sync_${source.id}")
            .build()
        workManager.enqueueUniqueWork(
            "full_sync_${source.id}",
            ExistingWorkPolicy.REPLACE,
            work
        )
        observeWork(work.id, source, isDelta = false)
    }

    private fun observeWork(workId: UUID, source: MediaSource, isDelta: Boolean) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workId).collect { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val msg = if (isDelta) {
                            val added = workInfo.outputData.getInt(DeltaSyncWorker.KEY_ADDED, 0)
                            val updated = workInfo.outputData.getInt(DeltaSyncWorker.KEY_UPDATED, 0)
                            val removed = workInfo.outputData.getInt(DeltaSyncWorker.KEY_REMOVED, 0)
                            val total = workInfo.outputData.getInt(DeltaSyncWorker.KEY_TOTAL_AFTER, 0)
                            if (added == 0 && updated == 0 && removed == 0) {
                                "\"${source.name}\" up to date ($total tracks)"
                            } else {
                                "\"${source.name}\" synced: +$added ~$updated -$removed ($total total)"
                            }
                        } else {
                            val trackCount = workInfo.outputData.getInt(SyncWorker.KEY_TRACK_COUNT, 0)
                            "\"${source.name}\" full sync: $trackCount tracks"
                        }
                        // Persist the global "last sync" timestamps in DataStore.
                        if (isDelta) persistLastDeltaSyncTime() else persistLastFullSyncTime()
                        _uiState.update { it.copy(message = msg, isSyncing = false, activeSourceId = null, activeSourceName = null) }
                    }
                    WorkInfo.State.FAILED -> {
                        val err = if (isDelta) {
                            workInfo.outputData.getString(DeltaSyncWorker.KEY_ERROR_MESSAGE) ?: "Unknown"
                        } else {
                            workInfo.outputData.getString(SyncWorker.KEY_ERROR_MESSAGE) ?: "Unknown"
                        }
                        _uiState.update { it.copy(message = "Error syncing \"${source.name}\": $err", isSyncing = false, activeSourceId = null, activeSourceName = null) }
                    }
                    WorkInfo.State.RUNNING -> {
                        _uiState.update { it.copy(isSyncing = true, activeSourceId = source.id, activeSourceName = source.name) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
