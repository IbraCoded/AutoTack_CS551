package com.autotrack.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.autotrack.ui.screens.HomeScreen

// Route constants
sealed class Screen(val route: String) {
    // Bottom nav tabs
    object Home      : Screen("home")
    object Records   : Screen("records")
    object Fuel      : Screen("fuel")
    object Services  : Screen("services")
    object Analytics : Screen("analytics")

    // Secondary screens
    object Preferences : Screen("preferences")

    object VehicleDetail : Screen("vehicle_detail/{vehicleId}") {
        fun createRoute(vehicleId: Long) = "vehicle_detail/$vehicleId"
    }

    object AddEditVehicle : Screen("add_edit_vehicle?vehicleId={vehicleId}") {
        fun createRoute(vehicleId: Long? = null) =
            if (vehicleId != null) "add_edit_vehicle?vehicleId=$vehicleId"
            else "add_edit_vehicle"
    }

    object AddEditRecord : Screen("add_edit_record/{vehicleId}?recordId={recordId}") {
        fun createRoute(vehicleId: Long, recordId: Long? = null) =
            if (recordId != null) "add_edit_record/$vehicleId?recordId=$recordId"
            else "add_edit_record/$vehicleId"
    }

    object AddEditFuel : Screen("add_edit_fuel/{vehicleId}?entryId={entryId}") {
        fun createRoute(vehicleId: Long, entryId: Long? = null) =
            if (entryId != null) "add_edit_fuel/$vehicleId?entryId=$entryId"
            else "add_edit_fuel/$vehicleId"
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    androidx.compose.material3.Text(text = "$name screen — coming soon")
}

// NavGraph
@Composable
fun AutoTrackNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Home.route
    ) {
        // Main tabs
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Records.route)   { PlaceholderScreen("Records") }
        composable(Screen.Fuel.route)      { PlaceholderScreen("Fuel") }
        composable(Screen.Services.route)  { PlaceholderScreen("Services") }
        composable(Screen.Analytics.route) { PlaceholderScreen("Analytics") }

        // Preferences
        composable(Screen.Preferences.route) { PlaceholderScreen("Preferences") }

        // Vehicle details
        composable(
            route     = Screen.VehicleDetail.route,
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType }
            )
        ) { PlaceholderScreen("Vehicle Detail") }

        // Add/Edit vehicle
        composable(
            route     = Screen.AddEditVehicle.route,
            arguments = listOf(
                navArgument("vehicleId") {
                    type         = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { PlaceholderScreen("Add/Edit Vehicle") }

        // Add/Edit service record
        composable(
            route     = Screen.AddEditRecord.route,
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("recordId")  {
                    type         = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { PlaceholderScreen("Add/Edit Record") }

        // Add/Edit fuel entry
        composable(
            route     = Screen.AddEditFuel.route,
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("entryId")   {
                    type         = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { PlaceholderScreen("Add/Edit Fuel") }
    }
}