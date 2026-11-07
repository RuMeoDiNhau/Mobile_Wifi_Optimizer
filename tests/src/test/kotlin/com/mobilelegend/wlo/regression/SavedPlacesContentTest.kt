package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.data.sensor.UserGeoCoordinate
import com.mobilelegend.wlo.data.sensor.assessLocation
import com.mobilelegend.wlo.domain.model.LocationConfidence
import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.usecase.CalculateScoreUseCase
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import com.mobilelegend.wlo.presentation.components.MISSING_METRIC
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureViewModel
import com.mobilelegend.wlo.presentation.measure_and_result.SavedSpotItem
import com.mobilelegend.wlo.presentation.saved.savedPlacesContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedPlacesContentTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeWifiRepository()
    private lateinit var model: MeasureViewModel
    private val location = MeasurementLocation(10.77, 106.7, 123L, 15f, LocationConfidence.RECENT)
    private val spots get() = model.uiState.value.savedSpots
    private fun content(user: UserGeoCoordinate = UserGeoCoordinate()) = savedPlacesContent(spots, user)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        model = MeasureViewModel(PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), dispatcher) { 1234L })
    }

    @After fun tearDown() {
        model.cancelBenchmark()
        Dispatchers.resetMain()
    }

    private fun TestScope.measure(at: MeasurementLocation? = location): SavedSpotItem {
        assertTrue(model.startBenchmark(UseCaseCategory.GAMING, at))
        advanceUntilIdle()
        return spots.last()
    }

    @Test fun empty_history_has_no_rows_or_invented_statistics() {
        assertTrue(content().places.isEmpty())
        assertEquals(0, content().totalMeasurements)
        assertEquals(0, content().savedWithoutLocation)
    }

    @Test fun unbookmark_all_then_save_one_shows_exactly_one_and_preserves_history() = runTest(dispatcher) {
        val first = measure()
        val second = measure()
        spots.forEach { model.toggleSpotSaved(it.id) }
        assertTrue(content().places.isEmpty())
        assertEquals(2, content().totalMeasurements)
        assertEquals(0, content().savedWithoutLocation)
        model.saveSpotToHistory("Bàn học", first.score)
        val place = content().places.single()
        assertEquals(first.id, place.measurement!!.measurement!!.id)
        assertEquals("Bàn học", place.name)
        assertSame(first.score, place.measurement)
        assertEquals(second.copy(isSaved = false), spots.last())
        assertEquals(2, content().totalMeasurements)
        model.toggleSpotSaved(first.id)
        assertTrue(content().places.isEmpty())
    }

    @Test fun deleting_every_record_clears_rows_counts_and_stale_save_cannot_restore_them() = runTest(dispatcher) {
        val first = measure()
        val second = measure()
        listOf(first, second).forEach { model.removeSpotFromHistory(it.id) }
        repeat(3) {
            model.saveSpotToHistory("Deleted", first.score)
            model.toggleSpotSaved(second.id)
            assertTrue(content().places.isEmpty())
            assertEquals(0, content().totalMeasurements)
            assertEquals(0, content().savedWithoutLocation)
        }
    }

    @Test fun new_measurement_after_deleting_all_creates_only_its_own_row() = runTest(dispatcher) {
        val old = measure()
        model.removeSpotFromHistory(old.id)
        val fresh = measure()
        assertNotEquals(old.id, fresh.id)
        assertEquals(fresh.id, content().places.single().measurement!!.measurement!!.id)
        assertEquals(1, content().totalMeasurements)
    }

    @Test fun saved_measurement_without_gps_remains_in_history_with_explicit_missing_count() = runTest(dispatcher) {
        val missing = measure(null)
        assertTrue(content().places.isEmpty())
        assertEquals(1, content().totalMeasurements)
        assertEquals(1, content().savedWithoutLocation)
        assertSame(missing.score, spots.single().score)
        measure()
        assertEquals(1, content().places.size)
        assertEquals(2, content().totalMeasurements)
        assertEquals(1, content().savedWithoutLocation)
    }

    @Test fun unbookmarking_or_deleting_missing_gps_updates_only_the_relevant_counts() = runTest(dispatcher) {
        val missing = measure(null)
        model.toggleSpotSaved(missing.id)
        assertEquals(0, content().savedWithoutLocation)
        assertEquals(1, content().totalMeasurements)
        model.saveSpotToHistory("Thiếu GPS", missing.score)
        assertEquals(1, content().savedWithoutLocation)
        model.removeSpotFromHistory(missing.id)
        assertEquals(0, content().totalMeasurements)
        assertEquals(0, content().savedWithoutLocation)
    }

    @Test fun same_ssid_time_and_location_still_keep_two_distinct_measurements() = runTest(dispatcher) {
        val first = measure()
        val second = measure()
        val places = content().places
        assertEquals(2, places.size)
        assertEquals(listOf(second.id, first.id), places.map { it.measurement!!.measurement!!.id })
        assertEquals(listOf(1, 2), places.map { it.rank })
        model.toggleSpotSaved(second.id)
        assertEquals(first.id, content().places.single().measurement!!.measurement!!.id)
        assertEquals(1, content().places.single().rank)
    }

    @Test fun user_movement_updates_distance_without_replacing_saved_snapshot() = runTest(dispatcher) {
        val original = measure()
        val near = content(assessLocation(10.77, 106.7, 5000L, 0, 10f, true, false)).places.single()
        val far = content(assessLocation(21.03, 105.85, 9000L, 0, 10f, true, false)).places.single()
        assertEquals("0 m", near.distanceText)
        assertNotEquals(near.distanceText, far.distanceText)
        assertEquals(near.copy(distanceText = far.distanceText), far)
        assertSame(original.score, far.measurement)
        assertEquals(original, spots.single())
        assertEquals(MISSING_METRIC, content().places.single().distanceText)
    }

    @Test fun repeated_save_renames_without_duplicating_or_changing_order() = runTest(dispatcher) {
        val first = measure()
        val second = measure()
        repeat(5) { model.saveSpotToHistory("  Góc học  ", first.score) }
        assertEquals(2, content().places.size)
        assertEquals("Góc học", content().places.last().name)
        assertEquals(second.id, content().places.first().measurement!!.measurement!!.id)
        assertSame(first.score, content().places.last().measurement)
    }

    @Test fun stale_mock_and_invalid_coordinates_are_counted_without_inventing_map_points() {
        val base = scoreA()
        val unusable = listOf(location.copy(confidence = LocationConfidence.STALE),
            location.copy(confidence = LocationConfidence.MOCK), location.copy(latitude = Double.NaN))
        val history = unusable.mapIndexed { index, fix ->
            val score = base.copy(measurement = base.measurement!!.copy(id = "bad-$index", location = fix))
            SavedSpotItem("bad-$index", "Invalid", score)
        }
        val projected = savedPlacesContent(history, UserGeoCoordinate())
        assertTrue(projected.places.isEmpty())
        assertEquals(3, projected.totalMeasurements)
        assertEquals(3, projected.savedWithoutLocation)
    }

    @Test fun toggling_unknown_or_demo_id_cannot_add_a_saved_place() = runTest(dispatcher) {
        val first = measure()
        val before = content()
        model.toggleSpotSaved("workshop_coffee")
        model.removeSpotFromHistory("missing")
        assertEquals(before, content())
        model.toggleSpotSaved(first.id)
        model.toggleSpotSaved("workshop_coffee")
        assertTrue(content().places.isEmpty())
    }

    @Test fun saving_old_display_data_does_not_replace_identity_location_or_metrics() = runTest(dispatcher) {
        val original = measure()
        model.toggleSpotSaved(original.id)
        model.saveSpotToHistory("Restored bookmark", original.score.copy(
            rawPingMs = 9999, measurement = original.score.measurement!!.copy(location = null, ssid = "Wrong network")))
        val saved = content().places.single()
        assertSame(original.score, saved.measurement)
        assertEquals(location.latitude, saved.latitude, 0.0)
        assertEquals(original.score.rawPingMs, saved.pingMs)
        assertEquals("Network A", saved.ssid)
        assertEquals(1, content().totalMeasurements)
    }
}
