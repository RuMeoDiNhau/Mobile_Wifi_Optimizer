package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.presentation.components.DefaultWifiPlaces
import com.mobilelegend.wlo.presentation.components.MISSING_METRIC
import com.mobilelegend.wlo.presentation.components.linkSpeedText
import com.mobilelegend.wlo.presentation.components.networkName
import com.mobilelegend.wlo.presentation.components.pingText
import com.mobilelegend.wlo.presentation.components.toWifiPlace
import com.mobilelegend.wlo.presentation.measure_and_result.MeasureStatus
import com.mobilelegend.wlo.presentation.measure_and_result.networkMeasureContent
import org.junit.Assert.*
import org.junit.Test

class MeasuredWifiPlaceTest {
    @Test
    fun low_and_unknown_link_speeds_match_across_measurement_and_place_views() {
        for (speed in listOf(-1, 0, 1, 5, 11, 150)) {
            val score = scoreA().copy(rawLinkSpeedMbps = speed)
            val place = requireNotNull(score.toWifiPlace())
            val current = networkMeasureContent(MeasureStatus.Completed(score), onlineRadar(), null)
            val inspected = networkMeasureContent(MeasureStatus.Idle, onlineRadar(), place)
            val expected = if (speed > 0) "$speed Mbps" else MISSING_METRIC
            assertEquals(expected, linkSpeedText(place.linkSpeedMbps))
            assertEquals(expected, current.linkSpeed)
            assertEquals(expected, inspected.linkSpeed)
            assertEquals(score, place.measurement)
        }
    }

    @Test
    fun ping_formatter_preserves_success_failure_and_missing_data() {
        assertEquals("Không phản hồi", pingText(2000L, false))
        assertEquals("2000 ms", pingText(2000L, true))
        assertEquals("0 ms", pingText(0L, true))
        assertEquals(MISSING_METRIC, pingText(null, null))
        assertEquals(MISSING_METRIC, pingText(-1L, true))
        assertEquals(MISSING_METRIC, pingText(12L, null))
    }

    @Test
    fun place_uses_captured_identity_location_and_zero_score_without_floors_or_offsets() {
        val score = scoreA().copy(finalScore = 0)
        val place = requireNotNull(score.toWifiPlace())
        assertEquals("measured_measurement-A", place.id)
        assertEquals("Network A", place.ssid)
        assertEquals("Network A", place.name)
        assertEquals(0, place.score)
        assertEquals("Kém", place.qualityLabel)
        assertEquals(10.77, place.latitude, 0.0)
        assertEquals(106.70, place.longitude, 0.0)
        assertEquals("", place.isp)
        assertFalse(place.isDemo)
        assertFalse(place.isFree)
    }

    @Test
    fun missing_measurement_or_location_does_not_fabricate_a_map_place() {
        assertNull(scoreA().copy(measurement = null).toWifiPlace())
        assertNull(scoreA().let { it.copy(measurement = it.measurement!!.copy(location = null)) }.toWifiPlace())
    }

    @Test
    fun sample_places_are_explicit_demos_without_download_values_relabelled_as_link_speed() {
        assertTrue(DefaultWifiPlaces.all { it.isDemo })
        assertTrue(DefaultWifiPlaces.all { it.linkSpeedMbps == null })
        assertTrue(DefaultWifiPlaces.all { it.measurement == null })
    }

    @Test
    fun hidden_identity_stays_unknown_without_suppressing_legitimate_names() {
        for (ssid in listOf(null, "", "<unknown ssid>", "Đang quét tín hiệu...", "Mạng Wi-Fi (Cần bật GPS)")) {
            assertEquals(MISSING_METRIC, networkName(ssid))
        }
        assertEquals("Unknown Cafe", networkName("Unknown Cafe"))
        val score = scoreA().let { it.copy(measurement = it.measurement!!.copy(ssid = "<unknown ssid>", bssid = "02:00:00:00:00:00")) }
        val content = networkMeasureContent(MeasureStatus.Completed(score), onlineRadar(), null)
        assertEquals(MISSING_METRIC, content.ssid)
        assertEquals(MISSING_METRIC, content.bssid)
    }
}
