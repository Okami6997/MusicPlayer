package com.musicplayer.ui.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class DownloadsUiState(
    val downloadLocation: String = "",
    val downloadQuality: String = "Lossless",
    val storageUsed: String = "0 MB",
    val downloadSourceUrl: String = "",
    val isTestingUrl: Boolean = false,
    val testUrlResult: String? = null
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        val KEY_DOWNLOAD_LOCATION = stringPreferencesKey("download_location")
        val KEY_DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val KEY_DOWNLOAD_SOURCE_URL = stringPreferencesKey("download_source_url")
    }

    private val _testResult = MutableStateFlow<String?>(null)
    private val _isTesting = MutableStateFlow(false)

    val uiState: StateFlow<DownloadsUiState> = combine(
        dataStore.data,
        _isTesting,
        _testResult
    ) { prefs, isTesting, testResult ->
        val cacheSize = getCacheSize()
        val location = prefs[KEY_DOWNLOAD_LOCATION] ?: "Default (App internal)"
        val displayLocation = if (location.startsWith("content://")) {
            Uri.parse(location).path?.split(":")?.lastOrNull() ?: "Custom Folder"
        } else {
            location
        }
        DownloadsUiState(
            downloadLocation = displayLocation,
            downloadQuality = prefs[KEY_DOWNLOAD_QUALITY] ?: "Lossless",
            storageUsed = cacheSize,
            downloadSourceUrl = prefs[KEY_DOWNLOAD_SOURCE_URL] ?: "",
            isTestingUrl = isTesting,
            testUrlResult = testResult
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    fun testUrl(url: String) {
        if (url.isBlank()) return
        
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null
            
            try {
                val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "http://$url"
                } else {
                    url
                }
                
                val result = withContext(Dispatchers.IO) {
                    try {
                        val request = Request.Builder()
                            .url(formattedUrl)
                            .head() // Use HEAD request for efficiency
                            .build()
                        
                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                "Success: URL is reachable"
                            } else {
                                "Error: Server returned ${response.code}"
                            }
                        }
                    } catch (e: Exception) {
                        "Error: ${e.localizedMessage ?: "Connection failed"}"
                    }
                }
                _testResult.value = result
            } catch (e: Exception) {
                _testResult.value = "Invalid URL format"
            } finally {
                _isTesting.value = false
            }
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    fun setDownloadLocation(uri: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_DOWNLOAD_LOCATION] = uri }
        }
    }

    fun setDownloadQuality(quality: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_DOWNLOAD_QUALITY] = quality }
        }
    }

    fun setDownloadSourceUrl(url: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_DOWNLOAD_SOURCE_URL] = url }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                context.cacheDir.deleteRecursively()
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }

    private fun getCacheSize(): String {
        return try {
            val cacheDir = context.cacheDir
            val size = cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            formatSize(size)
        } catch (e: Exception) {
            "0 MB"
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
