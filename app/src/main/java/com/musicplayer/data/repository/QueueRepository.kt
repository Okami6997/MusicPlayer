package com.musicplayer.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.musicplayer.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.queueDataStore: DataStore<Preferences> by preferencesDataStore(name = "queue_state")

data class SavedQueueState(
    val tracks: List<Track>,
    val currentIndex: Int,
    val currentPositionMs: Long,
)

@Singleton
class QueueRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val gson = Gson()

    companion object {
        private val KEY_QUEUE_JSON = stringPreferencesKey("queue_json")
        private val KEY_CURRENT_INDEX = intPreferencesKey("current_index")
        private val KEY_POSITION_MS = longPreferencesKey("position_ms")
    }

    suspend fun saveQueueState(tracks: List<Track>, index: Int, positionMs: Long = 0L) {
        if (tracks.isEmpty()) return
        try {
            val json = gson.toJson(tracks)
            context.queueDataStore.edit { prefs ->
                prefs[KEY_QUEUE_JSON] = json
                prefs[KEY_CURRENT_INDEX] = index
                prefs[KEY_POSITION_MS] = positionMs
            }
            Timber.d("Saved queue: ${tracks.size} tracks, index=$index, pos=$positionMs ms")
        } catch (e: Exception) {
            Timber.w(e, "Failed to save queue state")
        }
    }

    suspend fun loadQueueState(): SavedQueueState? {
        return try {
            val prefs = context.queueDataStore.data.first()
            val json = prefs[KEY_QUEUE_JSON] ?: return null
            val index = prefs[KEY_CURRENT_INDEX] ?: 0
            val positionMs = prefs[KEY_POSITION_MS] ?: 0L
            val type = object : TypeToken<List<Track>>() {}.type
            val tracks: List<Track> = gson.fromJson(json, type) ?: return null
            SavedQueueState(tracks, index, positionMs)
        } catch (e: Exception) {
            Timber.w(e, "Failed to load queue state")
            null
        }
    }
}
