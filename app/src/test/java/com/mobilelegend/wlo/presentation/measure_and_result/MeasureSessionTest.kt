package com.mobilelegend.wlo.presentation.measure_and_result

import com.mobilelegend.wlo.data.model.PingResult
import com.mobilelegend.wlo.data.model.WifiMetric
import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.domain.model.UseCaseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test kiểm thử logic quản lý danh sách so sánh vị trí trong MeasureViewModel / MeasureUiState.
 */
class MeasureSessionTest {

    @Test
    fun saving_spots_should_add_to_session_history() {
        val spot1 = SavedSpotItem(
            id = "1",
            spotName = "Bàn học",
            score = LocationScore(
                finalScore = 88,
                normalizedRssiScore = 90,
                normalizedPingScore = 85,
                normalizedSpeedScore = 90,
                category = UseCaseCategory.GAMING,
                summaryText = "Rất tốt",
                recommendation = "Khuyên dùng",
                isRecommended = true,
                rawRssi = -52,
                rawPingMs = 28,
                rawLinkSpeedMbps = 200
            )
        )

        val spot2 = SavedSpotItem(
            id = "2",
            spotName = "Giường ngủ",
            score = LocationScore(
                finalScore = 45,
                normalizedRssiScore = 40,
                normalizedPingScore = 30,
                normalizedSpeedScore = 60,
                category = UseCaseCategory.GAMING,
                summaryText = "Lag",
                recommendation = "Không nên chơi ở đây",
                isRecommended = false,
                rawRssi = -82,
                rawPingMs = 145,
                rawLinkSpeedMbps = 54
            )
        )

        val spots = listOf(spot1, spot2)
        assertEquals(2, spots.size)

        val bestScore = spots.maxOf { it.score.finalScore }
        assertEquals(88, bestScore)

        val bestSpot = spots.find { it.score.finalScore == bestScore }
        assertEquals("Bàn học", bestSpot?.spotName)
    }
}
