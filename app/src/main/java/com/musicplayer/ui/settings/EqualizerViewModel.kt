package com.musicplayer.ui.settings

import android.media.audiofx.Equalizer
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EqualizerBand(
    val index: Int,
    val centerFrequency: Int,
    val level: Int,
    val minLevel: Int,
    val maxLevel: Int
)

data class EqualizerUiState(
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList()
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(EqualizerUiState())
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    init {
        // We use a temporary equalizer instance just to get band info
        // This is safe as long as we don't attach it to session 0 for too long
        var tempEq: Equalizer? = null
        try {
            tempEq = Equalizer(0, 0)
            val numBands = tempEq.numberOfBands.toInt()
            val minLevel = tempEq.bandLevelRange[0].toInt()
            val maxLevel = tempEq.bandLevelRange[1].toInt()
            
            val initialBands = (0 until numBands).map { i ->
                EqualizerBand(
                    index = i,
                    centerFrequency = tempEq.getCenterFreq(i.toShort()) / 1000,
                    level = 0,
                    minLevel = minLevel,
                    maxLevel = maxLevel
                )
            }

            viewModelScope.launch {
                dataStore.data.collect { prefs ->
                    val isEnabled = prefs[SettingsKeys.EQUALIZER_ENABLED] ?: false
                    val bandsStr = prefs[SettingsKeys.EQUALIZER_BANDS] ?: ""
                    val bandLevels = if (bandsStr.isNotEmpty()) {
                        bandsStr.split(",").associate {
                            val parts = it.split(":")
                            parts[0].toInt() to parts[1].toInt()
                        }
                    } else emptyMap()

                    _uiState.update { state ->
                        state.copy(
                            isEnabled = isEnabled,
                            bands = initialBands.map { band ->
                                band.copy(level = bandLevels[band.index] ?: 0)
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error (e.g., no equalizer support)
        } finally {
            tempEq?.release()
        }
    }

    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[SettingsKeys.EQUALIZER_ENABLED] = enabled }
        }
    }

    fun updateBandLevel(bandIndex: Int, level: Int) {
        viewModelScope.launch {
            val currentBands = _uiState.value.bands.toMutableList()
            val bandIdx = currentBands.indexOfFirst { it.index == bandIndex }
            if (bandIdx != -1) {
                currentBands[bandIdx] = currentBands[bandIdx].copy(level = level)
                val bandsStr = currentBands.joinToString(",") { "${it.index}:${it.level}" }
                dataStore.edit { it[SettingsKeys.EQUALIZER_BANDS] = bandsStr }
            }
        }
    }
}
