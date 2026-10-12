package com.example.practice.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.practice.ui.screens.detail.TrackDetailScreen
import com.example.practice.ui.screens.favorites.FavoritesScreen
import com.example.practice.ui.screens.main.MainScreen
import com.example.practice.ui.screens.profile.ProfileScreen
import com.example.practice.ui.screens.tracks.TrackListRoute
import com.example.practice.viewmodel.TrackViewModel

@Composable
fun AppNavigation(
    viewModel: TrackViewModel,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val selectedTab = Screen.bottomItems.firstOrNull { it.route == route }

    MainScreen(
        selectedTab = selectedTab,
        onBottomItemClick = { screen ->
            if (screen.route != route) {
                navController.navigate(screen.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Tracks.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Tracks.route) {
                TrackListRoute(
                    viewModel = viewModel,
                    onTrackClick = { trackId ->
                        navController.navigate(Screen.TrackDetail.routeFor(trackId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Favorites.route) { FavoritesScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(
                route = Screen.TrackDetail.route,
                arguments = listOf(navArgument(Screen.TrackDetail.TRACK_ID) { type = NavType.LongType })
            ) { entry ->
                val trackId = entry.arguments?.getLong(Screen.TrackDetail.TRACK_ID)
                TrackDetailScreen(
                    track = trackId?.let(viewModel::getTrackById),
                    onBackClick = { navController.navigateUp() }
                )
            }
        }
    }
}
