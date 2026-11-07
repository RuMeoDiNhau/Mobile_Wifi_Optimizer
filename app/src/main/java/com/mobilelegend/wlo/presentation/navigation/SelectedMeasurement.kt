package com.mobilelegend.wlo.presentation.navigation

import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarViewModel
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureViewModel

/** Read the selection at click time, even before Compose observes the latest state. */
fun startSelectedMeasurement(
    radar: LiveRadarViewModel,
    measurements: MeasureViewModel,
    location: MeasurementLocation?
): Boolean = measurements.startBenchmark(radar.uiState.value.selectedCategory, location)
