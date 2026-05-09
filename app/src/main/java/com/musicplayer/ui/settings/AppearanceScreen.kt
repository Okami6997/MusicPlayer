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
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
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
                title = "Dynamic Color",
                subtitle = "Use Material You colors from your wallpaper",
                icon = Icons.Default.ColorLens,
                checked = uiState.dynamicColorEnabled,
                onCheckedChange = { viewModel.setDynamicColor(it) }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text(uiState.themeMode.replaceFirstChar { it.uppercase() }) },
                leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { showThemeDialog = true }
            )
            HorizontalDivider()
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    listOf("system", "light", "dark").forEach { mode ->
                        ListItem(
                            headlineContent = { Text(mode.replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.clickable {
                                viewModel.setThemeMode(mode)
                                showThemeDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

