package com.musicplayer.ui.newui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.ui.settings.SettingsViewModel
import com.musicplayer.ui.settings.SwitchListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewUiSettingsScreen(
    onNavigateToSources: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New UI Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Profiles") },
                    supportingContent = { Text("Manage media server connections") },
                    leadingContent = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSources() }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Appearance") },
                    supportingContent = { Text("Theme: ${uiState.themeMode.replaceFirstChar { it.uppercase() }}, Dynamic Color: ${if (uiState.dynamicColorEnabled) "On" else "Off"}") },
                    leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAppearance() }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Audio") },
                    supportingContent = { Text("Crossfade, gapless playback") },
                    leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAudio() }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Offline") },
                    supportingContent = { Text("Manage offline cache and downloads") },
                    leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDownloads() }
                )
                HorizontalDivider()
            }

            item {
                SwitchListItem(
                    title = "Dynamic Color",
                    subtitle = "Use Material You colors from your wallpaper",
                    icon = Icons.Default.ColorLens,
                    checked = uiState.dynamicColorEnabled,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )
                HorizontalDivider()
            }

            item {
                SwitchListItem(
                    title = "Gapless Playback",
                    subtitle = "Remove silence between tracks",
                    icon = Icons.Default.MusicNote,
                    checked = uiState.gaplessPlayback,
                    onCheckedChange = { viewModel.setGaplessPlayback(it) }
                )
                HorizontalDivider()
            }
        }
    }
}