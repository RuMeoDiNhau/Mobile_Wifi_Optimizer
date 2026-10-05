package com.mobilelegend.wlo.presentation.navigation

sealed class Screen(val route: String) {
    object LiveRadar : Screen("live_radar")
    object MeasureBenchmark : Screen("measure_benchmark/{categoryName}") {
        fun createRoute(categoryName: String) = "measure_benchmark/$categoryName"
    }
    object Result : Screen("result")
}
