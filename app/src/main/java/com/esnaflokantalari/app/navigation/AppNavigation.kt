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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.esnaflokantalari.app.data.equalsTr
import com.esnaflokantalari.app.ui.AppViewModel
import com.esnaflokantalari.app.ui.screens.CityScreen
import com.esnaflokantalari.app.ui.screens.CitySearchScreen
import com.esnaflokantalari.app.ui.screens.FavoritesScreen
import com.esnaflokantalari.app.ui.screens.HomeScreen
import com.esnaflokantalari.app.ui.screens.NearbyScreen
import com.esnaflokantalari.app.ui.screens.RestaurantDetailScreen
import com.esnaflokantalari.app.ui.screens.SuggestScreen

private object Routes {
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val NEARBY = "nearby"
    const val CITY_SEARCH = "city_search"
    const val CITY = "city/{cityName}"
    const val SUGGEST = "suggest/{cityName}"
    const val RESTAURANT = "restaurant/{restaurantId}"

    fun city(cityName: String) = "city/$cityName"
    fun suggest(cityName: String) = "suggest/$cityName"
    fun restaurant(restaurantId: String) = "restaurant/$restaurantId"
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "Ana Sayfa", Icons.Filled.Home),
    BottomTab(Routes.NEARBY, "Yakınımda", Icons.Filled.NearMe),
    BottomTab(Routes.FAVORITES, "Favoriler", Icons.Filled.Favorite),
)

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()
    val viewModel: AppViewModel = viewModel()

    val cities by viewModel.cities.collectAsState()
    val featured by viewModel.featured.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val dataUpdatedAt by viewModel.dataUpdatedAt.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    fun goToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            val onTab = bottomTabs.any { tab ->
                currentDestination?.hierarchy?.any { it.route == tab.route } == true
            }
            if (onTab) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { goToTab(tab.route) },
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
            modifier = Modifier.padding(scaffoldPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    cities = cities,
                    featured = featured,
                    dataUpdatedAt = dataUpdatedAt,
                    onCityClick = { navController.navigate(Routes.city(it)) },
                    onSearchClick = { navController.navigate(Routes.CITY_SEARCH) },
                    onRestaurantClick = { navController.navigate(Routes.restaurant(it)) },
                    onSurpriseMe = {
                        featured.randomOrNull()?.let { navController.navigate(Routes.restaurant(it.id)) }
                    },
                )
            }

            composable(Routes.CITY_SEARCH) {
                CitySearchScreen(
                    cities = cities,
                    onBack = { navController.popBackStack() },
                    onCityClick = { navController.navigate(Routes.city(it)) },
                )
            }

            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    favorites = favorites,
                    onRestaurantClick = { navController.navigate(Routes.restaurant(it)) },
                    onToggleFavorite = { viewModel.toggleFavoriteById(it) },
                    onExploreClick = { goToTab(Routes.HOME) },
                )
            }

            composable(Routes.NEARBY) {
                val nearby by viewModel.nearby.collectAsState()
                NearbyScreen(
                    state = nearby,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { viewModel.toggleFavoriteById(it) },
                    onRestaurantClick = { navController.navigate(Routes.restaurant(it)) },
                    onPermissionDenied = viewModel::onLocationPermissionDenied,
                    onLocatingStarted = viewModel::onLocatingStarted,
                    onLocationUnavailable = viewModel::onLocationUnavailable,
                    onLocationReady = viewModel::loadNearby,
                )
            }

            composable(
                route = Routes.CITY,
                arguments = listOf(navArgument("cityName") { type = NavType.StringType }),
            ) { entry ->
                val cityName = entry.arguments?.getString("cityName").orEmpty()
                CityScreen(
                    cityName = cityName,
                    city = viewModel.city(cityName),
                    favoriteIds = favoriteIds,
                    suggestionCount = suggestions.count { it.city.equalsTr(cityName) },
                    onToggleFavorite = { viewModel.toggleFavoriteById(it) },
                    onBack = { navController.popBackStack() },
                    onRestaurantClick = { navController.navigate(Routes.restaurant(it)) },
                    onSuggestClick = { navController.navigate(Routes.suggest(cityName)) },
                )
            }

            composable(
                route = Routes.SUGGEST,
                arguments = listOf(navArgument("cityName") { type = NavType.StringType }),
            ) { entry ->
                val cityName = entry.arguments?.getString("cityName").orEmpty()
                SuggestScreen(
                    cityName = cityName,
                    suggestions = suggestions.filter { it.city.equalsTr(cityName) },
                    onBack = { navController.popBackStack() },
                    onSubmit = { name, category, address, note ->
                        viewModel.addSuggestion(cityName, name, category, address, note)
                    },
                    onDelete = viewModel::removeSuggestion,
                    onMarkSent = viewModel::markSuggestionSent,
                )
            }

            composable(
                route = Routes.RESTAURANT,
                arguments = listOf(navArgument("restaurantId") { type = NavType.StringType }),
            ) { entry ->
                val restaurantId = entry.arguments?.getString("restaurantId").orEmpty()
                val restaurant = viewModel.findRestaurant(restaurantId)
                RestaurantDetailScreen(
                    restaurant = restaurant,
                    isFavorite = favoriteIds.contains(restaurantId),
                    dataUpdatedAt = dataUpdatedAt,
                    onToggleFavorite = { restaurant?.let(viewModel::toggleFavorite) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
