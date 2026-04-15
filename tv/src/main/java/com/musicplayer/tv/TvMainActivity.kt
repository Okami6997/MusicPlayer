package com.musicplayer.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import dagger.hilt.android.AndroidEntryPoint

/**
 * Android TV entry point — a D-pad-navigable music browser built with TV Compose Material.
 */
@AndroidEntryPoint
class TvMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvMusicPlayerApp()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMusicPlayerApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(32.dp)) {
                Text(
                    text = "Music Player",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Navigate with D-pad • OK to select",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {}) {
                    Text("Browse Library")
                }
            }
        }
    }
}
