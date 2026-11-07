package com.mobilelegend.wlo.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.mobilelegend.wlo.core.permissions.PermissionHelper
import com.mobilelegend.wlo.data.sensor.DeviceLocationProvider
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.presentation.components.placesWithDistances
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.mobilelegend.wlo.presentation.components.DefaultWifiPlaces
import com.mobilelegend.wlo.presentation.components.toWifiPlace
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
    onOpenWifiSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationProvider = remember(context) {
        DeviceLocationProvider(context, PermissionHelper(context))
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    var userLocation by remember { mutableStateOf(locationProvider.getCurrentLocation()) }
    var demoPlaces by remember { mutableStateOf(DefaultWifiPlaces) }
    var selectedNeed by remember { mutableStateOf(WifiNeedOption.WORK) }
    var inspectedPlace by remember { mutableStateOf<WifiPlaceItem?>(null) }

    val radarState by liveRadarViewModel.uiState.collectAsStateWithLifecycle()
    val measureState by measureViewModel.uiState.collectAsStateWithLifecycle()
    val readiness = radarState.readiness(measureState.isBenchmarkRunning)

    // Pure projection of the ViewModel store: composition never inserts measurements.
    val places = placesWithDistances(measureState.savedSpots.asReversed().mapNotNull { spot ->
        spot.score.toWifiPlace()?.copy(name = spot.spotName, isSaved = spot.isSaved)
    } + demoPlaces, userLocation).mapIndexed { index, place -> place.copy(rank = index + 1) }
    val areaScore = measureState.savedSpots.lastOrNull()?.score?.finalScore
    val toggleSavePlace: (String) -> Unit = { placeId ->
        val measurementId = places.find { it.id == placeId }?.measurement?.measurement?.id
        if (measurementId != null) {
            measureViewModel.toggleSpotSaved(measurementId)
        } else {
            demoPlaces = demoPlaces.map { if (it.id == placeId) it.copy(isSaved = !it.isSaved) else it }
        }
    }

    // Runs for system Back as well as tab buttons. The graph, not a fading-out screen,
    // owns cancellation, so disposing an old screen cannot cancel a newer measurement.
    DisposableEffect(navController, measureViewModel) {
        var previousRoute = navController.currentDestination?.route
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val leavingMeasurement = previousRoute == Screen.NetworkMeasure.route ||
                previousRoute == Screen.MeasureBenchmark.route
            if (leavingMeasurement && destination.route != previousRoute) {
                measureViewModel.cancelBenchmark()
            }
            previousRoute = destination.route
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
            measureViewModel.cancelBenchmark()
        }
    }

    val startCurrentNetworkMeasurement: () -> Unit = {
        inspectedPlace = null
        liveRadarViewModel.selectCategory(selectedNeed.category)
        val locationAtStart = locationProvider.getCurrentLocation().toMeasurementLocation()
        measureViewModel.startBenchmark(
            selectedNeed.category,
            locationAtStart
        )
    }

    val refreshUserLocation: () -> Unit = {
        locationProvider.requestSingleUpdate { latestCoord ->
            userLocation = latestCoord
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(locationProvider, lifecycleOwner, radarState.wifiAccess) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                refreshUserLocation()
                while (isActive) {
                    userLocation = locationProvider.getCurrentLocation()
                    delay(1000L)
                }
            } finally {
                locationProvider.cancelLocationUpdate()
            }
        }
    }
    DisposableEffect(locationProvider) { onDispose { locationProvider.cancelLocationUpdate() } }

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
                    inspectedPlace = null
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
                    readiness = readiness,
                    onOpenAppSettings = onOpenAppSettings,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onOpenWifiSettings = onOpenWifiSettings,
                    onSelectNeed = { option ->
                        selectedNeed = option
                        liveRadarViewModel.selectCategory(option.category)
                    },
                    onToggleSavePlace = toggleSavePlace,
                    onOpenMap = {
                        navigateToTab(Screen.WifiMap)
                    },
                    onStartMeasureHere = {
                        navigateToTab(Screen.NetworkMeasure)
                        startCurrentNetworkMeasurement()
                    },
                    onPlaceClick = { place ->
                        inspectedPlace = place
                        navigateToTab(Screen.NetworkMeasure)
                    }
                )
            }

            // Tab 2: Bản đồ Wi-Fi thực tế OpenStreetMap + GPS thật (anh2.png)
            composable(Screen.WifiMap.route) {
                WifiMapScreen(
                    places = places,
                    userLocation = userLocation,
                    onRefreshUserLocation = refreshUserLocation,
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
                    onMeasureCurrentNetwork = startCurrentNetworkMeasurement,
                    onOpenWifiSettings = onOpenWifiSettings
                )
            }

            // Tab 4: Địa điểm đã lưu (anh4.png)
            composable(Screen.SavedPlaces.route) {
                SavedPlacesScreen(
                    places = places,
                    sessionSpots = measureState.savedSpots,
                    onToggleSavePlace = toggleSavePlace,
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
                    totalMeasurements = measureState.savedSpots.size,
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
                        measureViewModel.startBenchmark(category, locationProvider.getCurrentLocation().toMeasurementLocation())
                    },
                    onOpenLocationSettings = onOpenLocationSettings,
                    onOpenAppSettings = onOpenAppSettings,
                    onOpenWifiSettings = onOpenWifiSettings,
                    readiness = readiness
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
                    onStartMeasurement = {
                        measureViewModel.startBenchmark(category, locationProvider.getCurrentLocation().toMeasurementLocation())
                    },
                    readiness = readiness,
                    onOpenAppSettings = onOpenAppSettings,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onOpenWifiSettings = onOpenWifiSettings,
                    onBenchmarkCompleted = {
                        if (navController.currentDestination?.route == Screen.MeasureBenchmark.route) {
                            navController.navigate(Screen.Result.route) {
                                popUpTo(Screen.MeasureBenchmark.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
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
