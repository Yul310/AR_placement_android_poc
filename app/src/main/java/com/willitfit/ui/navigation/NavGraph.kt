package com.willitfit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.willitfit.data.model.DoorMeasurement
import com.willitfit.data.model.MeasurementMode
import com.willitfit.data.model.Product
import com.willitfit.data.model.SpaceMeasurement
import com.willitfit.data.model.Verdict
import com.willitfit.ui.home.HomeScreen
import com.willitfit.ui.measurement.ARMeasurementScreen
import com.willitfit.ui.placement.VirtualPlacementScreen
import com.willitfit.ui.result.ResultScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Measurement : Screen("measurement")
    object Placement : Screen("placement")
    object Result : Screen("result")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    // Shared state for navigation
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var selectedMode by remember { mutableStateOf(MeasurementMode.DOOR) }
    var currentVerdict by remember { mutableStateOf<Verdict?>(null) }
    var doorMeasurement by remember { mutableStateOf<DoorMeasurement?>(null) }
    var spaceMeasurement by remember { mutableStateOf<SpaceMeasurement?>(null) }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToMeasurement = { product, mode ->
                    selectedProduct = product
                    selectedMode = mode
                    currentVerdict = null
                    doorMeasurement = null
                    spaceMeasurement = null

                    when (mode) {
                        MeasurementMode.VIRTUAL_PLACEMENT -> {
                            navController.navigate(Screen.Placement.route)
                        }
                        else -> {
                            navController.navigate(Screen.Measurement.route)
                        }
                    }
                }
            )
        }

        composable(Screen.Measurement.route) {
            selectedProduct?.let { product ->
                ARMeasurementScreen(
                    product = product,
                    mode = selectedMode,
                    onComplete = { verdict ->
                        currentVerdict = verdict
                        navController.navigate(Screen.Result.route) {
                            popUpTo(Screen.Measurement.route) { inclusive = true }
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.Placement.route) {
            selectedProduct?.let { product ->
                VirtualPlacementScreen(
                    product = product,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.Result.route) {
            selectedProduct?.let { product ->
                ResultScreen(
                    product = product,
                    mode = selectedMode,
                    verdict = currentVerdict,
                    doorMeasurement = doorMeasurement,
                    spaceMeasurement = spaceMeasurement,
                    onMeasureAgain = {
                        currentVerdict = null
                        doorMeasurement = null
                        spaceMeasurement = null
                        navController.navigate(Screen.Measurement.route) {
                            popUpTo(Screen.Result.route) { inclusive = true }
                        }
                    },
                    onDone = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
