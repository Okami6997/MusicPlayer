package com.musicplayer.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.remote.jellyfin.JellyfinApi
import com.musicplayer.data.remote.jellyfin.JellyfinClient
import com.musicplayer.data.remote.plex.PlexApi
import com.musicplayer.data.remote.plex.PlexClient
import com.musicplayer.data.remote.subsonic.SubsonicApi
import com.musicplayer.data.remote.subsonic.SubsonicClient
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.MediaSourceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
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
    private val plexClient: PlexClient
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
        viewModelScope.launch {
            _scanState.update { it.copy(isScanning = true, progress = "Syncing ${source.name}...") }
            try {
                val tracks = repository.syncSource(source)
                _scanState.update { it.copy(isScanning = false, progress = "Found ${tracks.size} tracks") }
            } catch (e: Exception) {
                _scanState.update { it.copy(isScanning = false, progress = "Error: ${e.message}") }
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
            MediaSourceType.CLOUD_DRIVE -> {
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
