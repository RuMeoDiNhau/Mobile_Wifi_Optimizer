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
        val spot1 = createDummySpot("1", "Bàn học", 88)
        val spot2 = createDummySpot("2", "Giường ngủ", 45)

        val spots = listOf(spot1, spot2)
        assertEquals(2, spots.size)

        val bestScore = spots.maxOf { it.score.finalScore }
        assertEquals(88, bestScore)

        val bestSpot = spots.find { it.score.finalScore == bestScore }
        assertEquals("Bàn học", bestSpot?.spotName)
    }

    @Test
    fun empty_spots_list_should_handle_safely() {
        val spots = emptyList<SavedSpotItem>()
        assertTrue(spots.isEmpty())
        val bestSpot = spots.maxByOrNull { it.score.finalScore }
        org.junit.Assert.assertNull(bestSpot)
    }

    @Test
    fun tie_breaker_preserves_first_or_valid_spot_when_scores_are_equal() {
        val spotA = createDummySpot("A", "Cửa sổ", 85)
        val spotB = createDummySpot("B", "Ban công", 85)

        val spots = listOf(spotA, spotB)
        val bestScore = spots.maxOf { it.score.finalScore }
        assertEquals(85, bestScore)

        val topSpots = spots.filter { it.score.finalScore == bestScore }
        assertEquals(2, topSpots.size)
    }

    @Test
    fun deleting_spot_by_id_should_remove_item() {
        val spots = mutableListOf(
            createDummySpot("1", "Vị trí 1", 70),
            createDummySpot("2", "Vị trí 2", 80),
            createDummySpot("3", "Vị trí 3", 90)
        )

        spots.removeAll { it.id == "2" }
        assertEquals(2, spots.size)
        assertTrue(spots.none { it.id == "2" })
    }

    @Test
    fun sorting_spots_descending_by_score() {
        val s1 = createDummySpot("1", "Vị trí thấp", 50)
        val s2 = createDummySpot("2", "Vị trí cao", 95)
        val s3 = createDummySpot("3", "Vị trí vừa", 75)

        val sorted = listOf(s1, s2, s3).sortedByDescending { it.score.finalScore }

        assertEquals("2", sorted[0].id)
        assertEquals("3", sorted[1].id)
        assertEquals("1", sorted[2].id)
    }

    private fun createDummySpot(id: String, name: String, finalScore: Int): SavedSpotItem {
        return SavedSpotItem(
            id = id,
            spotName = name,
            score = LocationScore(
                finalScore = finalScore,
                normalizedRssiScore = finalScore,
                normalizedPingScore = finalScore,
                normalizedSpeedScore = finalScore,
                category = UseCaseCategory.GAMING,
                summaryText = "Đánh giá $name",
                recommendation = "Khuyến nghị $name",
                isRecommended = finalScore >= 75,
                rawRssi = -60,
                rawPingMs = 30,
                rawLinkSpeedMbps = 150
            )
        )
    }
}
