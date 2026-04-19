package com.musicplayer.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidAutoScreen(
    onNavigateBack: () -> Unit
) {
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
                checked = false,
                onCheckedChange = { }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Auto Play") },
                supportingContent = { Text("Automatically resume playback when connected") },
                leadingContent = { Icon(Icons.Default.PlayCircle, contentDescription = null) }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Shuffle Mode") },
                supportingContent = { Text("Off by default") },
                leadingContent = { Icon(Icons.Default.Shuffle, contentDescription = null) }
            )
            HorizontalDivider()
        }
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