package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.usecase.CalculateScoreUseCase
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import com.mobilelegend.wlo.presentation.components.toWifiPlace
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureStatus
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureViewModel
import com.mobilelegend.wlo.presentation.measure_and_result.networkMeasureContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BenchmarkRegressionTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeWifiRepository()
    private val benchmark = PerformBenchmarkUseCase(repository, CalculateScoreUseCase(), dispatcher) { 1234L }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun benchmark_captures_the_sampled_network_category_time_and_optional_location() = runTest(dispatcher) {
        val result = benchmark(UseCaseCategory.VIDEO_CALL, MeasurementLocation(1.25, 2.75))
        repository.metric = repository.metric.copy(ssid = "Network B", bssid = "AA:BB:CC:DD:EE:02")
        val context = requireNotNull(result.measurement)
        assertEquals("Network A", context.ssid)
        assertEquals("AA:BB:CC:DD:EE:01", context.bssid)
        assertEquals(1234L, context.measuredAtMillis)
        assertEquals(MeasurementLocation(1.25, 2.75), context.location)
        assertEquals(UseCaseCategory.VIDEO_CALL, result.category)
        assertEquals(3, repository.requestedPackets)
        assertTrue(context.id.isNotBlank())
        assertEquals(true, result.pingSuccessful)
    }

    @Test
    fun separate_benchmarks_get_different_ids_even_when_the_clock_is_identical() = runTest(dispatcher) {
        val first = benchmark(UseCaseCategory.GAMING)
        val second = benchmark(UseCaseCategory.GAMING)
        assertNotEquals(first.measurement!!.id, second.measurement!!.id)
        assertNull(first.measurement?.location)
    }

    @Test
    fun offline_attempt_reports_failure_and_never_probes_or_saves_a_good_result() = runTest(dispatcher) {
        repository.metric = repository.metric.copy(isConnected = false)
        val viewModel = MeasureViewModel(benchmark)
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        val failure = viewModel.uiState.value.status as MeasureStatus.Failed
        assertTrue(failure.errorMessage.contains("Chưa kết nối Wi-Fi"))
        assertEquals(0, repository.pingCalls)
        assertTrue(viewModel.uiState.value.savedSpots.isEmpty())
    }

    @Test
    fun missing_scanning_requirements_are_rejected_at_the_use_case() = runTest(dispatcher) {
        repository.ready = false
        try {
            benchmark(UseCaseCategory.GAMING)
            fail("Missing permissions/GPS must not produce a result")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("quyền"))
        }
        assertEquals(0, repository.pingCalls)
    }

    @Test
    fun timeout_status_survives_scoring_history_and_place_conversion() = runTest(dispatcher) {
        repository.ping = PingResult(2000, 100, false)
        val viewModel = MeasureViewModel(benchmark)
        viewModel.startBenchmark(UseCaseCategory.GAMING, MeasurementLocation(1.0, 2.0))
        advanceUntilIdle()
        val score = (viewModel.uiState.value.status as MeasureStatus.Completed).score
        assertEquals(false, score.pingSuccessful)
        assertEquals(2000L, score.rawPingMs)
        assertFalse(score.isRecommended)
        assertEquals(score, viewModel.uiState.value.savedSpots.single().score)
        val place = requireNotNull(score.toWifiPlace())
        assertEquals(2000L, place.pingMs)
        assertEquals(false, place.pingSuccessful)
        assertEquals("Không phản hồi", networkMeasureContent(MeasureStatus.Completed(score), onlineRadar(), place).ping)
    }

    @Test
    fun failed_retry_clears_old_result_display_and_keeps_existing_history() = runTest(dispatcher) {
        val viewModel = MeasureViewModel(benchmark)
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        val saved = viewModel.uiState.value.savedSpots.single()
        assertEquals(1234L, saved.timestamp)
        assertEquals(saved.score.measurement!!.id, saved.id)

        repository.error = IllegalStateException("Lỗi cảm biến thử nghiệm")
        viewModel.startBenchmark(UseCaseCategory.VIDEO_CALL)
        assertTrue(viewModel.uiState.value.status is MeasureStatus.Measuring)
        advanceUntilIdle()
        val status = viewModel.uiState.value.status
        assertTrue(status is MeasureStatus.Failed)
        assertEquals(listOf(saved), viewModel.uiState.value.savedSpots)
        val content = networkMeasureContent(status, onlineRadar(), null)
        assertNull(content.score)
        assertEquals("Lỗi cảm biến thử nghiệm", content.description)
    }

    @Test
    fun zero_score_is_saved_as_zero_and_never_replaced_by_a_default() = runTest(dispatcher) {
        repository.metric = repository.metric.copy(rssi = -100, linkSpeedMbps = 1)
        repository.ping = PingResult(250, 0, true)
        val viewModel = MeasureViewModel(benchmark)
        viewModel.startBenchmark(UseCaseCategory.GAMING)
        advanceUntilIdle()
        val score = (viewModel.uiState.value.status as MeasureStatus.Completed).score
        assertEquals(0, score.finalScore)
        assertEquals(0, viewModel.uiState.value.savedSpots.single().score.finalScore)
        val content = networkMeasureContent(viewModel.uiState.value.status, onlineRadar(), null)
        assertEquals(0, content.score)
        assertEquals("1 Mbps", content.linkSpeed)
    }
}
