package com.musicplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.musicplayer.ui.home.HomeScreen
import com.musicplayer.ui.library.LibraryScreen
import com.musicplayer.ui.player.PlayerScreen
import com.musicplayer.ui.search.SearchScreen
import com.musicplayer.ui.settings.SettingsScreen
import com.musicplayer.ui.sources.SourcesScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Library : Screen("library")
    data object Player : Screen("player")
    data object Search : Screen("search")
    data object Settings : Screen("settings")
    data object Sources : Screen("sources")
}

@Composable
fun MusicPlayerNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPlayer = { navController.navigate(Screen.Player.route) },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateToPlayer = { navController.navigate(Screen.Player.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Player.route) {
            PlayerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToPlayer = { navController.navigate(Screen.Player.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToSources = { navController.navigate(Screen.Sources.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Sources.route) {
            SourcesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
