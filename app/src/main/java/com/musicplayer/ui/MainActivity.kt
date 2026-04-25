package com.musicplayer.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.musicplayer.service.PlayerHolder
import dagger.hilt.android.AndroidEntryPoint
import com.musicplayer.ui.theme.MusicPlayerTheme
import com.musicplayer.ui.navigation.MusicPlayerNavGraph
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    @OptIn(UnstableApi::class)
    lateinit var playerHolder: PlayerHolder

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val controller = rememberNavController()
            navController = controller
            MusicPlayerTheme {
                MusicPlayerNavGraph(navController = controller)
            }
        }
        
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navController?.handleDeepLink(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                if (uri.scheme == "content" || uri.scheme == "file") {
                    playerHolder.playUri(uri)
                }
            }
        }
    }
}
