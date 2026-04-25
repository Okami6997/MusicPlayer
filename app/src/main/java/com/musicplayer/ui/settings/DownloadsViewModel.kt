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
import java.io.File
import javax.inject.Inject

data class DownloadsUiState(
    val downloadLocation: String = "",
    val downloadQuality: String = "Lossless",
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
            val location = prefs[KEY_DOWNLOAD_LOCATION] ?: "Default (App internal)"
            val displayLocation = if (location.startsWith("content://")) {
                Uri.parse(location).path?.split(":")?.lastOrNull() ?: "Custom Folder"
            } else {
                location
            }
            DownloadsUiState(
                downloadLocation = displayLocation,
                downloadQuality = prefs[KEY_DOWNLOAD_QUALITY] ?: "Lossless",
                storageUsed = cacheSize
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

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
