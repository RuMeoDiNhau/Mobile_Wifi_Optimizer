package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.domain.model.UseCaseCategory
import com.mobilelegend.wlo.presentation.components.DefaultWifiPlaces
import com.mobilelegend.wlo.presentation.components.WifiNeedOption
import com.mobilelegend.wlo.presentation.components.toWifiPlace
import com.mobilelegend.wlo.presentation.home.homeRecommendations
import com.mobilelegend.wlo.presentation.home.latestMeasurementForNeed
import com.mobilelegend.wlo.presentation.measure_and_result.SavedSpotItem
import org.junit.Assert.*
import org.junit.Test

class HomeRecommendationsTest {
    private fun score(id: String, category: UseCaseCategory, points: Int, time: Long = 1000L) =
        scoreA().let { it.copy(finalScore = points, category = category,
            measurement = it.measurement!!.copy(id = id, measuredAtMillis = time)) }
    private fun place(id: String, category: UseCaseCategory, points: Int, time: Long = 1000L) =
        requireNotNull(score(id, category, points, time).toWifiPlace())
    private val gaming = UseCaseCategory.GAMING
    private val study = UseCaseCategory.STUDY_DOWNLOAD
    private val video = UseCaseCategory.VIDEO_CALL

    @Test fun selecting_each_need_changes_results_to_only_its_measured_category() {
        val places = listOf(place("g", gaming, 90), place("s", study, 75), place("v", video, 80))
        for (need in WifiNeedOption.values()) {
            val result = homeRecommendations(places, need).single()
            assertEquals(need.category, result.measurement!!.category)
            assertEquals(1, result.rank)
        }
    }

    @Test fun missing_category_returns_empty_instead_of_other_need_or_demo() {
        val places = DefaultWifiPlaces + place("g", gaming, 100)
        assertTrue(homeRecommendations(places, WifiNeedOption.VIDEO_MEETING).isEmpty())
        assertTrue(homeRecommendations(emptyList(), WifiNeedOption.WORK).isEmpty())
    }

    @Test fun generic_suitable_need_tags_cannot_override_actual_measurement_category() {
        val misleading = place("g", gaming, 100).copy(suitableNeeds = WifiNeedOption.values().toSet())
        assertTrue(homeRecommendations(listOf(misleading), WifiNeedOption.WORK).isEmpty())
    }

    @Test fun demo_and_missing_measurement_identity_never_rank() {
        val valid = place("g", gaming, 70)
        val input = listOf(valid.copy(id = "demo", isDemo = true),
            valid.copy(id = "no-score", measurement = null),
            valid.copy(id = "no-id", measurement = valid.measurement!!.copy(measurement = null)), valid)
        assertEquals(listOf(valid), homeRecommendations(input, WifiNeedOption.GAMING))
    }

    @Test fun ordering_uses_score_then_recency_then_id_with_contiguous_ranks() {
        val input = listOf(place("d", gaming, 40), place("c", gaming, 90, 2000),
            place("a", gaming, 90, 2000), place("b", gaming, 90, 1000))
        val output = homeRecommendations(input, WifiNeedOption.GAMING, showAll = true)
        assertEquals(listOf("measured_a", "measured_c", "measured_b", "measured_d"), output.map { it.id })
        assertEquals(listOf(1, 2, 3, 4), output.map { it.rank })
    }

    @Test fun default_top_three_is_applied_after_filtering_and_sorting() {
        val input = listOf(place("other", video, 100)) + (1..5).map { place("$it", gaming, it * 10) }
        assertEquals(listOf(50, 40, 30), homeRecommendations(input, WifiNeedOption.GAMING).map { it.score })
        assertEquals(5, homeRecommendations(input, WifiNeedOption.GAMING, showAll = true).size)
    }

    @Test fun query_matches_name_or_district_ignoring_case_and_outer_whitespace_within_need() {
        val match = place("g", gaming, 70).copy(name = "Study Hall", district = "District One")
        val other = place("s", study, 100).copy(name = "Study Hall")
        for (query in listOf(" HALL ", "district one", "  ")) {
            assertEquals(listOf(match), homeRecommendations(listOf(other, match), WifiNeedOption.GAMING, query))
        }
        assertTrue(homeRecommendations(listOf(match), WifiNeedOption.GAMING, "not here").isEmpty())
    }

    @Test fun work_and_relax_share_the_existing_download_score_without_rescoring() {
        val input = listOf(place("s", study, 65), place("g", gaming, 99))
        val work = homeRecommendations(input, WifiNeedOption.WORK)
        val relax = homeRecommendations(input, WifiNeedOption.RELAX)
        assertEquals(work, relax)
        assertSame(input.first().measurement, relax.single().measurement)
    }

    @Test fun ranking_preserves_snapshot_and_uses_its_score_instead_of_decorative_value() {
        val original = place("g", gaming, 0).copy(rank = 99, score = 100)
        val result = homeRecommendations(listOf(original), WifiNeedOption.GAMING).single()
        assertEquals(0, result.score)
        assertEquals(1, result.rank)
        assertSame(original.measurement, result.measurement)
        assertEquals(original.copy(rank = 1, score = 0), result)
        assertEquals(100, original.score)
        assertEquals(99, original.rank)
    }

    @Test fun latest_card_uses_matching_category_time_instead_of_highest_score_or_list_order() {
        val old = score("old", study, 100, 1000)
        val latest = score("latest", study, 0, 2000)
        val other = score("other", gaming, 90, 3000)
        val spots = listOf(latest, other, old).map { SavedSpotItem(it.measurement!!.id, "Name", it) }
        assertSame(latest, latestMeasurementForNeed(spots, WifiNeedOption.WORK))
        assertSame(latest, latestMeasurementForNeed(spots, WifiNeedOption.RELAX))
        assertNull(latestMeasurementForNeed(spots, WifiNeedOption.VIDEO_MEETING))
    }

    @Test fun latest_card_can_show_measurement_without_gps_but_excludes_missing_snapshot() {
        val noGps = score("real", study, 60).let { it.copy(measurement = it.measurement!!.copy(location = null)) }
        val malformed = score("missing", study, 100).copy(measurement = null)
        val spots = listOf(SavedSpotItem("real", "No GPS", noGps, isSaved = false),
            SavedSpotItem("missing", "No identity", malformed, timestamp = 9999))
        assertSame(noGps, latestMeasurementForNeed(spots, WifiNeedOption.WORK))
    }

    @Test fun recomputing_after_remove_or_selection_change_does_not_keep_previous_rankings() {
        val gamingPlace = place("g", gaming, 99)
        val workPlace = place("s", study, 60)
        val all = listOf(gamingPlace, workPlace)
        assertEquals(listOf(gamingPlace), homeRecommendations(all, WifiNeedOption.GAMING))
        assertEquals(listOf(workPlace), homeRecommendations(all, WifiNeedOption.WORK))
        assertTrue(homeRecommendations(listOf(gamingPlace), WifiNeedOption.WORK).isEmpty())
    }
}
