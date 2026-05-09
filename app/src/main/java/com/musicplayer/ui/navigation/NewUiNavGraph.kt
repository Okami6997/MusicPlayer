package com.musicplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.musicplayer.ui.newui.NewUiHomeScreen
import com.musicplayer.ui.newui.ProfileListScreen
import com.musicplayer.ui.newui.ProfileEditScreen
import com.musicplayer.ui.download.DownloadMusicScreen
import com.musicplayer.ui.library.LibraryScreen
import com.musicplayer.ui.library.AlbumDetailScreen
import com.musicplayer.ui.library.ArtistDetailScreen
import com.musicplayer.ui.library.PlaylistDetailScreen
import com.musicplayer.ui.newui.NewUiSettingsScreen
import com.musicplayer.ui.player.PlayerScreen
import com.musicplayer.ui.player.PlayerViewModel
import com.musicplayer.ui.search.SearchScreen
import com.musicplayer.ui.settings.AppearanceScreen
import com.musicplayer.ui.settings.AudioScreen
import com.musicplayer.ui.settings.DownloadsScreen
import com.musicplayer.ui.settings.SettingsScreen
import com.musicplayer.ui.settings.AndroidAutoScreen
import com.musicplayer.ui.sources.SourcesScreen

sealed class NewUiScreen(val route: String) {
    data object Home : NewUiScreen("new_ui_home")
    data object ProfileList : NewUiScreen("profile_list")
    data object ProfileEdit : NewUiScreen("profile_edit/{profileId}") {
        fun createRoute(profileId: String?) = if (profileId != null) "profile_edit/$profileId" else "profile_edit/null"
    }
    data object DownloadMusic : NewUiScreen("new_ui_download_music")
    data object Library : NewUiScreen("new_ui_library")
    data object Player : NewUiScreen("new_ui_player")
    data object Search : NewUiScreen("new_ui_search")
    data object Settings : NewUiScreen("new_ui_settings")
    data object Sources : NewUiScreen("new_ui_sources")
    data object Appearance : NewUiScreen("new_ui_appearance")
    data object Audio : NewUiScreen("new_ui_audio")
    data object Downloads : NewUiScreen("new_ui_downloads")
    data object AndroidAuto : NewUiScreen("new_ui_android_auto")
    data object AlbumDetail : NewUiScreen("new_ui_album_detail/{albumId}") {
        fun createRoute(albumId: String) = "new_ui_album_detail/$albumId"
    }
    data object ArtistDetail : NewUiScreen("new_ui_artist_detail/{artistName}") {
        fun createRoute(artistName: String) = "new_ui_artist_detail/$artistName"
    }
    data object PlaylistDetail : NewUiScreen("new_ui_playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "new_ui_playlist_detail/$playlistId"
    }
}

@Composable
fun NewUiNavGraph(
    navController: NavHostController = rememberNavController(),
    onSwitchToOldUi: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = NewUiScreen.Home.route
    ) {
        // Home
        composable(NewUiScreen.Home.route) {
            NewUiHomeScreen(
                onNavigateToProfiles = {
                    navController.navigate(NewUiScreen.ProfileList.route)
                },
                onNavigateToDownloadMusic = {
                    navController.navigate(NewUiScreen.DownloadMusic.route)
                },
                onNavigateToLibrary = {
                    navController.navigate(NewUiScreen.Library.route)
                },
                onNavigateToSearch = {
                    navController.navigate(NewUiScreen.Search.route)
                },
                onNavigateToSettings = {
                    navController.navigate(NewUiScreen.Settings.route)
                },
                onNavigateToPlayer = {
                    navController.navigate(NewUiScreen.Player.route)
                },
                onSwitchToOldUi = onSwitchToOldUi
            )
        }

        // Profile management
        composable(NewUiScreen.ProfileList.route) {
            ProfileListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { profileId ->
                    navController.navigate(NewUiScreen.ProfileEdit.createRoute(profileId))
                }
            )
        }

        composable(
            route = NewUiScreen.ProfileEdit.route,
            arguments = listOf(
                navArgument("profileId") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId")?.takeIf { it != "null" }
            ProfileEditScreen(
                profileId = profileId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Download music
        composable(NewUiScreen.DownloadMusic.route) {
            DownloadMusicScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(NewUiScreen.Downloads.route) }
            )
        }

        // Library
        composable(NewUiScreen.Library.route) {
            val playerViewModel: PlayerViewModel = hiltViewModel()
            LibraryScreen(
                onNavigateToPlayer = { navController.navigate(NewUiScreen.Player.route) },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(NewUiScreen.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistName ->
                    navController.navigate(NewUiScreen.ArtistDetail.createRoute(artistName))
                },
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate(NewUiScreen.PlaylistDetail.createRoute(playlistId))
                },
                onNavigateBack = { navController.popBackStack() },
                playerViewModel = playerViewModel
            )
        }

        composable(NewUiScreen.AlbumDetail.route) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
            AlbumDetailScreen(
                albumId = albumId,
                onNavigateToPlayer = { navController.navigate(NewUiScreen.Player.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NewUiScreen.PlaylistDetail.route) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            PlaylistDetailScreen(
                playlistId = playlistId,
                onNavigateToPlayer = { navController.navigate(NewUiScreen.Player.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NewUiScreen.ArtistDetail.route) { backStackEntry ->
            val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
            ArtistDetailScreen(
                artistName = artistName,
                onNavigateToPlayer = { navController.navigate(NewUiScreen.Player.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Player
        composable(NewUiScreen.Player.route) {
            PlayerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Search
        composable(NewUiScreen.Search.route) {
            SearchScreen(
                onNavigateToPlayer = { navController.navigate(NewUiScreen.Player.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings
        composable(NewUiScreen.Settings.route) {
            NewUiSettingsScreen(
                onNavigateToSources = { navController.navigate(NewUiScreen.ProfileList.route) },
                onNavigateToAppearance = { navController.navigate(NewUiScreen.Appearance.route) },
                onNavigateToAudio = { navController.navigate(NewUiScreen.Audio.route) },
                onNavigateToDownloads = { navController.navigate(NewUiScreen.Downloads.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NewUiScreen.Sources.route) {
            SourcesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NewUiScreen.Appearance.route) {
            AppearanceScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NewUiScreen.Audio.route) {
            AudioScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NewUiScreen.Downloads.route) {
            DownloadsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NewUiScreen.AndroidAuto.route) {
            AndroidAutoScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}