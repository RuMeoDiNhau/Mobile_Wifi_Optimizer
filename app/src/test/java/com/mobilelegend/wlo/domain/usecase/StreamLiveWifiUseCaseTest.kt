package com.mobilelegend.wlo.domain.usecase

import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.SignalQualityLevel
import com.mobilelegend.wlo.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test kiểm thử logic phân cấp sóng, chuyển đổi % tín hiệu và luồng dữ liệu của StreamLiveWifiUseCase.
 */
class StreamLiveWifiUseCaseTest {

    private lateinit var useCase: StreamLiveWifiUseCase

    // Fake repository để test Flow mà không phụ thuộc phần cứng Android
    private val fakeRepository = object : WifiRepository {
        var currentMetric = WifiMetric(
            ssid = "Test_Wifi",
            bssid = "00:11:22:33:44:55",
            rssi = -60,
            linkSpeedMbps = 150,
            frequencyMhz = 5180,
            isConnected = true
        )

        override fun getLiveWifiStream(): Flow<WifiMetric> = flowOf(currentMetric)
        override fun getCurrentWifiMetric(): WifiMetric = currentMetric
        override suspend fun measurePing(packetsCount: Int): PingResult = PingResult(20, 0, true)
        override fun isWifiConnected(): Boolean = currentMetric.isConnected
        override fun isReadyForScanning(): Boolean = true
    }

    @Before
    fun setUp() {
        useCase = StreamLiveWifiUseCase(fakeRepository)
    }

    @Test
    fun quality_level_excellent_when_rssi_at_or_above_minus_60() {
        val metric1 = fakeRepository.currentMetric.copy(rssi = -60)
        val metric2 = fakeRepository.currentMetric.copy(rssi = -45)
        val metric3 = fakeRepository.currentMetric.copy(rssi = -20)

        assertEquals(SignalQualityLevel.EXCELLENT, useCase.mapToLiveRadarData(metric1).qualityLevel)
        assertEquals(SignalQualityLevel.EXCELLENT, useCase.mapToLiveRadarData(metric2).qualityLevel)
        assertEquals(SignalQualityLevel.EXCELLENT, useCase.mapToLiveRadarData(metric3).qualityLevel)
    }

    @Test
    fun quality_level_good_when_rssi_between_minus_61_and_minus_75() {
        val metric1 = fakeRepository.currentMetric.copy(rssi = -61)
        val metric2 = fakeRepository.currentMetric.copy(rssi = -68)
        val metric3 = fakeRepository.currentMetric.copy(rssi = -75)

        assertEquals(SignalQualityLevel.GOOD, useCase.mapToLiveRadarData(metric1).qualityLevel)
        assertEquals(SignalQualityLevel.GOOD, useCase.mapToLiveRadarData(metric2).qualityLevel)
        assertEquals(SignalQualityLevel.GOOD, useCase.mapToLiveRadarData(metric3).qualityLevel)
    }

    @Test
    fun quality_level_poor_when_rssi_below_or_equal_minus_76() {
        val metric1 = fakeRepository.currentMetric.copy(rssi = -76)
        val metric2 = fakeRepository.currentMetric.copy(rssi = -85)
        val metric3 = fakeRepository.currentMetric.copy(rssi = -105)

        assertEquals(SignalQualityLevel.POOR, useCase.mapToLiveRadarData(metric1).qualityLevel)
        assertEquals(SignalQualityLevel.POOR, useCase.mapToLiveRadarData(metric2).qualityLevel)
        assertEquals(SignalQualityLevel.POOR, useCase.mapToLiveRadarData(metric3).qualityLevel)
    }

    @Test
    fun percentage_calculation_boundaries() {
        // >= -50 dBm -> 100%
        assertEquals(100, useCase.mapToLiveRadarData(fakeRepository.currentMetric.copy(rssi = -50)).signalPercentage)
        assertEquals(100, useCase.mapToLiveRadarData(fakeRepository.currentMetric.copy(rssi = -30)).signalPercentage)

        // <= -100 dBm -> 0%
        assertEquals(0, useCase.mapToLiveRadarData(fakeRepository.currentMetric.copy(rssi = -100)).signalPercentage)
        assertEquals(0, useCase.mapToLiveRadarData(fakeRepository.currentMetric.copy(rssi = -120)).signalPercentage)

        // -75 dBm -> 50%
        assertEquals(50, useCase.mapToLiveRadarData(fakeRepository.currentMetric.copy(rssi = -75)).signalPercentage)

        // -60 dBm -> 80% (2 * (-60 + 100) = 80)
        assertEquals(80, useCase.mapToLiveRadarData(fakeRepository.currentMetric.copy(rssi = -60)).signalPercentage)
    }

    @Test
    fun flow_emits_correctly_mapped_live_radar_data() = runBlocking {
        val radarData = useCase().first()

        assertEquals("Test_Wifi", radarData.ssid)
        assertEquals(-60, radarData.rssi)
        assertEquals(80, radarData.signalPercentage)
        assertEquals(SignalQualityLevel.EXCELLENT, radarData.qualityLevel)
        assertTrue(radarData.isConnected)
    }
}
