package com.musicplayer.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.wear.compose.material.*
import dagger.hilt.android.AndroidEntryPoint

/**
 * Wear OS entry point — shows a compact now-playing card and playback controls.
 */
@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearPlayerScreen()
        }
    }
}

@Composable
fun WearPlayerScreen() {
    MaterialTheme {
        ScalingLazyColumn(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            item {
                Text(
                    text = "Music Player",
                    style = MaterialTheme.typography.title2,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = {},
                    label = { Text("Play / Pause") },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
                    CompactChip(onClick = {}, label = { Text("Prev") })
                    CompactChip(onClick = {}, label = { Text("Next") })
                }
            }
        }
    }
}
