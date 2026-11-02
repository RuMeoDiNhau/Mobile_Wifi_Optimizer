package com.mobilelegend.wlo.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object WifiMap : Screen("wifi_map")
    object NetworkMeasure : Screen("network_measure")
    object SavedPlaces : Screen("saved_places")
    object Profile : Screen("profile")

    // Các route phụ trợ tương thích ngược
    object LiveRadar : Screen("live_radar")
    object MeasureBenchmark : Screen("measure_benchmark/{categoryName}") {
        fun createRoute(categoryName: String) = "measure_benchmark/$categoryName"
    }
    object Result : Screen("result")
}
