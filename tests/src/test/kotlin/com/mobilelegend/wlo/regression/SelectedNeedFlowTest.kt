package com.mobilelegend.wlo.regression

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.usecase.CalculateScoreUseCase
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import com.mobilelegend.wlo.domain.usecase.StreamLiveWifiUseCase
import com.mobilelegend.wlo.presentation.components.WifiNeedOption
import com.mobilelegend.wlo.presentation.home.latestMeasurementForNeed
import com.mobilelegend.wlo.presentation.live_radar.LiveRadarViewModel
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureViewModel
import com.mobilelegend.wlo.presentation.measure_and_result.historyComparison
import com.mobilelegend.wlo.presentation.navigation.startSelectedMeasurement
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SelectedNeedFlowTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeWifiRepository()
    private val store = ViewModelStore()
    private val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
            LiveRadarViewModel::class.java -> LiveRadarViewModel(StreamLiveWifiUseCase(repository), repository) as T
            MeasureViewModel::class.java -> MeasureViewModel(PerformBenchmarkUseCase(repository,
                CalculateScoreUseCase(), dispatcher) { 1234L }) as T
            else -> error("Unknown model")
        }
    }
    private lateinit var radar: LiveRadarViewModel
    private lateinit var measurements: MeasureViewModel
    private val location = MeasurementLocation(10.77, 106.7)
    private fun start() = startSelectedMeasurement(radar, measurements, location)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        val provider = ViewModelProvider(store, factory)
        radar = provider[LiveRadarViewModel::class.java]
        measurements = provider[MeasureViewModel::class.java]
    }

    @After fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test fun default_direct_measurement_matches_home_work_selection() = runTest(dispatcher) {
        assertEquals(WifiNeedOption.WORK, radar.uiState.value.selectedNeed)
        assertTrue(start())
        advanceUntilIdle()
        val result = measurements.uiState.value.savedSpots.single().score
        assertEquals(UseCaseCategory.STUDY_DOWNLOAD, result.category)
        assertEquals(location, result.measurement!!.location)
    }

    @Test fun selecting_and_starting_before_recomposition_uses_latest_choice() = runTest(dispatcher) {
        val oldUi = radar.uiState.value
        radar.selectNeed(WifiNeedOption.GAMING)
        assertEquals(WifiNeedOption.WORK, oldUi.selectedNeed)
        assertTrue(start())
        advanceUntilIdle()
        assertEquals(UseCaseCategory.GAMING, measurements.uiState.value.savedSpots.single().score.category)
    }

    @Test fun every_home_option_drives_the_shared_measurement_action() = runTest(dispatcher) {
        for (need in WifiNeedOption.values()) {
            radar.selectNeed(need)
            assertTrue(start())
            advanceUntilIdle()
            assertEquals(need.category, measurements.uiState.value.savedSpots.last().score.category)
            assertEquals(need, radar.uiState.value.selectedNeed)
        }
        assertEquals(4, repository.openedSessions)
    }

    @Test fun profile_cycle_uses_store_value_even_with_repeated_clicks_before_rendering() = runTest(dispatcher) {
        val seen = (1..4).map { radar.cycleNeed(); radar.uiState.value.selectedNeed }
        assertEquals(listOf(WifiNeedOption.VIDEO_MEETING, WifiNeedOption.GAMING, WifiNeedOption.RELAX, WifiNeedOption.WORK), seen)
        radar.cycleNeed()
        assertTrue(start())
        advanceUntilIdle()
        assertEquals(UseCaseCategory.VIDEO_CALL, measurements.uiState.value.savedSpots.single().score.category)
    }

    @Test fun radar_category_selection_updates_home_option_and_shared_start() = runTest(dispatcher) {
        radar.selectCategory(UseCaseCategory.VIDEO_CALL)
        assertEquals(WifiNeedOption.VIDEO_MEETING, radar.uiState.value.selectedNeed)
        assertTrue(start())
        advanceUntilIdle()
        assertEquals(UseCaseCategory.VIDEO_CALL, measurements.uiState.value.savedSpots.single().score.category)
    }

    @Test fun radar_reselecting_download_keeps_exact_relax_choice() {
        radar.selectNeed(WifiNeedOption.RELAX)
        radar.selectCategory(UseCaseCategory.STUDY_DOWNLOAD)
        assertEquals(WifiNeedOption.RELAX, radar.uiState.value.selectedNeed)
        radar.selectCategory(UseCaseCategory.GAMING)
        radar.selectCategory(UseCaseCategory.STUDY_DOWNLOAD)
        assertEquals(WifiNeedOption.WORK, radar.uiState.value.selectedNeed)
    }

    @Test fun selection_survives_owner_recreation_and_background_refresh() = runTest(dispatcher) {
        radar.selectNeed(WifiNeedOption.RELAX)
        radar.setForeground(false)
        radar.refreshHardwareStatus()
        runCurrent()
        val recreated = ViewModelProvider(store, factory)[LiveRadarViewModel::class.java]
        assertSame(radar, recreated)
        assertEquals(WifiNeedOption.RELAX, recreated.uiState.value.selectedNeed)
        assertEquals(UseCaseCategory.STUDY_DOWNLOAD, recreated.uiState.value.selectedCategory)
        assertEquals(0, repository.openedSessions)
    }

    @Test fun changing_selection_during_measurement_preserves_captured_and_completed_category() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        repository.onProbe = { release.await() }
        radar.selectNeed(WifiNeedOption.GAMING)
        assertTrue(start())
        runCurrent()
        radar.selectNeed(WifiNeedOption.VIDEO_MEETING)
        assertFalse(start())
        assertEquals(UseCaseCategory.GAMING, measurements.uiState.value.selectedCategory)
        release.complete(Unit)
        advanceUntilIdle()
        val original = measurements.uiState.value.savedSpots.single().score
        assertEquals(UseCaseCategory.GAMING, original.category)
        assertNull(latestMeasurementForNeed(measurements.uiState.value.savedSpots, WifiNeedOption.VIDEO_MEETING))
        assertTrue(start())
        advanceUntilIdle()
        assertSame(original, measurements.uiState.value.savedSpots.first().score)
        assertEquals(UseCaseCategory.VIDEO_CALL, measurements.uiState.value.savedSpots.last().score.category)
    }

    @Test fun blocked_start_keeps_selection_and_retry_uses_new_choice() = runTest(dispatcher) {
        repository.ready = false
        radar.selectNeed(WifiNeedOption.GAMING)
        assertFalse(start())
        assertEquals(WifiNeedOption.GAMING, radar.uiState.value.selectedNeed)
        assertEquals(0, repository.openedSessions)
        radar.selectNeed(WifiNeedOption.VIDEO_MEETING)
        repository.ready = true
        assertTrue(start())
        advanceUntilIdle()
        assertEquals(UseCaseCategory.VIDEO_CALL, measurements.uiState.value.savedSpots.single().score.category)
    }

    @Test fun inspecting_or_saving_an_old_result_does_not_change_the_next_need() = runTest(dispatcher) {
        assertTrue(start())
        advanceUntilIdle()
        val old = measurements.uiState.value.savedSpots.single()
        radar.selectNeed(WifiNeedOption.GAMING)
        measurements.saveSpotToHistory("Old result", old.score)
        assertEquals(WifiNeedOption.GAMING, radar.uiState.value.selectedNeed)
        assertSame(old.score, measurements.uiState.value.savedSpots.single().score)
        assertTrue(start())
        advanceUntilIdle()
        assertEquals(UseCaseCategory.GAMING, measurements.uiState.value.savedSpots.last().score.category)
    }

    @Test fun history_comparison_updates_after_actual_bookmark_save_and_delete_actions() = runTest(dispatcher) {
        repeat(2) { assertTrue(start()); advanceUntilIdle() }
        val original = measurements.uiState.value.savedSpots
        assertTrue(historyComparison(original).single().rows.all { it.bestLabel != null })
        measurements.toggleSpotSaved(original.first().id)
        var group = historyComparison(measurements.uiState.value.savedSpots).single()
        assertEquals(1, group.savedCount)
        assertTrue(group.rows.all { it.bestLabel == null })
        measurements.saveSpotToHistory("Saved again", original.first().score)
        group = historyComparison(measurements.uiState.value.savedSpots).single()
        assertEquals(2, group.savedCount)
        assertTrue(group.rows.all { it.bestLabel!!.startsWith("Đồng điểm cao nhất") })
        original.forEach { measurements.removeSpotFromHistory(it.id) }
        assertTrue(historyComparison(measurements.uiState.value.savedSpots).isEmpty())
    }
}
