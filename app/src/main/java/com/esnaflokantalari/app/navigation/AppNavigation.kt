package com.esnaflokantalari.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.esnaflokantalari.app.ui.AppViewModel
import com.esnaflokantalari.app.ui.screens.CityScreen
import com.esnaflokantalari.app.ui.screens.FavoritesScreen
import com.esnaflokantalari.app.ui.screens.HomeScreen
import com.esnaflokantalari.app.ui.screens.NearbyScreen
import com.esnaflokantalari.app.ui.screens.RestaurantDetailScreen

private object Routes {
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val NEARBY = "nearby"
    const val CITY = "city/{cityName}"
    const val RESTAURANT = "restaurant/{restaurantId}"

    fun city(cityName: String) = "city/$cityName"
    fun restaurant(restaurantId: String) = "restaurant/$restaurantId"
}

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()
    val viewModel: AppViewModel = viewModel()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onCityClick = { cityName -> navController.navigate(Routes.city(cityName)) },
                onFavoritesClick = { navController.navigate(Routes.FAVORITES) },
                onNearbyClick = { navController.navigate(Routes.NEARBY) },
            )
        }
        composable(Routes.FAVORITES) {
            val favorites = favoriteIds.mapNotNull { viewModel.findRestaurant(it) }
            FavoritesScreen(
                favorites = favorites,
                onBack = { navController.popBackStack() },
                onRestaurantClick = { id -> navController.navigate(Routes.restaurant(id)) },
            )
        }
        composable(Routes.NEARBY) {
            val state by viewModel.nearbyState().collectAsState()
            NearbyScreen(
                state = state,
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
