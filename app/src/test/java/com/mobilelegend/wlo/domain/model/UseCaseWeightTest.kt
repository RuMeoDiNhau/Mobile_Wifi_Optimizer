package com.mobilelegend.wlo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test kiểm tra tính toàn vẹn và chính xác của cấu hình Trọng số (UseCaseWeight).
 */
class UseCaseWeightTest {

    private val delta = 0.0001f

    @Test
    fun gaming_weight_sum_must_equal_one() {
        val weight = UseCaseWeight.getWeightFor(UseCaseCategory.GAMING)
        val sum = weight.pingWeight + weight.rssiWeight + weight.speedWeight
        assertEquals(1.0f, sum, delta)
        assertEquals(0.60f, weight.pingWeight, delta)
        assertEquals(0.30f, weight.rssiWeight, delta)
        assertEquals(0.10f, weight.speedWeight, delta)
    }

    @Test
    fun study_download_weight_sum_must_equal_one() {
        val weight = UseCaseWeight.getWeightFor(UseCaseCategory.STUDY_DOWNLOAD)
        val sum = weight.pingWeight + weight.rssiWeight + weight.speedWeight
        assertEquals(1.0f, sum, delta)
        assertEquals(0.10f, weight.pingWeight, delta)
        assertEquals(0.40f, weight.rssiWeight, delta)
        assertEquals(0.50f, weight.speedWeight, delta)
    }

    @Test
    fun video_call_weight_sum_must_equal_one() {
        val weight = UseCaseWeight.getWeightFor(UseCaseCategory.VIDEO_CALL)
        val sum = weight.pingWeight + weight.rssiWeight + weight.speedWeight
        assertEquals(1.0f, sum, delta)
        assertEquals(0.40f, weight.pingWeight, delta)
        assertEquals(0.40f, weight.rssiWeight, delta)
        assertEquals(0.20f, weight.speedWeight, delta)
    }

    @Test
    fun all_weights_must_be_positive_and_non_zero() {
        UseCaseCategory.values().forEach { category ->
            val weight = UseCaseWeight.getWeightFor(category)
            assertTrue("Ping weight must be > 0 for $category", weight.pingWeight > 0f)
            assertTrue("RSSI weight must be > 0 for $category", weight.rssiWeight > 0f)
            assertTrue("Speed weight must be > 0 for $category", weight.speedWeight > 0f)
        }
    }
}
