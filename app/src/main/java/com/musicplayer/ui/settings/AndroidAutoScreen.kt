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
fun AndroidAutoScreen(
    onNavigateBack: () -> Unit,
    viewModel: AndroidAutoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android Auto") },
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
                title = "Enable Android Auto",
                subtitle = "Control playback from your car display",
                icon = Icons.Default.DirectionsCar,
                checked = uiState.androidAutoEnabled,
                onCheckedChange = { viewModel.setAndroidAutoEnabled(it) }
            )
            HorizontalDivider()

            SwitchListItem(
                title = "Auto Play",
                subtitle = "Automatically resume playback when connected",
                icon = Icons.Default.PlayCircle,
                checked = uiState.autoPlay,
                onCheckedChange = { viewModel.setAutoPlay(it) }
            )
            HorizontalDivider()

            SwitchListItem(
                title = "Shuffle Mode",
                subtitle = "Enable shuffle mode by default in Android Auto",
                icon = Icons.Default.Shuffle,
                checked = uiState.shuffleMode,
                onCheckedChange = { viewModel.setShuffleMode(it) }
            )
            HorizontalDivider()
        }
    }
}

