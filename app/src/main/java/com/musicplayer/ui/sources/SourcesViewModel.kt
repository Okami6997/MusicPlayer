package com.musicplayer.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.remote.jellyfin.JellyfinApi
import com.musicplayer.data.remote.jellyfin.JellyfinClient
import com.musicplayer.data.remote.plex.PlexApi
import com.musicplayer.data.remote.plex.PlexClient
import com.musicplayer.data.remote.subsonic.SubsonicApi
import com.musicplayer.data.remote.subsonic.SubsonicClient
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.MediaSourceType
import com.musicplayer.worker.DeltaSyncWorker
import com.musicplayer.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SourcesUiState(
    val sources: List<MediaSource> = emptyList(),
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val scanProgress: String = "",
    val testConnectionResult: TestConnectionResult? = null
)

sealed class TestConnectionResult {
    data object Success : TestConnectionResult()
    data class Error(val message: String) : TestConnectionResult()
}

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val subsonicClient: SubsonicClient,
    private val jellyfinClient: JellyfinClient,
    private val plexClient: PlexClient,
    private val workManager: WorkManager
) : ViewModel() {

    private val _scanState = MutableStateFlow(ScanState())

    val uiState: StateFlow<SourcesUiState> = combine(
        repository.getAllSources(),
        _scanState
    ) { sources, scanState ->
        SourcesUiState(
            sources = sources,
            isLoading = false,
            isScanning = scanState.isScanning,
            scanProgress = scanState.progress
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SourcesUiState())

    private val _activeWorkIds = MutableStateFlow<Map<String, UUID>>(emptyMap())

    private data class ScanState(
        val isScanning: Boolean = false,
        val progress: String = ""
    )

    fun addSource(source: MediaSource) {
        viewModelScope.launch {
            repository.saveSource(source)
        }
    }

    fun updateSource(source: MediaSource) {
        viewModelScope.launch {
            repository.saveSource(source)
        }
    }

    fun deleteSource(id: String) {
        viewModelScope.launch {
            repository.deleteSource(id)
        }
    }

    fun scanSource(source: MediaSource) {
        // Default behaviour is now a delta sync (fast, fetches only changes).
        deltaSyncSource(source)
    }

    /**
     * Enqueues a [DeltaSyncWorker] for the given [source] so only the changes
     * since the last sync are downloaded. Falls back to a full sync internally
     * if the source has never been fully synced.
     */
    fun deltaSyncSource(source: MediaSource) {
        val inputData = workDataOf(
            DeltaSyncWorker.KEY_SOURCE_ID to source.id,
            DeltaSyncWorker.KEY_SOURCE_NAME to source.name
        )

        val syncWork = OneTimeWorkRequestBuilder<DeltaSyncWorker>()
            .setInputData(inputData)
            .addTag("delta_sync_${source.id}")
            .build()

        workManager.enqueueUniqueWork(
            "delta_sync_${source.id}",
            ExistingWorkPolicy.REPLACE,
            syncWork
        )

        _activeWorkIds.update { it + (source.id to syncWork.id) }

        _scanState.update { it.copy(isScanning = true, progress = "Syncing ${source.name}...") }

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(syncWork.id).collect { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val added = workInfo.outputData.getInt(DeltaSyncWorker.KEY_ADDED, 0)
                        val updated = workInfo.outputData.getInt(DeltaSyncWorker.KEY_UPDATED, 0)
                        val removed = workInfo.outputData.getInt(DeltaSyncWorker.KEY_REMOVED, 0)
                        val total = workInfo.outputData.getInt(DeltaSyncWorker.KEY_TOTAL_AFTER, 0)
                        _scanState.update {
                            it.copy(
                                isScanning = false,
                                progress = if (added == 0 && updated == 0 && removed == 0) {
                                    "Up to date ($total tracks)"
                                } else {
                                    "Synced +$added ~$updated -$removed ($total total)"
                                }
                            )
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val errorMessage = workInfo.outputData.getString(DeltaSyncWorker.KEY_ERROR_MESSAGE) ?: "Unknown error"
                        _scanState.update { it.copy(isScanning = false, progress = "Error: $errorMessage") }
                    }
                    WorkInfo.State.RUNNING -> {
                        _scanState.update { it.copy(isScanning = true, progress = "Syncing ${source.name}...") }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Enqueues a [SyncWorker] for the given [source], performing a full
     * re-sync that rebuilds the local cache from scratch.
     */
    fun fullSyncSource(source: MediaSource) {
        val inputData = workDataOf(
            SyncWorker.KEY_SOURCE_ID to source.id,
            SyncWorker.KEY_SOURCE_NAME to source.name
        )

        val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(inputData)
            .addTag("full_sync_${source.id}")
            .build()

        workManager.enqueueUniqueWork(
            "full_sync_${source.id}",
            ExistingWorkPolicy.REPLACE,
            syncWork
        )

        _activeWorkIds.update { it + (source.id to syncWork.id) }

        _scanState.update { it.copy(isScanning = true, progress = "Full sync of ${source.name}...") }

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(syncWork.id).collect { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val trackCount = workInfo.outputData.getInt(SyncWorker.KEY_TRACK_COUNT, 0)
                        _scanState.update { it.copy(isScanning = false, progress = "Full sync done: $trackCount tracks") }
                    }
                    WorkInfo.State.FAILED -> {
                        val errorMessage = workInfo.outputData.getString(SyncWorker.KEY_ERROR_MESSAGE) ?: "Unknown error"
                        _scanState.update { it.copy(isScanning = false, progress = "Error: $errorMessage") }
                    }
                    WorkInfo.State.RUNNING -> {
                        _scanState.update { it.copy(isScanning = true, progress = "Full sync of ${source.name}...") }
                    }
                    else -> {}
                }
            }
        }
    }

    fun scanLocalLibrary() {
        viewModelScope.launch {
            _scanState.update { it.copy(isScanning = true, progress = "Scanning local library...") }
            try {
                val tracks = repository.scanLocalLibrary()
                _scanState.update { it.copy(isScanning = false, progress = "Found ${tracks.size} tracks") }
            } catch (e: Exception) {
                _scanState.update { it.copy(isScanning = false, progress = "Error: ${e.message}") }
            }
        }
    }

    suspend fun testConnection(source: MediaSource): TestConnectionResult {
        return when (source.type) {
            MediaSourceType.LOCAL -> {
                TestConnectionResult.Success
            }
            MediaSourceType.JELLYFIN,
            MediaSourceType.EMBY -> {
                testJellyfinConnection(source)
            }
            MediaSourceType.PLEX -> {
                testPlexConnection(source)
            }
            MediaSourceType.SUBSONIC,
            MediaSourceType.OPEN_SUBSONIC,
            MediaSourceType.NAVIDROME -> {
                testSubsonicConnection(source)
            }
            MediaSourceType.AUDIOBOOKSHELF,
            MediaSourceType.CLOUD_DRIVE,
            MediaSourceType.USER -> {
                TestConnectionResult.Error("Not yet supported")
            }
        }
    }

    private fun <T> createApi(source: MediaSource, clazz: Class<T>): T {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(source.baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(clazz)
    }

    private suspend fun testJellyfinConnection(source: MediaSource): TestConnectionResult {
        return try {
            val api = createApi(source, JellyfinApi::class.java)
            val success = jellyfinClient.testConnection(api, source)
            if (success) TestConnectionResult.Success
            else TestConnectionResult.Error("Failed to authenticate with Jellyfin/Emby server")
        } catch (e: Exception) {
            Timber.e(e, "Jellyfin/Emby connection test failed for ${source.name}")
            TestConnectionResult.Error(formatConnectionError(e))
        }
    }

    private suspend fun testPlexConnection(source: MediaSource): TestConnectionResult {
        return try {
            val api = createApi(source, PlexApi::class.java)
            val success = plexClient.testConnection(api, source)
            if (success) TestConnectionResult.Success
            else TestConnectionResult.Error("Failed to authenticate with Plex server")
        } catch (e: Exception) {
            Timber.e(e, "Plex connection test failed for ${source.name}")
            TestConnectionResult.Error(formatConnectionError(e))
        }
    }

    private suspend fun testSubsonicConnection(source: MediaSource): TestConnectionResult {
        return try {
            val api = createApi(source, SubsonicApi::class.java)
            val (token, salt) = subsonicClient.generateToken(source.password)

            Timber.d("Testing connection to ${source.baseUrl} with username ${source.username}")

            val response = api.ping(
                username = source.username,
                token = token,
                salt = salt
            )

            if (response == null) {
                return TestConnectionResult.Error("Empty response from server")
            }
            
            val responseBody = response.response
            if (responseBody == null) {
                return TestConnectionResult.Error("Invalid response structure")
            }
            
            Timber.d("Ping response: status=${responseBody.status}, version=${responseBody.version}")
            
            responseBody.error?.let { error ->
                Timber.e("Ping error: ${error.code} - ${error.message}")
                return TestConnectionResult.Error("${error.message} (code: ${error.code})")
            }

            if (responseBody.status == "ok") {
                TestConnectionResult.Success
            } else {
                TestConnectionResult.Error("Unexpected response status: ${responseBody.status}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Connection test failed for ${source.name}")
            TestConnectionResult.Error(formatConnectionError(e))
        }
    }

    private fun formatConnectionError(e: Exception): String {
        val message = e.message ?: "Connection failed"
        return when {
            message.contains("Unable to resolve host") -> "Cannot reach server. Check URL is correct."
            message.contains("connection refused", ignoreCase = true) -> "Connection refused. Is the server running?"
            message.contains("401") -> "Authentication failed. Check username/password."
            message.contains("403") -> "Access denied. Check credentials."
            message.contains("cleartext") -> "HTTPS required. Use HTTPS URL."
            message.contains("JSON") -> "Invalid response from server."
            message.contains("timeout", ignoreCase = true) -> "Connection timed out. Check URL and server status."
            else -> "Connection failed: $message"
        }
    }

    fun clearTestConnectionResult() {
        // This would clear the result in the UI state
    }
}
