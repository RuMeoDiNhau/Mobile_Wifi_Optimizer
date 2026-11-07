package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.presentation.components.MISSING_METRIC
import com.mobilelegend.wlo.presentation.measure_and_result.SavedSpotItem
import com.mobilelegend.wlo.presentation.measure_and_result.historyComparison
import com.mobilelegend.wlo.presentation.measure_and_result.historyMeasurementDetails
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class HistoryComparisonTest {
    private val gaming = UseCaseCategory.GAMING
    private val study = UseCaseCategory.STUDY_DOWNLOAD
    private val video = UseCaseCategory.VIDEO_CALL
    private fun spot(id: String, points: Int, category: UseCaseCategory = gaming,
                     time: Long = 1000L, saved: Boolean = true): SavedSpotItem {
        val original = scoreA()
        val score = original.copy(finalScore = points, category = category,
            measurement = original.measurement!!.copy(id = id, measuredAtMillis = time))
        return SavedSpotItem(id, "Place $id", score, isSaved = saved)
    }

    @Test fun empty_history_has_no_groups_or_winners() {
        assertTrue(historyComparison(emptyList()).isEmpty())
    }

    @Test fun mixed_needs_choose_winners_independently_instead_of_one_global_maximum() {
        val spots = listOf(spot("g1", 70), spot("s1", 99, study), spot("v1", 60, video),
            spot("g2", 50), spot("s2", 80, study), spot("v2", 40, video))
        val groups = historyComparison(spots)
        assertEquals(setOf(gaming, study, video), groups.map { it.category }.toSet())
        assertEquals(setOf("g1", "s1", "v1"), groups.flatMap { it.rows }.filter { it.bestLabel != null }.map { it.spot.id }.toSet())
        assertTrue(groups.all { group -> group.rows.all { it.spot.score.category == group.category } })
        assertTrue(groups.flatMap { it.rows }.filter { it.bestLabel != null }.all {
            it.bestLabel!!.contains("trong các điểm đã lưu cho nhu cầu này")
        })
    }

    @Test fun single_result_per_need_has_no_comparison_badge_even_with_many_total_results() {
        val groups = historyComparison(listOf(spot("g", 90), spot("s", 80, study), spot("v", 70, video)))
        assertEquals(3, groups.size)
        assertTrue(groups.all { it.savedCount == 1 })
        assertTrue(groups.flatMap { it.rows }.all { it.bestLabel == null })
    }

    @Test fun tied_highest_scores_all_receive_an_explicit_tie_label() {
        val rows = historyComparison(listOf(spot("a", 90), spot("b", 90), spot("c", 80))).single().rows
        assertEquals(listOf("a", "b"), rows.filter { it.bestLabel != null }.map { it.spot.id })
        assertTrue(rows.take(2).all { it.bestLabel!!.startsWith("Đồng điểm cao nhất") })
        assertNull(rows.last().bestLabel)
    }

    @Test fun all_zero_scores_stay_zero_and_are_relative_ties_not_good_recommendations() {
        val spots = listOf(spot("a", 0), spot("b", 0)).map { it.copy(score = it.score.copy(isRecommended = false)) }
        val rows = historyComparison(spots).single().rows
        assertTrue(rows.all { it.spot.score.finalScore == 0 && !it.spot.score.isRecommended })
        assertTrue(rows.all { it.bestLabel!!.startsWith("Đồng điểm cao nhất") })
    }

    @Test fun unsaved_higher_score_remains_visible_but_cannot_win_saved_comparison() {
        val group = historyComparison(listOf(spot("unsaved", 100, saved = false), spot("a", 70), spot("b", 50))).single()
        assertEquals(3, group.rows.size)
        assertEquals(2, group.savedCount)
        assertEquals("a", group.rows.single { it.bestLabel != null }.spot.id)
        assertNull(group.rows.first().bestLabel)
    }

    @Test fun no_saved_results_keeps_history_without_inventing_a_best_result() {
        val group = historyComparison(listOf(spot("a", 95, saved = false), spot("b", 75, saved = false))).single()
        assertEquals(0, group.savedCount)
        assertEquals(2, group.rows.size)
        assertTrue(group.rows.all { it.bestLabel == null })
    }

    @Test fun deleting_winner_recomputes_within_group_and_removes_empty_groups() {
        val history = listOf(spot("a", 99), spot("b", 80), spot("c", 60), spot("s", 100, study))
        val after = historyComparison(history.filterNot { it.id in setOf("a", "s") })
        assertEquals(1, after.size)
        assertEquals(gaming, after.single().category)
        assertEquals("b", after.single().rows.single { it.bestLabel != null }.spot.id)
        assertTrue(historyComparison(history.filter { it.id == "b" }).single().rows.all { it.bestLabel == null })
    }

    @Test fun order_is_deterministic_within_each_need_by_score_time_and_id() {
        val input = listOf(spot("c", 80, time = 2000), spot("b", 80, time = 1000),
            spot("a", 80, time = 2000), spot("d", 70, time = 3000))
        val first = historyComparison(input).single().rows
        val reversed = historyComparison(input.reversed()).single().rows
        assertEquals(listOf("a", "c", "b", "d"), first.map { it.spot.id })
        assertEquals(first, reversed)
    }

    @Test fun detail_uses_original_snapshot_network_category_and_time_not_view_time_or_record_override() {
        val original = spot("a", 80, video, time = 0).copy(timestamp = 9_999_999)
        val details = historyMeasurementDetails(original, TimeZone.getTimeZone("UTC"))
        assertEquals(video.title, details.categoryTitle)
        assertEquals("Network A", details.ssid)
        assertEquals("AA:BB:CC:DD:EE:01", details.bssid)
        assertEquals("01/01/1970 00:00:00 UTC", details.measuredAtText)
        assertSame(original, historyComparison(listOf(original)).single().rows.single().spot)
    }

    @Test fun formatting_includes_date_seconds_and_timezone_without_mutating_timestamp() {
        val original = spot("a", 80, time = 1000L)
        val details = historyMeasurementDetails(original, TimeZone.getTimeZone("GMT+07:00"))
        assertEquals("01/01/1970 07:00:01 GMT+07:00", details.measuredAtText)
        assertEquals(1000L, original.score.measurement!!.measuredAtMillis)
    }

    @Test fun malformed_snapshot_shows_missing_details_and_never_competes() {
        val missing = spot("missing", 100).let { it.copy(score = it.score.copy(measurement = null)) }
        val group = historyComparison(listOf(missing, spot("valid", 60))).single()
        assertEquals(1, group.savedCount)
        assertTrue(group.rows.all { it.bestLabel == null })
        val details = historyMeasurementDetails(missing)
        assertEquals(MISSING_METRIC, details.ssid)
        assertEquals(MISSING_METRIC, details.bssid)
        assertEquals(MISSING_METRIC, details.measuredAtText)
    }

    @Test fun same_location_or_missing_gps_does_not_merge_measurement_ids() {
        val first = spot("a", 80)
        val second = spot("b", 70).let { it.copy(score = it.score.copy(measurement = it.score.measurement!!.copy(location = null))) }
        val rows = historyComparison(listOf(first, second)).single().rows
        assertEquals(listOf("a", "b"), rows.map { it.spot.id })
        assertSame(first.score, rows[0].spot.score)
        assertSame(second.score, rows[1].spot.score)
        assertNotNull(rows.first().bestLabel)
    }
}
