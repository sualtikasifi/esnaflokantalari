package com.esnaflokantalari.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.esnaflokantalari.app.ui.AppViewModel
import com.esnaflokantalari.app.ui.screens.CityScreen
import com.esnaflokantalari.app.ui.screens.CitySearchScreen
import com.esnaflokantalari.app.ui.screens.FavoritesScreen
import com.esnaflokantalari.app.ui.screens.HomeScreen
import com.esnaflokantalari.app.ui.screens.NearbyScreen
import com.esnaflokantalari.app.ui.screens.RestaurantDetailScreen

private object Routes {
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val NEARBY = "nearby"
    const val CITY_SEARCH = "city_search"
    const val CITY = "city/{cityName}"
    const val RESTAURANT = "restaurant/{restaurantId}"

    fun city(cityName: String) = "city/$cityName"
    fun restaurant(restaurantId: String) = "restaurant/$restaurantId"
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "Ana Sayfa", Icons.Filled.Home),
    BottomTab(Routes.NEARBY, "Yakınımda", Icons.Filled.NearMe),
    BottomTab(Routes.FAVORITES, "Favoriler", Icons.Filled.Favorite),
)

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()
    val viewModel: AppViewModel = viewModel()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            val showBottomBar = bottomTabs.any { tab ->
                currentDestination?.hierarchy?.any { it.route == tab.route } == true
            }
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = androidx.compose.ui.Modifier.padding(scaffoldPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onCityClick = { cityName -> navController.navigate(Routes.city(cityName)) },
                    onSearchClick = { navController.navigate(Routes.CITY_SEARCH) },
                )
            }
            composable(Routes.CITY_SEARCH) {
                CitySearchScreen(
                    onBack = { navController.popBackStack() },
                    onCityClick = { cityName -> navController.navigate(Routes.city(cityName)) },
                )
            }
            composable(Routes.FAVORITES) {
                val favorites = favoriteIds.mapNotNull { viewModel.findRestaurant(it) }
                FavoritesScreen(
                    favorites = favorites,
                    onBack = { navController.popBackStack() },
                    onRestaurantClick = { id -> navController.navigate(Routes.restaurant(id)) },
                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                    onExploreClick = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.NEARBY) {
                val state by viewModel.nearbyState().collectAsState()
                NearbyScreen(
                    state = state,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                    onBack = { navController.popBackStack() },
                    onRestaurantClick = { id -> navController.navigate(Routes.restaurant(id)) },
                    onLocationReady = { lat, lng -> viewModel.loadNearby(lat, lng) },
                )
            }
            composable(
                route = Routes.CITY,
                arguments = listOf(navArgument("cityName") { type = NavType.StringType }),
            ) { backStackEntry ->
                val cityName = backStackEntry.arguments?.getString("cityName").orEmpty()
                val state by viewModel.cityState(cityName).collectAsState()
                CityScreen(
                    cityName = cityName,
                    state = state,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                    onBack = { navController.popBackStack() },
                    onRestaurantClick = { id -> navController.navigate(Routes.restaurant(id)) },
                )
            }
            composable(
                route = Routes.RESTAURANT,
                arguments = listOf(navArgument("restaurantId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val restaurantId = backStackEntry.arguments?.getString("restaurantId").orEmpty()
                val restaurant = viewModel.findRestaurant(restaurantId)
                RestaurantDetailScreen(
                    restaurant = restaurant,
                    isFavorite = favoriteIds.contains(restaurantId),
                    onToggleFavorite = { viewModel.toggleFavorite(restaurantId) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
