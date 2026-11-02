package com.mobilelegend.wlo.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.presentation.components.DefaultWifiPlaces
import com.mobilelegend.wlo.presentation.components.WifiNeedOption
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.components.WloBottomNavigationBar
import com.mobilelegend.wlo.presentation.home.HomeScreen
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarScreen
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarViewModel
import com.mobilelegend.wlo.presentation.map.WifiMapScreen
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureScreen
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureStatus
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureViewModel
import com.mobilelegend.wlo.presentation.measure_and_result.NetworkMeasureScreen
import com.mobilelegend.wlo.presentation.measure_and_result.ResultScreen
import com.mobilelegend.wlo.presentation.profile.ProfileScreen
import com.mobilelegend.wlo.presentation.saved.SavedPlacesScreen
import com.mobilelegend.wlo.presentation.theme.AppBackground

@Composable
fun AppNavigation(
    navController: NavHostController,
    liveRadarViewModel: LiveRadarViewModel,
    measureViewModel: MeasureViewModel,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    var places by remember { mutableStateOf(DefaultWifiPlaces) }
    var selectedNeed by remember { mutableStateOf(WifiNeedOption.WORK) }
    var areaScore by remember { mutableIntStateOf(92) }
    var inspectedPlace by remember { mutableStateOf<WifiPlaceItem?>(null) }

    val measureState by measureViewModel.uiState.collectAsStateWithLifecycle()

    val navigateToTab: (Screen) -> Unit = { targetScreen ->
        navController.navigate(targetScreen.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = AppBackground,
        bottomBar = {
            WloBottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { target ->
                    if (target != Screen.NetworkMeasure) {
                        inspectedPlace = null
                    }
                    navigateToTab(target)
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Tab 1: Trang chủ (anh1.png)
            composable(Screen.Home.route) {
                HomeScreen(
                    places = places,
                    selectedNeed = selectedNeed,
                    areaScore = areaScore,
                    onSelectNeed = { option ->
                        selectedNeed = option
                        liveRadarViewModel.selectCategory(option.category)
                    },
                    onToggleSavePlace = { placeId ->
                        places = places.map {
                            if (it.id == placeId) it.copy(isSaved = !it.isSaved) else it
                        }
                    },
                    onOpenMap = {
                        navigateToTab(Screen.WifiMap)
                    },
                    onStartMeasureHere = {
                        inspectedPlace = null
                        liveRadarViewModel.selectCategory(selectedNeed.category)
                        measureViewModel.startBenchmark(selectedNeed.category)
                        navigateToTab(Screen.NetworkMeasure)
                    },
                    onPlaceClick = { place ->
                        inspectedPlace = place
                        navigateToTab(Screen.NetworkMeasure)
                    }
                )
            }

            // Tab 2: Bản đồ Wi-Fi (anh2.png)
            composable(Screen.WifiMap.route) {
                WifiMapScreen(
                    places = places,
                    onInspectPlace = { place ->
                        inspectedPlace = place
                        navigateToTab(Screen.NetworkMeasure)
                    }
                )
            }

            // Tab 3: Đo chất lượng mạng (anh3.png)
            composable(Screen.NetworkMeasure.route) {
                NetworkMeasureScreen(
                    liveRadarViewModel = liveRadarViewModel,
                    measureViewModel = measureViewModel,
                    inspectedPlace = inspectedPlace,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onOpenAppSettings = onOpenAppSettings,
                    onScoreMeasured = { newScore ->
                        areaScore = newScore
                    }
                )
            }

            // Tab 4: Địa điểm đã lưu (anh4.png)
            composable(Screen.SavedPlaces.route) {
                SavedPlacesScreen(
                    places = places,
                    sessionSpots = measureState.savedSpots,
                    onToggleSavePlace = { placeId ->
                        places = places.map {
                            if (it.id == placeId) it.copy(isSaved = !it.isSaved) else it
                        }
                    },
                    onRemoveSessionSpot = { spotId ->
                        measureViewModel.removeSpotFromHistory(spotId)
                    },
                    onOpenMap = {
                        navigateToTab(Screen.WifiMap)
                    },
                    onPlaceClick = { place ->
                        inspectedPlace = place
                        navigateToTab(Screen.NetworkMeasure)
                    }
                )
            }

            // Tab 5: Cá nhân (anh5.png)
            composable(Screen.Profile.route) {
                ProfileScreen(
                    selectedNeed = selectedNeed,
                    totalMeasurements = 12 + measureState.savedSpots.size,
                    onCycleNeed = {
                        val allNeeds = WifiNeedOption.values()
                        val nextIndex = (selectedNeed.ordinal + 1) % allNeeds.size
                        val nextNeed = allNeeds[nextIndex]
                        selectedNeed = nextNeed
                        liveRadarViewModel.selectCategory(nextNeed.category)
                    },
                    onOpenAppSettings = onOpenAppSettings
                )
            }

            // Các route phụ trợ tương thích ngược
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
                        navController.navigate(Screen.Result.route)
                    },
                    onBackToRadar = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Result.route) {
                val completedScore = (measureState.status as? MeasureStatus.Completed)?.score
                if (completedScore != null) {
                    ResultScreen(
                        score = completedScore,
                        viewModel = measureViewModel,
                        onBackToRadar = {
                            navigateToTab(Screen.Home)
                        }
                    )
                }
            }
        }
    }
}
