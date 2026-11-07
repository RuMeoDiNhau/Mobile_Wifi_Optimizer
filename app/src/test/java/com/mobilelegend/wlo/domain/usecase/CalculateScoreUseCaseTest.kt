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
            tcpFailurePercent = 0,
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
            tcpFailurePercent = 0,
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
            tcpFailurePercent = 100,
            isSuccess = false
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.STUDY_DOWNLOAD)

        assertEquals(0, score.finalScore)
        assertFalse(score.isRecommended)
        assertEquals("Chưa kết nối Wi-Fi", score.summaryText)
    }

    @Test
    fun all_tcp_attempts_failed_should_return_zero_and_no_recommendation() {
        val wifiMetric = WifiMetric(
            ssid = "Unstable_Wifi",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -50,
            linkSpeedMbps = 300,
            frequencyMhz = 5180,
            isConnected = true
        )
        val pingResult = PingResult(
            latencyMs = 35,
            tcpFailurePercent = 100,
            isSuccess = false
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.GAMING)

        assertEquals(0, score.finalScore)
        assertFalse(score.isRecommended)
        assertEquals("Không kết nối được đích TCP", score.summaryText)
    }

    @Test
    fun gaming_medium_ping_should_give_moderate_evaluation() {
        // Final score >= 60 và ping <= 90ms -> "Khá ổn cho Gaming nhẹ"
        val wifiMetric = WifiMetric(
            ssid = "Home_Wifi",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -60, // Điểm sóng ~ 75
            linkSpeedMbps = 150, // Điểm tốc độ ~ 48
            frequencyMhz = 2412,
            isConnected = true
        )
        val pingResult = PingResult(
            latencyMs = 75, // Điểm ping ~ 69
            tcpFailurePercent = 0,
            isSuccess = true
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.GAMING)

        assertTrue("Điểm final phải >= 60", score.finalScore >= 60)
        assertEquals("Khá ổn cho Gaming nhẹ", score.summaryText)
        assertFalse(score.isRecommended)
    }

    @Test
    fun study_download_with_high_speed_should_be_recommended() {
        val wifiMetric = WifiMetric(
            ssid = "HighSpeed_Wifi",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -55, // Điểm sóng ~ 88
            linkSpeedMbps = 250, // Điểm tốc độ ~ 83 (W_Speed = 0.5)
            frequencyMhz = 5180,
            isConnected = true
        )
        val pingResult = PingResult(
            latencyMs = 40,
            tcpFailurePercent = 0,
            isSuccess = true
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.STUDY_DOWNLOAD)

        assertTrue("Điểm học tập phải >= 70", score.finalScore >= 70)
        assertTrue("Băng thông tốt phải được khuyến nghị", score.isRecommended)
        assertEquals("Liên kết Wi-Fi tốt", score.summaryText)
    }

    @Test
    fun study_download_with_medium_speed_should_be_acceptable_for_web_reading() {
        val wifiMetric = WifiMetric(
            ssid = "Average_Wifi",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -54, // Sóng tốt (điểm ~ 90) bù lại tốc độ vừa phải
            linkSpeedMbps = 45, // Tốc độ < 50Mbps
            frequencyMhz = 2412,
            isConnected = true
        )
        val pingResult = PingResult(
            latencyMs = 30,
            tcpFailurePercent = 0,
            isSuccess = true
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.STUDY_DOWNLOAD)

        assertEquals("Liên kết Wi-Fi trung bình", score.summaryText)
        assertFalse(score.isRecommended)
    }

    @Test
    fun study_download_with_very_low_speed_should_give_warning() {
        val wifiMetric = WifiMetric(
            ssid = "Slow_Wifi",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -80,
            linkSpeedMbps = 8, // < 10Mbps -> 0 điểm tốc độ
            frequencyMhz = 2412,
            isConnected = true
        )
        val pingResult = PingResult(
            latencyMs = 60,
            tcpFailurePercent = 0,
            isSuccess = true
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.STUDY_DOWNLOAD)

        assertTrue("Điểm phải thấp khi tốc độ cực yếu", score.finalScore < 50)
        assertEquals("Liên kết Wi-Fi yếu hoặc chưa xác định", score.summaryText)
        assertFalse(score.isRecommended)
    }

    @Test
    fun video_call_with_good_rssi_and_ping_should_be_recommended() {
        val wifiMetric = WifiMetric(
            ssid = "Meet_Wifi",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -58, // >= -75dBm (điểm sóng 80)
            linkSpeedMbps = 150, // điểm tốc độ 48
            frequencyMhz = 5180,
            isConnected = true
        )
        val pingResult = PingResult(
            latencyMs = 35, // <= 100ms (điểm ping 92)
            tcpFailurePercent = 0,
            isSuccess = true
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.VIDEO_CALL)

        assertTrue("Điểm Video Call phải >= 70", score.finalScore >= 70)
        assertTrue("Phải được khuyến nghị gọi Video", score.isRecommended)
        assertEquals("Tín hiệu Wi-Fi và TCP đạt ngưỡng gọi video", score.summaryText)
    }

    @Test
    fun video_call_with_poor_rssi_should_not_be_recommended() {
        val wifiMetric = WifiMetric(
            ssid = "Distant_Wifi",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -82, // < -75dBm
            linkSpeedMbps = 100,
            frequencyMhz = 2412,
            isConnected = true
        )
        val pingResult = PingResult(
            latencyMs = 40,
            tcpFailurePercent = 0,
            isSuccess = true
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.VIDEO_CALL)

        assertFalse("Sóng yếu không được khuyến nghị Video Call", score.isRecommended)
        assertEquals("Chưa đạt ngưỡng tham khảo cho gọi video", score.summaryText)
    }

    @Test
    fun extreme_edge_case_near_router_should_clamp_to_100() {
        val wifiMetric = WifiMetric(
            ssid = "NextToRouter",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -15, // Cực mạnh
            linkSpeedMbps = 1200, // Wi-Fi 6 siêu tốc
            frequencyMhz = 5800,
            isConnected = true
        )
        val pingResult = PingResult(
            latencyMs = 2, // Ping siêu thấp
            tcpFailurePercent = 0,
            isSuccess = true
        )

        val score = calculateScoreUseCase(wifiMetric, pingResult, UseCaseCategory.GAMING)

        assertEquals(100, score.finalScore)
        assertEquals(100, score.normalizedRssiScore)
        assertEquals(100, score.normalizedPingScore)
        assertEquals(100, score.normalizedSpeedScore)
        assertTrue(score.isRecommended)
    }
}
