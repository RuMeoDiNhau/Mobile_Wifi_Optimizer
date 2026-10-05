package com.mobilelegend.wlo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarScreen
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarViewModel
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureScreen
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureStatus
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureViewModel
import com.mobilelegend.wlo.presentation.measure_and_result.ResultScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    liveRadarViewModel: LiveRadarViewModel,
    measureViewModel: MeasureViewModel,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.LiveRadar.route,
        modifier = modifier
    ) {
        composable(Screen.LiveRadar.route) {
            LiveRadarScreen(
                viewModel = liveRadarViewModel,
                onNavigateToBenchmark = { category ->
                    navController.navigate(Screen.MeasureBenchmark.createRoute(category.name))
                },
                onOpenLocationSettings = onOpenLocationSettings,
                onOpenAppSettings = onOpenAppSettings
            )
        }

        composable(
            route = Screen.MeasureBenchmark.route,
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: UseCaseCategory.GAMING.name
            val category = try {
                UseCaseCategory.valueOf(categoryName)
            } catch (e: Exception) {
                UseCaseCategory.GAMING
            }

            MeasureScreen(
                category = category,
                viewModel = measureViewModel,
                onBenchmarkCompleted = {
                    navController.navigate(Screen.Result.route) {
                        popUpTo(Screen.LiveRadar.route)
                    }
                },
                onBackToRadar = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Result.route) {
            val measureState by measureViewModel.uiState.collectAsStateWithLifecycle()
            val completedScore = (measureState.status as? MeasureStatus.Completed)?.score

            if (completedScore != null) {
                ResultScreen(
                    score = completedScore,
                    viewModel = measureViewModel,
                    onBackToRadar = {
                        navController.navigate(Screen.LiveRadar.route) {
                            popUpTo(Screen.LiveRadar.route) { inclusive = true }
                        }
                    }
                )
            } else {
                // Fallback nếu không có score thì về LiveRadar
                navController.navigate(Screen.LiveRadar.route) {
                    popUpTo(Screen.LiveRadar.route) { inclusive = true }
                }
            }
        }
    }
}
