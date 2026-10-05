package com.mobilelegend.wlo.domain.usecase

import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test kiểm thử thuật toán chuẩn hóa và chấm điểm Scoring Engine trong CalculateScoreUseCase.
 */
class CalculateScoreUseCaseTest {

    private lateinit var calculateScoreUseCase: CalculateScoreUseCase

    @Before
    fun setUp() {
        calculateScoreUseCase = CalculateScoreUseCase()
    }

    @Test
    fun normalize_rssi_boundary_tests() {
        // >= -50 dBm phải đạt tối đa 100 điểm (không vượt 100)
        assertEquals(100, calculateScoreUseCase.normalizeRssi(-30))
        assertEquals(100, calculateScoreUseCase.normalizeRssi(-50))

        // Ngưỡng giữa (-70 dBm) phải đạt khoảng 50 điểm
        assertEquals(50, calculateScoreUseCase.normalizeRssi(-70))

        // <= -90 dBm phải là 0 điểm (không âm)
        assertEquals(0, calculateScoreUseCase.normalizeRssi(-90))
        assertEquals(0, calculateScoreUseCase.normalizeRssi(-110))
    }

    @Test
    fun normalize_ping_boundary_tests() {
        // <= 20ms phải đạt tối đa 100 điểm
        assertEquals(100, calculateScoreUseCase.normalizePing(10))
        assertEquals(100, calculateScoreUseCase.normalizePing(20))

        // Ngưỡng giữa (110ms) phải đạt 50 điểm
        assertEquals(50, calculateScoreUseCase.normalizePing(110))

        // >= 200ms phải là 0 điểm
        assertEquals(0, calculateScoreUseCase.normalizePing(200))
        assertEquals(0, calculateScoreUseCase.normalizePing(500))
    }

    @Test
    fun normalize_speed_boundary_tests() {
        // >= 300Mbps phải đạt 100 điểm
        assertEquals(100, calculateScoreUseCase.normalizeLinkSpeed(500))
        assertEquals(100, calculateScoreUseCase.normalizeLinkSpeed(300))

        // <= 10Mbps phải là 0 điểm
        assertEquals(0, calculateScoreUseCase.normalizeLinkSpeed(10))
        assertEquals(0, calculateScoreUseCase.normalizeLinkSpeed(0))
    }

    @Test
    fun gaming_with_low_ping_should_give_high_score() {
        val wifiMetric = WifiMetric(
            ssid = "Gaming_Network",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -52, // Điểm sóng ~ 95
            linkSpeedMbps = 200, // Điểm tốc độ ~ 66
            frequencyMhz = 5180,
            isConnected = true
        )
        val pingResult = PingResult(
            latencyMs = 25, // Điểm ping ~ 97
            packetLossPercent = 0,
            isSuccess = true
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.GAMING)

        assertTrue("Điểm gaming phải >= 85", score.finalScore >= 85)
        assertTrue("Vị trí phải được khuyến nghị cho gaming", score.isRecommended)
    }

    @Test
    fun gaming_with_high_ping_should_not_be_recommended() {
        val wifiMetric = WifiMetric(
            ssid = "Weak_Wifi",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -55,
            linkSpeedMbps = 300,
            frequencyMhz = 5180,
            isConnected = true
        )
        val pingResult = PingResult(
            latencyMs = 180, // High ping -> Điểm ping ~ 11
            packetLossPercent = 0,
            isSuccess = true
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.GAMING)

        assertTrue("Điểm gaming với ping cao phải < 65", score.finalScore < 65)
        assertFalse("Vị trí ping cao không được khuyến nghị", score.isRecommended)
    }

    @Test
    fun disconnected_wifi_should_return_zero_score() {
        val wifiMetric = WifiMetric(
            ssid = "Offline",
            bssid = "00:00:00:00:00:00",
            rssi = -100,
            linkSpeedMbps = 0,
            frequencyMhz = 0,
            isConnected = false
        )
        val pingResult = PingResult(
            latencyMs = 2000,
            packetLossPercent = 100,
            isSuccess = false
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.STUDY_DOWNLOAD)

        assertEquals(0, score.finalScore)
        assertFalse(score.isRecommended)
    }
}
