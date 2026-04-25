package com.musicplayer.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onNavigateBack: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Equalizer", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = uiState.isEnabled,
                    onCheckedChange = { viewModel.toggleEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.bands.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Equalizer not supported on this device")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.bands) { band ->
                        BandControl(
                            band = band,
                            enabled = uiState.isEnabled,
                            onLevelChange = { viewModel.updateBandLevel(band.index, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BandControl(
    band: EqualizerBand,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit
) {
    var sliderValue by remember(band.level) { mutableStateOf(band.level.toFloat()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (band.centerFrequency >= 1000) 
                    "${band.centerFrequency / 1000} kHz" 
                else 
                    "${band.centerFrequency} Hz",
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = "${sliderValue.toInt() / 100} dB",
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onLevelChange(sliderValue.toInt()) },
            valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
            enabled = enabled
        )
    }
}
