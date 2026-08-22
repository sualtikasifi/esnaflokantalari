package com.esnaflokantalari.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.esnaflokantalari.app.ui.screens.CityScreen
import com.esnaflokantalari.app.ui.screens.FavoritesScreen
import com.esnaflokantalari.app.ui.screens.HomeScreen
import com.esnaflokantalari.app.ui.screens.RestaurantDetailScreen

private object Routes {
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val CITY = "city/{cityName}"
    const val RESTAURANT = "restaurant/{restaurantId}"

    fun city(cityName: String) = "city/$cityName"
    fun restaurant(restaurantId: String) = "restaurant/$restaurantId"
}

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onCityClick = { cityName -> navController.navigate(Routes.city(cityName)) },
                onFavoritesClick = { navController.navigate(Routes.FAVORITES) },
            )
        }
        composable(Routes.FAVORITES) {
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onRestaurantClick = { id -> navController.navigate(Routes.restaurant(id)) },
            )
        }
        composable(
            route = Routes.CITY,
            arguments = listOf(navArgument("cityName") { type = NavType.StringType }),
        ) { backStackEntry ->
            val cityName = backStackEntry.arguments?.getString("cityName").orEmpty()
            CityScreen(
                cityName = cityName,
                onBack = { navController.popBackStack() },
                onRestaurantClick = { id -> navController.navigate(Routes.restaurant(id)) },
            )
        }
        composable(
            route = Routes.RESTAURANT,
            arguments = listOf(navArgument("restaurantId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val restaurantId = backStackEntry.arguments?.getString("restaurantId").orEmpty()
            RestaurantDetailScreen(
                restaurantId = restaurantId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
