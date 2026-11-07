package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.presentation.components.DefaultWifiPlaces
import com.mobilelegend.wlo.presentation.components.MISSING_METRIC
import com.mobilelegend.wlo.presentation.components.toWifiPlace
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureStatus
import com.mobilelegend.wlo.presentation.measure_and_result.measurementBlockReason
import com.mobilelegend.wlo.presentation.measure_and_result.networkMeasureContent
import org.junit.Assert.*
import org.junit.Test

class NetworkMeasureContentTest {
    @Test
    fun offline_idle_has_no_sample_metrics_or_recommendation() {
        val content = networkMeasureContent(MeasureStatus.Idle, onlineRadar().copy(isWifiConnected = false), null)
        assertEquals("Chưa kết nối Wi-Fi", content.headline)
        assertNull(content.score)
        assertEquals(MISSING_METRIC, content.ssid)
        assertEquals(MISSING_METRIC, content.linkSpeed)
        assertEquals(MISSING_METRIC, content.ping)
        assertEquals(MISSING_METRIC, content.isp)
        assertEquals(MISSING_METRIC, content.placeName)
        assertNull(content.measuredAtMillis)
        assertEquals(0f, content.progress, 0f)
        assertFalse(content.description.contains("4K"))
    }

    @Test
    fun online_idle_does_not_turn_live_signal_strength_into_a_benchmark_score() {
        val content = networkMeasureContent(MeasureStatus.Idle, onlineRadar(), null)
        assertEquals("Live network C", content.ssid)
        assertNull(content.score)
        assertEquals(MISSING_METRIC, content.linkSpeed)
        assertEquals(MISSING_METRIC, content.ping)
        assertEquals("Chưa đo", content.statusLabel)
    }

    @Test
    fun missing_permission_and_gps_have_distinct_explanations() {
        val noPermission = onlineRadar().let { it.copy(wifiAccess = it.wifiAccess.copy(fineLocation = false, coarseLocation = false)) }
        val noGps = onlineRadar().let { it.copy(wifiAccess = it.wifiAccess.copy(locationServiceEnabled = false)) }
        assertEquals("Cần cấp quyền vị trí chính xác để đọc Wi-Fi", measurementBlockReason(noPermission))
        assertEquals("Cần bật dịch vụ vị trí", measurementBlockReason(noGps))
        for (radar in listOf(noPermission, noGps)) {
            val content = networkMeasureContent(MeasureStatus.Idle, radar, null)
            assertEquals("Chưa sẵn sàng", content.statusLabel)
            assertNull(content.score)
        }
    }

    @Test
    fun initial_scanner_state_cannot_enable_measurement() {
        val radar = onlineRadar().let { it.copy(radarData = it.radarData.copy(isConnected = false)) }
        assertEquals("Đang chờ thông tin Wi-Fi", measurementBlockReason(radar))
        assertNull(measurementBlockReason(onlineRadar()))
    }

    @Test
    fun measuring_shows_progress_without_final_metrics_or_live_network_attribution() {
        val content = networkMeasureContent(MeasureStatus.Measuring(0.66f, "Đang đo TCP"), onlineRadar(), null)
        assertEquals(0.66f, content.progress, 0f)
        assertEquals("Đang đo TCP", content.description)
        assertNull(content.score)
        assertEquals(MISSING_METRIC, content.ssid)
        assertEquals(MISSING_METRIC, content.linkSpeed)
        assertEquals(MISSING_METRIC, content.ping)
    }

    @Test
    fun failure_is_visible_and_does_not_show_a_good_connection() {
        val content = networkMeasureContent(MeasureStatus.Failed("Lỗi cảm biến"), onlineRadar(), null)
        assertEquals("Đo thất bại", content.headline)
        assertEquals("Lỗi cảm biến", content.description)
        assertEquals("Thất bại", content.statusLabel)
        assertNull(content.score)
        assertEquals(MISSING_METRIC, content.ping)
    }

    @Test
    fun completed_zero_is_preserved_with_an_empty_gauge() {
        val content = networkMeasureContent(MeasureStatus.Completed(scoreA().copy(finalScore = 0)), onlineRadar(), null)
        assertEquals(0, content.score)
        assertEquals(0f, content.progress, 0f)
        assertEquals("Chất lượng kết nối kém", content.headline)
    }

    @Test
    fun timeout_2000_ms_is_no_response_and_never_sample_12_ms() {
        val score = scoreA().copy(rawPingMs = 2000, pingSuccessful = false)
        val content = networkMeasureContent(MeasureStatus.Completed(score), onlineRadar(), null)
        assertEquals("Không phản hồi", content.ping)
        assertEquals("Không phản hồi", content.headline)
        assertFalse(content.description.contains(score.recommendation))
    }

    @Test
    fun successful_zero_and_slow_ping_values_are_not_clamped_or_treated_as_timeout() {
        for (latency in listOf(0L, 999L, 1000L, 1500L, 2000L, 2500L)) {
            val content = networkMeasureContent(MeasureStatus.Completed(scoreA().copy(rawPingMs = latency)), onlineRadar(), null)
            assertEquals("$latency ms", content.ping)
        }
    }

    @Test
    fun missing_ping_has_no_latency_or_recommendation() {
        val content = networkMeasureContent(MeasureStatus.Completed(scoreA().copy(pingSuccessful = null)), onlineRadar(), null)
        assertEquals(MISSING_METRIC, content.ping)
        assertEquals("Thiếu dữ liệu", content.statusLabel)
        assertFalse(content.description.contains("Khuyến nghị mạng A"))
    }

    @Test
    fun low_link_speed_is_not_raised_and_unknown_link_speed_is_missing() {
        for (speed in listOf(-1, 0, 1, 5, 11)) {
            val content = networkMeasureContent(MeasureStatus.Completed(scoreA().copy(rawLinkSpeedMbps = speed)), onlineRadar(), null)
            assertEquals(if (speed > 0) "$speed Mbps" else MISSING_METRIC, content.linkSpeed)
            if (speed <= 0) assertEquals("Thiếu dữ liệu", content.statusLabel)
        }
    }

    @Test
    fun completed_result_keeps_network_category_and_time_when_live_network_changes() {
        val score = scoreA()
        val content = networkMeasureContent(MeasureStatus.Completed(score), onlineRadar(), null)
        assertEquals("Network A", content.ssid)
        assertEquals("AA:BB:CC:DD:EE:01", content.bssid)
        assertEquals(score.measurement!!.measuredAtMillis, content.measuredAtMillis)
        assertEquals(UseCaseCategory.GAMING, content.category)
        assertEquals("150 Mbps", content.linkSpeed)
        assertEquals(score.recommendation, content.description)
        val offline = networkMeasureContent(MeasureStatus.Completed(score), onlineRadar().copy(isWifiConnected = false), null)
        assertEquals(content, offline)
    }

    @Test
    fun completed_result_without_identity_never_borrows_live_ssid_or_time() {
        val content = networkMeasureContent(MeasureStatus.Completed(scoreA().copy(measurement = null)), onlineRadar(), null)
        assertEquals(MISSING_METRIC, content.ssid)
        assertEquals(MISSING_METRIC, content.bssid)
        assertNull(content.measuredAtMillis)
    }

    @Test
    fun measuring_A_then_inspecting_demo_B_uses_only_B_in_every_measurement_state() {
        val placeB = DefaultWifiPlaces.first().copy(id = "B", name = "Cafe B", ssid = "Network B", score = 42, pingMs = 78)
        for (status in listOf(MeasureStatus.Idle, MeasureStatus.Completed(scoreA()), MeasureStatus.Measuring(0.5f, "A"), MeasureStatus.Failed("A"))) {
            val content = networkMeasureContent(status, onlineRadar(), placeB)
            assertEquals("Network B", content.ssid)
            assertEquals("Cafe B", content.placeName)
            assertEquals(42, content.score)
            assertEquals("78 ms", content.ping)
            assertEquals(MISSING_METRIC, content.linkSpeed)
            assertNull(content.category)
            assertNull(content.measuredAtMillis)
            assertEquals("Dữ liệu minh họa", content.statusLabel)
        }
    }

    @Test
    fun inspected_measured_B_keeps_its_own_result_category_and_timestamp() {
        val scoreB = scoreA().copy(
            finalScore = 29, rawLinkSpeedMbps = 7, rawPingMs = 2000, pingSuccessful = false,
            category = UseCaseCategory.VIDEO_CALL,
            measurement = scoreA().measurement!!.copy(id = "B", ssid = "Network B", measuredAtMillis = 123L)
        )
        val content = networkMeasureContent(MeasureStatus.Completed(scoreA()), onlineRadar(), scoreB.toWifiPlace())
        assertEquals("Network B", content.ssid)
        assertEquals(29, content.score)
        assertEquals("7 Mbps", content.linkSpeed)
        assertEquals("Không phản hồi", content.ping)
        assertEquals(UseCaseCategory.VIDEO_CALL, content.category)
        assertEquals(123L, content.measuredAtMillis)
    }

    @Test
    fun clearing_inspection_then_starting_measurement_removes_B_from_the_display() {
        val content = networkMeasureContent(MeasureStatus.Measuring(0f, "Đang chuẩn bị"), onlineRadar(), null)
        assertEquals(MISSING_METRIC, content.placeName)
        assertNull(content.score)
        assertEquals("Đang đo", content.statusLabel)
    }
}
