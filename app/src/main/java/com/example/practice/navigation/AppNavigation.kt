package com.example.practice.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.practice.ui.screens.detail.TrackDetailRoute
import com.example.practice.ui.screens.favorites.FavoritesRoute
import com.example.practice.ui.screens.filters.FiltersRoute
import com.example.practice.ui.screens.main.MainScreen
import com.example.practice.ui.screens.profile.ProfileRoute
import com.example.practice.ui.screens.profile.ProfileEditRoute
import com.example.practice.ui.screens.tracks.TrackListRoute
import com.example.practice.viewmodel.TrackViewModel
import com.example.practice.viewmodel.TrackDetailViewModel
import com.example.practice.viewmodel.TrackViewModelFactory
import com.example.practice.viewmodel.FavoritesViewModel
import com.example.practice.viewmodel.FiltersViewModel

@Composable
fun AppNavigation(
    viewModel: TrackViewModel,
    viewModelFactory: TrackViewModelFactory,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val selectedTab = Screen.bottomItems.firstOrNull { it.route == route }
    val showFilterBadge by viewModel.showFilterBadge.collectAsStateWithLifecycle()
    val favoritesViewModel: FavoritesViewModel = viewModel(factory = viewModelFactory)

    MainScreen(
        selectedTab = selectedTab,
        showFilterBadge = showFilterBadge,
        onEditProfile = { navController.navigate(Screen.ProfileEdit.route) { launchSingleTop = true } },
        onFiltersClick = { navController.navigate(Screen.Filters.route) { launchSingleTop = true } },
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
            composable(Screen.Favorites.route) {
                FavoritesRoute(favoritesViewModel) { trackId ->
                    navController.navigate(Screen.TrackDetail.routeFor(trackId)) { launchSingleTop = true }
                }
            }
            composable(Screen.Profile.route) { ProfileRoute() }
            composable(Screen.ProfileEdit.route) { ProfileEditRoute(onBack = { navController.navigateUp() }) }
            composable(Screen.Filters.route) { entry ->
                val filtersViewModel: FiltersViewModel = viewModel(viewModelStoreOwner = entry, factory = viewModelFactory)
                FiltersRoute(
                    viewModel = filtersViewModel,
                    onBack = { navController.navigateUp() },
                    onApplied = {
                        viewModel.onFiltersApplied()
                        navController.navigateUp()
                    }
                )
            }
            composable(
                route = Screen.TrackDetail.route,
                arguments = listOf(navArgument(Screen.TrackDetail.TRACK_ID) { type = NavType.LongType })
            ) { entry ->
                val detailViewModel: TrackDetailViewModel = viewModel(
                    viewModelStoreOwner = entry, factory = viewModelFactory
                )
                TrackDetailRoute(
                    viewModel = detailViewModel,
                    favoritesViewModel = favoritesViewModel,
                    onBackClick = { navController.navigateUp() }
                )
            }
        }
    }
}
