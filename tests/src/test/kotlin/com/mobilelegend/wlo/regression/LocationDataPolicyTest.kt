package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.data.sensor.*
import com.mobilelegend.wlo.domain.model.*
import com.mobilelegend.wlo.domain.usecase.CalculateScoreUseCase
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import com.mobilelegend.wlo.presentation.components.*
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationDataPolicyTest {
    private fun fix(lat: Double = 10.77, lon: Double = 106.7, age: Long = 1000, accuracy: Float? = 10f,
                    fine: Boolean = true, mock: Boolean = false) =
        assessLocation(lat, lon, 1234L, age, accuracy, fine, mock)

    @Test fun default_camera_center_is_not_a_user_marker_or_measurement_location() {
        val missing = UserGeoCoordinate()
        assertFalse(missing.isRealFix)
        assertNull(missing.toMeasurementLocation())
        assertTrue(missing.statusText.contains("tâm mặc định"))
    }

    @Test fun new_fix_keeps_exact_coordinates_capture_time_accuracy_and_confidence() {
        val location = requireNotNull(fix().toMeasurementLocation())
        assertEquals(10.77, location.latitude, 0.0)
        assertEquals(106.7, location.longitude, 0.0)
        assertEquals(1234L, location.capturedAtMillis)
        assertEquals(10f, location.accuracyMeters)
        assertEquals(LocationConfidence.RECENT, location.confidence)
    }

    @Test fun coarse_permission_or_large_uncertainty_is_labeled_approximate() {
        for (location in listOf(fix(fine = false), fix(accuracy = 1500f))) {
            assertTrue(location.isRealFix)
            assertEquals(LocationConfidence.APPROXIMATE, location.toMeasurementLocation()!!.confidence)
            assertTrue(location.statusText.contains("gần đúng"))
        }
    }

    @Test fun stale_or_negative_age_does_not_create_current_marker_or_measurement() {
        for (age in listOf(MAX_LOCATION_AGE_MS + 1, -1L)) {
            val location = fix(age = age)
            assertEquals(LocationConfidence.STALE, location.confidence)
            assertFalse(location.isRealFix)
            assertNull(location.toMeasurementLocation())
        }
        assertTrue(fix(age = MAX_LOCATION_AGE_MS).isRealFix)
    }

    @Test fun mocked_fix_is_explicit_and_not_used_for_real_measurements() {
        val location = fix(mock = true)
        assertEquals(LocationConfidence.MOCK, location.confidence)
        assertNull(location.toMeasurementLocation())
    }

    @Test fun genuine_mountain_view_location_is_not_replaced_with_vietnam_coordinates() {
        val location = fix(37.42, -122.08)
        assertTrue(location.isRealFix)
        assertEquals(37.42, location.latitude, 0.0)
        assertEquals(-122.08, location.longitude, 0.0)
    }

    @Test fun invalid_coordinates_or_accuracy_never_become_real_fixes() {
        val cases = listOf(
            fix(lat = Double.NaN), fix(lon = Double.POSITIVE_INFINITY), fix(lat = 91.0),
            fix(lon = -181.0), fix(accuracy = null), fix(accuracy = -1f),
            fix(accuracy = Float.NaN), fix(accuracy = Float.POSITIVE_INFINITY)
        )
        assertTrue(cases.all { !it.isRealFix && it.toMeasurementLocation() == null })
    }

    @Test fun moving_user_far_away_never_relocates_named_demo_places() {
        val before = DefaultWifiPlaces
        var after = placesWithDistances(before, fix(21.03, 105.85))
        after = placesWithDistances(after, fix(37.42, -122.08))
        assertEquals(before.map { Triple(it.id, it.latitude, it.longitude) }, after.map { Triple(it.id, it.latitude, it.longitude) })
        assertTrue(after.all { it.distanceText == MISSING_METRIC })
    }

    @Test fun demo_is_opt_in_on_map_and_excluded_from_measured_recommendations() {
        val actual = requireNotNull(scoreA().toWifiPlace())
        val places = DefaultWifiPlaces + actual
        assertEquals(listOf(actual), visibleMapPlaces(places, showDemo = false))
        assertEquals(places, visibleMapPlaces(places, showDemo = true))
        assertEquals(listOf(actual), measuredPlacesOnly(places))
        assertTrue(measuredPlacesOnly(DefaultWifiPlaces).isEmpty())
    }

    @Test fun user_movement_changes_distance_only_and_unknown_location_has_no_fake_distance() {
        val original = requireNotNull(scoreA().toWifiPlace())
        val same = placesWithDistances(listOf(original), fix(original.latitude, original.longitude)).single()
        assertEquals("0 m", same.distanceText)
        val far = placesWithDistances(listOf(original), fix(21.03, 105.85)).single()
        assertNotEquals(same.distanceText, far.distanceText)
        assertEquals(original.copy(distanceText = far.distanceText), far)
        assertSame(original.measurement, far.measurement)
        assertEquals(MISSING_METRIC, placesWithDistances(listOf(original), UserGeoCoordinate()).single().distanceText)
    }

    @Test fun invalid_or_stale_measurement_coordinates_do_not_create_map_points() {
        val score = scoreA()
        for (location in listOf(
            MeasurementLocation(Double.NaN, 10.0),
            MeasurementLocation(1.0, 2.0, confidence = LocationConfidence.STALE),
            MeasurementLocation(1.0, 2.0, confidence = LocationConfidence.MOCK)
        )) {
            assertNull(score.copy(measurement = score.measurement!!.copy(location = location)).toWifiPlace())
        }
    }

    @Test fun two_measurements_keep_distinct_ids_locations_and_confidence_after_moving() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeWifiRepository()
            val model = MeasureViewModel(PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), dispatcher))
            val a = fix(10.77, 106.7).toMeasurementLocation()
            val b = fix(21.03, 105.85, accuracy = 500f).toMeasurementLocation()
            model.startBenchmark(UseCaseCategory.GAMING, a)
            advanceUntilIdle()
            repository.metric = repository.metric.copy(ssid = "B", bssid = "AA:BB:CC:DD:EE:02")
            model.startBenchmark(UseCaseCategory.GAMING, b)
            advanceUntilIdle()
            val saved = model.uiState.value.savedSpots
            val places = placesWithDistances(saved.mapNotNull { it.score.toWifiPlace() }, fix(37.42, -122.08))
            assertEquals(2, places.map { it.id }.distinct().size)
            assertEquals(a, saved[0].score.measurement!!.location)
            assertEquals(b, saved[1].score.measurement!!.location)
            assertEquals(10.77, places[0].latitude, 0.0)
            assertEquals(105.85, places[1].longitude, 0.0)
            assertTrue(places[1].district.contains("gần đúng"))
        } finally { Dispatchers.resetMain() }
    }
}
