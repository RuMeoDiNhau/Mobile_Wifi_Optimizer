package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.domain.model.BenchmarkStage
import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.usecase.BenchmarkTimeoutException
import com.mobilelegend.wlo.domain.usecase.CalculateScoreUseCase
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureStatus
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BenchmarkTimingTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeWifiRepository()
    private val benchmark = PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), dispatcher)
    private lateinit var model: MeasureViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        model = MeasureViewModel(benchmark)
    }

    @After fun tearDown() {
        model.cancelBenchmark()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test fun fast_measurement_has_no_artificial_delay_and_reports_real_stages() = runTest(dispatcher) {
        val stages = mutableListOf<BenchmarkStage>()
        benchmark(UseCaseCategory.GAMING, onStage = { stages.add(it) })
        assertEquals(0L, testScheduler.currentTime)
        assertEquals(listOf(BenchmarkStage.PREPARING, BenchmarkStage.PROBING, BenchmarkStage.SCORING), stages)
        assertEquals(3, repository.requestedPackets)
        assertEquals(1, repository.closedSessions)
    }

    @Test fun slow_network_can_finish_after_three_seconds_within_total_budget() = runTest(dispatcher) {
        repository.onOpen = { delay(2_500L) }
        repository.onProbe = { delay(6_000L) }
        model.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        assertEquals(8_500L, testScheduler.currentTime)
        assertTrue(model.uiState.value.status is MeasureStatus.Completed)
        assertEquals(1, model.uiState.value.savedSpots.size)
    }

    @Test fun setup_timeout_closes_session_without_probing_or_scoring() = runTest(dispatcher) {
        val stages = mutableListOf<BenchmarkStage>()
        repository.onOpen = { awaitCancellation() }
        try {
            benchmark(UseCaseCategory.GAMING, onStage = { stages.add(it) })
            fail("Setup must obey total deadline")
        } catch (expected: BenchmarkTimeoutException) {
            assertEquals(PerformBenchmarkUseCase.TOTAL_TIMEOUT_MS, testScheduler.currentTime)
            assertEquals(listOf(BenchmarkStage.PREPARING), stages)
            assertEquals(0, repository.pingCalls)
            assertEquals(1, repository.closedSessions)
        }
    }

    @Test fun setup_and_probe_share_one_total_deadline() = runTest(dispatcher) {
        repository.onOpen = { delay(3_000L) }
        repository.onProbe = { delay(8_000L) }
        model.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        assertEquals(10_000L, testScheduler.currentTime)
        assertTrue(model.uiState.value.status is MeasureStatus.Failed)
        assertTrue((model.uiState.value.status as MeasureStatus.Failed).errorMessage.contains("10 giây"))
        assertEquals(1, repository.closedSessions)
        assertTrue(model.uiState.value.savedSpots.isEmpty())
        assertFalse(model.uiState.value.isBenchmarkRunning)
    }

    @Test fun result_just_before_deadline_is_accepted() = runTest(dispatcher) {
        repository.onProbe = { delay(9_999L) }
        model.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        assertTrue(model.uiState.value.status is MeasureStatus.Completed)
        assertEquals(9_999L, testScheduler.currentTime)
    }

    @Test fun three_tcp_failures_within_budget_are_saved_as_zero_not_total_timeout() = runTest(dispatcher) {
        repository.onProbe = { delay(6_000L) }
        repository.ping = PingResult(2_000L, 100, false)
        model.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        val score = (model.uiState.value.status as MeasureStatus.Completed).score
        assertEquals(6_000L, testScheduler.currentTime)
        assertEquals(0, score.finalScore)
        assertEquals(100, score.tcpFailurePercent)
        assertEquals(false, score.pingSuccessful)
        assertEquals(score, model.uiState.value.savedSpots.single().score)
    }

    @Test fun result_at_deadline_is_not_saved() = runTest(dispatcher) {
        repository.onProbe = { delay(10_000L) }
        model.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        assertTrue(model.uiState.value.status is MeasureStatus.Failed)
        assertTrue(model.uiState.value.savedSpots.isEmpty())
    }

    @Test fun caller_timeout_remains_cancellation_instead_of_benchmark_failure() = runTest(dispatcher) {
        repository.onProbe = { awaitCancellation() }
        try {
            withTimeout(500L) { benchmark(UseCaseCategory.GAMING) }
            fail("Caller timeout must propagate")
        } catch (expected: TimeoutCancellationException) {
            assertEquals(500L, testScheduler.currentTime)
            assertEquals(1, repository.closedSessions)
        }
    }

    @Test fun manual_cancel_does_not_become_timeout_error_later() = runTest(dispatcher) {
        repository.onProbe = { awaitCancellation() }
        model.startBenchmark(UseCaseCategory.GAMING)
        runCurrent()
        advanceTimeBy(500L)
        model.cancelBenchmark()
        advanceUntilIdle()
        assertEquals(MeasureStatus.Cancelled, model.uiState.value.status)
        assertTrue(model.uiState.value.savedSpots.isEmpty())
        assertEquals(1, repository.closedSessions)
    }

    @Test fun timeout_waits_for_cleanup_before_accepting_new_measurement() = runTest(dispatcher) {
        val cleanup = CompletableDeferred<Unit>()
        repository.onProbe = { awaitCancellation() }
        repository.onClose = { withContext(NonCancellable) { cleanup.await() } }
        model.startBenchmark(UseCaseCategory.GAMING)
        try {
            advanceTimeBy(10_000L)
            runCurrent()
            assertTrue(model.uiState.value.isBenchmarkRunning)
            assertFalse(model.startBenchmark(UseCaseCategory.GAMING))
            assertEquals(1, repository.activeSessions)
            assertTrue(model.uiState.value.savedSpots.isEmpty())
        } finally {
            cleanup.complete(Unit)
        }
        runCurrent()
        assertTrue(model.uiState.value.status is MeasureStatus.Failed)
        repository.onProbe = {}
        repository.onClose = {}
        assertTrue(model.startBenchmark(UseCaseCategory.GAMING))
        advanceUntilIdle()
        assertEquals(1, repository.maxActiveSessions)
        assertEquals(1, model.uiState.value.savedSpots.size)
    }

    @Test fun noncancellable_late_success_cannot_create_history() = runTest(dispatcher) {
        repository.onProbe = { withContext(NonCancellable) { delay(11_000L) } }
        model.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        assertTrue(model.uiState.value.status is MeasureStatus.Failed)
        assertTrue(model.uiState.value.savedSpots.isEmpty())
        assertEquals(1, repository.closedSessions)
    }

    @Test fun deadline_during_cleanup_rejects_an_already_computed_result() = runTest(dispatcher) {
        repository.onClose = { withContext(NonCancellable) { delay(11_000L) } }
        model.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        assertTrue(model.uiState.value.status is MeasureStatus.Failed)
        assertTrue(model.uiState.value.savedSpots.isEmpty())
        assertEquals(0, repository.activeSessions)
    }

    @Test fun viewmodel_phase_changes_only_when_real_work_advances() = runTest(dispatcher) {
        repository.onOpen = { delay(2_000L) }
        repository.onProbe = { delay(2_000L) }
        model.startBenchmark(UseCaseCategory.GAMING)
        runCurrent()
        assertEquals(BenchmarkStage.PREPARING.message,
            (model.uiState.value.status as MeasureStatus.Measuring).currentStepText)
        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(BenchmarkStage.PROBING.message,
            (model.uiState.value.status as MeasureStatus.Measuring).currentStepText)
        advanceTimeBy(2_000L)
        runCurrent()
        assertTrue(model.uiState.value.status is MeasureStatus.Completed)
    }
}
