package com.musicplayer.ui.settings

import android.content.Context
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
import java.io.File
import javax.inject.Inject

data class DownloadsUiState(
    val downloadLocation: String = "",
    val downloadQuality: String = "High (320 kbps)",
    val storageUsed: String = "0 MB"
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        val KEY_DOWNLOAD_LOCATION = stringPreferencesKey("download_location")
        val KEY_DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
    }

    var showDownloadLocationDialog by mutableStateOf(false)

    val uiState: StateFlow<DownloadsUiState> = dataStore.data
        .map { prefs ->
            val cacheSize = getCacheSize()
            DownloadsUiState(
                downloadLocation = prefs[KEY_DOWNLOAD_LOCATION] ?: "Internal storage",
                downloadQuality = prefs[KEY_DOWNLOAD_QUALITY] ?: "High (320 kbps)",
                storageUsed = cacheSize
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    fun setDownloadQuality(quality: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_DOWNLOAD_QUALITY] = quality }
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
