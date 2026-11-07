package com.mobilelegend.wlo.domain.usecase

import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test kiểm thử tiến trình điều phối Benchmark 1-chạm (3 giây) của PerformBenchmarkUseCase.
 */
class PerformBenchmarkUseCaseTest {

    private lateinit var performBenchmarkUseCase: PerformBenchmarkUseCase
    private val calculateScoreUseCase = CalculateScoreUseCase()

    private class MockWifiRepository : WifiRepository {
        var metric = WifiMetric(
            ssid = "Benchmark_AP",
            bssid = "11:22:33:44:55:66",
            rssi = -52,
            linkSpeedMbps = 250,
            frequencyMhz = 5200,
            isConnected = true
        )
        var ping = PingResult(
            latencyMs = 22,
            packetLossPercent = 0,
            isSuccess = true
        )
        var measuredPacketsCount = 0

        override fun getLiveWifiStream(): Flow<WifiMetric> = emptyFlow()
        override fun getCurrentWifiMetric(): WifiMetric = metric
        override suspend fun measurePing(packetsCount: Int): PingResult {
            measuredPacketsCount = packetsCount
            return ping
        }
        override fun isWifiConnected(): Boolean = metric.isConnected
        override fun isReadyForScanning(): Boolean = true
    }

    private lateinit var mockRepo: MockWifiRepository

    @Before
    fun setUp() {
        mockRepo = MockWifiRepository()
        performBenchmarkUseCase = PerformBenchmarkUseCase(mockRepo, calculateScoreUseCase)
    }

    @Test
    fun benchmark_orchestration_measures_3_packets_and_calculates_score() = runBlocking {
        val result = performBenchmarkUseCase(UseCaseCategory.GAMING)

        assertEquals("Phải đo đúng 3 gói tin", 3, mockRepo.measuredPacketsCount)
        assertEquals(UseCaseCategory.GAMING, result.category)
        assertTrue("Điểm gaming phải cao khi sóng tốt và ping thấp", result.finalScore >= 85)
        assertTrue(result.isRecommended)
    }

    @Test
    fun benchmark_for_study_download_weights_bandwidth_properly() = runBlocking {
        mockRepo.metric = mockRepo.metric.copy(linkSpeedMbps = 300, rssi = -50)
        mockRepo.ping = PingResult(latencyMs = 30, packetLossPercent = 0, isSuccess = true)

        val result = performBenchmarkUseCase(UseCaseCategory.STUDY_DOWNLOAD)

        assertTrue(result.finalScore >= 90)
        assertTrue(result.isRecommended)
        assertEquals("Liên kết Wi-Fi tốt", result.summaryText)
    }

    @Test
    fun benchmark_fails_gracefully_when_ping_fails() = runBlocking {
        mockRepo.ping = PingResult(latencyMs = 2000, packetLossPercent = 100, isSuccess = false)

        val result = performBenchmarkUseCase(UseCaseCategory.VIDEO_CALL)

        assertEquals(15, result.finalScore)
        assertEquals("Mạng mất tín hiệu Internet / Rớt gói", result.summaryText)
    }
}
