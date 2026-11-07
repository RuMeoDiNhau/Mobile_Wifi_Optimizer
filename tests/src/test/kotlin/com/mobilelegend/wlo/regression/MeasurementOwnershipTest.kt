package com.mobilelegend.wlo.regression

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.usecase.CalculateScoreUseCase
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import com.mobilelegend.wlo.presentation.components.toWifiPlace
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureStatus
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureViewModel
import com.mobilelegend.wlo.presentation.measure_and_result.networkMeasureContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementOwnershipTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeWifiRepository()
    private val benchmark = PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), dispatcher) { 1234L }
    private lateinit var viewModel: MeasureViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = MeasureViewModel(benchmark)
    }

    @After fun tearDown() {
        viewModel.cancelBenchmark()
        Dispatchers.resetMain()
    }

    @Test fun repeated_viewing_keeps_original_identity_location_time_and_count() = runTest(dispatcher) {
        viewModel.startBenchmark(UseCaseCategory.GAMING, MeasurementLocation(10.5, 106.5))
        advanceUntilIdle()
        val original = viewModel.uiState.value.savedSpots.single()
        repository.metric = repository.metric.copy(ssid = "B", bssid = "AA:BB:CC:DD:EE:02")
        repeat(10) {
            val content = networkMeasureContent(viewModel.uiState.value.status, onlineRadar(), null)
            val place = requireNotNull(original.score.toWifiPlace())
            assertEquals("Network A", content.ssid)
            assertEquals(1234L, content.measuredAtMillis)
            assertEquals(10.5, place.latitude, 0.0)
            assertEquals(106.5, place.longitude, 0.0)
            assertEquals(listOf(original), viewModel.uiState.value.savedSpots)
        }
    }

    @Test fun repeated_save_only_changes_name_and_never_replaces_measurement() = runTest(dispatcher) {
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        val original = viewModel.uiState.value.savedSpots.single()
        repeat(5) {
            viewModel.saveSpotToHistory("  Bàn học  ", original.score.copy(rawPingMs = 9999))
        }
        val saved = viewModel.uiState.value.savedSpots.single()
        assertEquals(original.copy(spotName = "Bàn học"), saved)
        assertSame(original.score, saved.score)
    }

    @Test fun removed_record_does_not_return_when_old_result_is_viewed_or_saved() = runTest(dispatcher) {
        viewModel.startBenchmark(UseCaseCategory.GAMING, MeasurementLocation(1.0, 2.0))
        advanceUntilIdle()
        val original = viewModel.uiState.value.savedSpots.single()
        viewModel.removeSpotFromHistory(original.id)
        repeat(3) {
            networkMeasureContent(viewModel.uiState.value.status, onlineRadar(), null)
            viewModel.saveSpotToHistory("Stale result", original.score)
        }
        viewModel.resetMeasureState()
        assertTrue(viewModel.uiState.value.savedSpots.isEmpty())
    }

    @Test fun bookmarks_change_without_mutating_snapshot_or_duplicating_history() = runTest(dispatcher) {
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        val original = viewModel.uiState.value.savedSpots.single()
        viewModel.toggleSpotSaved(original.id)
        assertEquals(original.copy(isSaved = false), viewModel.uiState.value.savedSpots.single())
        viewModel.saveSpotToHistory("", original.score)
        assertEquals(original, viewModel.uiState.value.savedSpots.single())
    }

    @Test fun separate_measurements_at_same_time_have_distinct_history_ids() = runTest(dispatcher) {
        repeat(2) {
            assertTrue(viewModel.startBenchmark(UseCaseCategory.GAMING))
            advanceUntilIdle()
        }
        val spots = viewModel.uiState.value.savedSpots
        assertEquals(2, spots.size)
        assertEquals(2, spots.map { it.id }.toSet().size)
        assertTrue(spots.all { it.timestamp == 1234L })
    }

    @Test fun rapid_starts_share_one_job_and_one_history_record() = runTest(dispatcher) {
        assertTrue(viewModel.startBenchmark(UseCaseCategory.GAMING))
        repeat(10) { assertFalse(viewModel.startBenchmark(UseCaseCategory.VIDEO_CALL)) }
        advanceUntilIdle()
        assertEquals(1, repository.openedSessions)
        assertEquals(1, repository.maxActiveSessions)
        assertEquals(UseCaseCategory.GAMING, viewModel.uiState.value.savedSpots.single().score.category)
    }

    @Test fun cancel_before_dispatch_opens_no_session_and_allows_next_run() = runTest(dispatcher) {
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        viewModel.cancelBenchmark()
        advanceUntilIdle()
        assertEquals(0, repository.openedSessions)
        assertEquals(MeasureStatus.Cancelled, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isBenchmarkRunning)
        assertTrue(viewModel.uiState.value.savedSpots.isEmpty())
        assertTrue(viewModel.startBenchmark(UseCaseCategory.GAMING))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.savedSpots.size)
    }

    @Test fun cancellation_during_session_setup_closes_it_without_probing() = runTest(dispatcher) {
        repository.onOpen = { awaitCancellation() }
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        runCurrent()
        viewModel.cancelBenchmark()
        advanceUntilIdle()
        assertEquals(1, repository.closedSessions)
        assertEquals(0, repository.pingCalls)
        assertTrue(viewModel.uiState.value.savedSpots.isEmpty())
        assertFalse(viewModel.uiState.value.isBenchmarkRunning)
    }

    @Test fun cancellation_during_probe_closes_session_without_saving() = runTest(dispatcher) {
        repository.onProbe = { awaitCancellation() }
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        runCurrent()
        viewModel.cancelBenchmark()
        advanceUntilIdle()
        assertEquals(1, repository.closedSessions)
        assertEquals(0, repository.activeSessions)
        assertEquals(MeasureStatus.Cancelled, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.savedSpots.isEmpty())
        assertNull(networkMeasureContent(viewModel.uiState.value.status, onlineRadar(), null).score)
    }

    @Test fun next_run_waits_until_cancelled_session_finishes_cleanup() = runTest(dispatcher) {
        val cleanup = CompletableDeferred<Unit>()
        repository.onProbe = { awaitCancellation() }
        repository.onClose = { withContext(NonCancellable) { cleanup.await() } }
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        runCurrent()
        viewModel.cancelBenchmark()
        runCurrent()
        assertTrue(viewModel.uiState.value.isBenchmarkRunning)
        assertFalse(viewModel.startBenchmark(UseCaseCategory.VIDEO_CALL))
        assertEquals(1, repository.activeSessions)
        cleanup.complete(Unit)
        advanceUntilIdle()
        repository.onProbe = {}
        repository.onClose = {}
        assertTrue(viewModel.startBenchmark(UseCaseCategory.VIDEO_CALL))
        advanceUntilIdle()
        assertEquals(1, repository.maxActiveSessions)
        assertEquals(2, repository.closedSessions)
        assertEquals(UseCaseCategory.VIDEO_CALL, viewModel.uiState.value.savedSpots.single().score.category)
    }

    @Test fun reset_cancels_job_and_late_success_cannot_replace_idle() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        repository.onProbe = { withContext(NonCancellable) { release.await() } }
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        runCurrent()
        viewModel.resetMeasureState()
        assertFalse(viewModel.startBenchmark(UseCaseCategory.GAMING))
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(MeasureStatus.Idle, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.savedSpots.isEmpty())
        assertFalse(viewModel.uiState.value.isBenchmarkRunning)
        assertEquals(1, repository.closedSessions)
    }

    @Test fun late_failure_after_cancel_cannot_publish_failed_or_history() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        repository.onProbe = { withContext(NonCancellable) {
            release.await()
            throw IllegalStateException("Late failure")
        } }
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        runCurrent()
        viewModel.cancelBenchmark()
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(MeasureStatus.Cancelled, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.savedSpots.isEmpty())
        assertEquals(1, repository.closedSessions)
    }

    @Test fun failure_releases_ownership_and_next_success_is_saved_once() = runTest(dispatcher) {
        repository.error = IllegalStateException("Probe failed")
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.status is MeasureStatus.Failed)
        assertFalse(viewModel.uiState.value.isBenchmarkRunning)
        repository.error = null
        assertTrue(viewModel.startBenchmark(UseCaseCategory.GAMING))
        advanceUntilIdle()
        assertEquals(2, repository.closedSessions)
        assertEquals(1, viewModel.uiState.value.savedSpots.size)
    }

    @Test fun cancel_after_completion_preserves_committed_result() = runTest(dispatcher) {
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        val original = viewModel.uiState.value
        viewModel.cancelBenchmark()
        assertEquals(original, viewModel.uiState.value)
    }

    @Test fun clearing_viewmodel_store_cancels_active_session() = runTest(dispatcher) {
        val store = ViewModelStore()
        ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
        })[MeasureViewModel::class.java]
        repository.onProbe = { awaitCancellation() }
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        runCurrent()
        store.clear()
        advanceUntilIdle()
        assertEquals(1, repository.closedSessions)
        assertFalse(viewModel.uiState.value.isBenchmarkRunning)
        assertTrue(viewModel.uiState.value.savedSpots.isEmpty())
    }

    @Test fun reusing_viewmodel_store_keeps_history_without_reinsertion() = runTest(dispatcher) {
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
        }
        val first = ViewModelProvider(store, factory)[MeasureViewModel::class.java]
        first.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        val second = ViewModelProvider(store, factory)[MeasureViewModel::class.java]
        assertSame(first, second)
        assertEquals(1, second.uiState.value.savedSpots.size)
        assertEquals(1, repository.openedSessions)
        store.clear()
    }

    @Test fun immediate_dispatch_completion_does_not_leave_stale_job_ownership() = runTest(dispatcher) {
        val immediate = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(immediate)
        val model = MeasureViewModel(PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), immediate))
        repeat(2) {
            assertTrue(model.startBenchmark(UseCaseCategory.GAMING))
            assertFalse(model.uiState.value.isBenchmarkRunning)
        }
        assertEquals(2, model.uiState.value.savedSpots.size)
        assertEquals(1, repository.maxActiveSessions)
    }
}
