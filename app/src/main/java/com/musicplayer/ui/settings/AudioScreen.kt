package com.musicplayer.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScreen(
    onNavigateToEqualizer: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AudioSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCrossfadeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SwitchListItem(
                title = "Gapless Playback",
                subtitle = "Remove silence between tracks",
                icon = Icons.Default.MusicNote,
                checked = uiState.gaplessPlayback,
                onCheckedChange = { viewModel.setGaplessPlayback(it) }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Equalizer") },
                supportingContent = { Text(if (uiState.equalizerEnabled) "On" else "Off") },
                leadingContent = { Icon(Icons.Default.Equalizer, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToEqualizer() }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Crossfade") },
                supportingContent = { Text("${uiState.crossfadeDurationMs} ms") },
                leadingContent = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { showCrossfadeDialog = true }
            )
            HorizontalDivider()
        }
    }

    if (showCrossfadeDialog) {
        AlertDialog(
            onDismissRequest = { showCrossfadeDialog = false },
            title = { Text("Crossfade Duration") },
            text = {
                Column {
                    listOf(0, 1000, 2000, 3000, 5000).forEach { duration ->
                        ListItem(
                            headlineContent = { Text(if (duration == 0) "Off" else "$duration ms") },
                            modifier = Modifier.clickable {
                                viewModel.setCrossfadeDuration(duration)
                                showCrossfadeDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCrossfadeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SwitchListItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}