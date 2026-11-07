package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.core.network.PingTester
import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.usecase.CalculateScoreUseCase
import com.mobilelegend.wlo.domain.usecase.PerformBenchmarkUseCase
import com.mobilelegend.wlo.domain.usecase.TCP_MEASUREMENT_SCOPE
import com.mobilelegend.wlo.presentation.components.tcpFailureText
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
import org.junit.Assert.*
import org.junit.Test
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class TcpFailureScoringTest {
    private val scorer = CalculateScoreUseCase()
    private val metric = WifiMetric("A", "AA:BB:CC:DD:EE:01", -50, 300, 5180)

    @Test fun zero_failures_preserves_full_score_in_all_categories() {
        for (category in UseCaseCategory.values()) {
            val score = scorer(metric, PingResult(10, 0, true), category)
            assertEquals(100, score.finalScore)
            assertEquals(0, score.tcpFailurePercent)
            assertTrue(score.isRecommended)
        }
    }

    @Test fun one_of_three_failures_reduces_score_and_blocks_recommendation() {
        for (category in UseCaseCategory.values()) {
            val score = scorer(metric, PingResult(10, 33, true), category)
            assertEquals(67, score.finalScore)
            assertFalse(score.isRecommended)
            assertEquals(33, score.tcpFailurePercent)
            assertTrue(score.recommendation.contains("33%"))
        }
    }

    @Test fun two_of_three_failures_preserves_success_latency_but_penalizes_score() {
        for (category in UseCaseCategory.values()) {
            val score = scorer(metric, PingResult(10, 66, true), category)
            assertEquals(34, score.finalScore)
            assertEquals(10L, score.rawPingMs)
            assertEquals(true, score.pingSuccessful)
            assertFalse(score.isRecommended)
        }
    }

    @Test fun all_failures_cannot_receive_positive_score_from_strong_wifi() {
        for (category in UseCaseCategory.values()) {
            val score = scorer(metric, PingResult(2_000, 100, false), category)
            assertEquals(0, score.finalScore)
            assertEquals(0, score.normalizedPingScore)
            assertFalse(score.isRecommended)
            assertEquals(100, score.tcpFailurePercent)
            assertEquals("Không phản hồi", networkMeasureContent(MeasureStatus.Completed(score), onlineRadar(), null).ping)
        }
    }

    @Test fun score_never_increases_when_failure_rate_increases() {
        for (category in UseCaseCategory.values()) {
            var previous = 101
            for (failureRate in 0..100) {
                val score = scorer(metric.copy(rssi = -65, linkSpeedMbps = 90),
                    PingResult(60, failureRate, failureRate < 100), category)
                assertTrue(score.finalScore in 0..previous)
                if (failureRate > 0) assertFalse(score.isRecommended)
                previous = score.finalScore
            }
        }
    }

    @Test fun weak_wifi_with_failed_attempts_is_not_lifted_to_a_fixed_score() {
        val score = scorer(metric.copy(rssi = -100, linkSpeedMbps = 1),
            PingResult(250, 66, true), UseCaseCategory.GAMING)
        assertEquals(0, score.finalScore)
        assertFalse(score.isRecommended)
    }

    @Test fun invalid_or_contradictory_tcp_results_are_rejected() {
        assertThrows(IllegalArgumentException::class.java) { PingResult(10, -1, true) }
        assertThrows(IllegalArgumentException::class.java) { PingResult(10, 101, false) }
        assertThrows(IllegalArgumentException::class.java) { PingResult(10, 100, true) }
        assertThrows(IllegalArgumentException::class.java) { PingResult(10, 33, false) }
    }

    @Test fun evaluation_is_scoped_to_tcp_destination_without_promising_game_or_video_quality() {
        for (category in UseCaseCategory.values()) {
            for (failureRate in listOf(0, 33, 66, 100)) {
                val score = scorer(metric, PingResult(10, failureRate, failureRate < 100), category)
                assertTrue(score.recommendation.contains(TCP_MEASUREMENT_SCOPE))
                assertFalse(score.recommendation.contains("PUBG"))
                assertFalse(score.recommendation.contains("Liên Quân"))
                assertFalse(score.recommendation.contains("Hoàn hảo"))
            }
        }
    }

    @Test fun unmeasured_or_invalid_failure_rate_is_not_formatted_as_zero() {
        assertEquals("—", tcpFailureText(null))
        assertEquals("—", tcpFailureText(-1))
        assertEquals("—", tcpFailureText(101))
        assertEquals("0%", tcpFailureText(0))
        assertEquals("33%", tcpFailureText(33))
        assertEquals("100%", tcpFailureText(100))
        assertEquals("—", networkMeasureContent(MeasureStatus.Idle, onlineRadar(), null).tcpFailure)
    }

    @Test fun offline_result_does_not_claim_a_tcp_test_was_performed() {
        val score = scorer(metric.copy(isConnected = false), PingResult(2_000, 100, false), UseCaseCategory.GAMING)
        assertEquals(0, score.finalScore)
        assertNull(score.tcpFailurePercent)
        assertNull(score.pingSuccessful)
    }

    @Test fun partial_failure_survives_history_and_place_view_without_using_current_network() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeWifiRepository().apply { ping = PingResult(10, 33, true) }
        val model = MeasureViewModel(PerformBenchmarkUseCase(repository, scorer, dispatcher))
        try {
            model.startBenchmark(UseCaseCategory.GAMING, MeasurementLocation(10.0, 106.0))
            advanceUntilIdle()
            val score = model.uiState.value.savedSpots.single().score
            val place = requireNotNull(score.toWifiPlace())
            assertEquals(33, score.tcpFailurePercent)
            assertEquals(33, place.measurement!!.tcpFailurePercent)
            assertEquals("33%", networkMeasureContent(MeasureStatus.Idle, onlineRadar(), place).tcpFailure)
            model.saveSpotToHistory("A", score)
            assertEquals(score, model.uiState.value.savedSpots.single().score)
        } finally {
            model.cancelBenchmark()
            Dispatchers.resetMain()
        }
    }

    @Test fun fake_socket_batches_report_zero_one_two_and_three_failures() = runTest {
        for (failedCount in 0..3) {
            var connections = 0
            var closes = 0
            var clock = 0L
            val tester = PingTester(dispatcher = StandardTestDispatcher(testScheduler), nanoTime = { clock })
            val result = tester.batchPing(socketFactory = {
                object : Socket() {
                    override fun connect(endpoint: SocketAddress, timeout: Int) {
                        connections++
                        if (connections <= failedCount) {
                            clock += 2_000_000_000L
                            throw SocketTimeoutException("fake timeout")
                        }
                        clock += 10_000_000L
                    }
                    override fun close() { closes++ }
                }
            })
            assertEquals(3, connections)
            assertEquals(3, closes)
            assertEquals(listOf(0, 33, 66, 100)[failedCount], result.tcpFailurePercent)
            assertEquals(failedCount < 3, result.isSuccess)
            assertEquals(if (failedCount < 3) 10L else 2_000L, result.latencyMs)
            assertFalse(result.message.contains("rớt gói", ignoreCase = true))
        }
    }
}
