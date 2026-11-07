package com.mobilelegend.wlo.regression

import com.mobilelegend.wlo.presentation.components.DefaultWifiPlaces
import com.mobilelegend.wlo.presentation.components.WifiNeedOption
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.components.toWifiPlace
import com.mobilelegend.wlo.presentation.map.MapContent
import com.mobilelegend.wlo.presentation.map.MapFilterChip
import com.mobilelegend.wlo.presentation.map.mapContent
import org.junit.Assert.*
import org.junit.Test

class MapContentTest {
    private fun place(id: String, name: String = id) = requireNotNull(scoreA().let {
        it.copy(measurement = it.measurement!!.copy(id = id)).toWifiPlace()
    }).copy(name = name)

    private fun content(
        places: List<WifiPlaceItem>,
        query: String = "",
        filter: MapFilterChip = MapFilterChip.ALL,
        showDemo: Boolean = false,
        selectedId: String? = null
    ) = mapContent(places, query, filter, showDemo, selectedId)

    private fun assertEmpty(result: MapContent) {
        assertTrue(result.places.isEmpty())
        assertNull("No detail/action target without a matching marker", result.selectedPlace)
        assertFalse(result.emptyMessage.isNullOrBlank())
    }

    @Test fun empty_input_is_safe_with_any_filter_or_stale_selection() {
        for (filter in MapFilterChip.values()) {
            assertEmpty(content(emptyList(), filter = filter, selectedId = "removed"))
        }
    }

    @Test fun unmatched_query_clears_markers_and_the_previous_detail_target() {
        val original = place("a", "Coffee House")
        assertEmpty(content(listOf(original), query = "not here", selectedId = original.id))
    }

    @Test fun unmatched_filters_never_fall_back_to_the_unfiltered_first_place() {
        val original = place("a").copy(score = 89, suitableNeeds = emptySet(), isFree = false)
        for (filter in listOf(MapFilterChip.EXCELLENT, MapFilterChip.WORK, MapFilterChip.FREE)) {
            assertEmpty(content(listOf(original), filter = filter, selectedId = original.id))
        }
    }

    @Test fun query_and_filter_must_match_the_same_place() {
        val queryOnly = place("query", "Coffee").copy(isFree = false)
        val filterOnly = place("filter", "Library").copy(isFree = true)
        val both = place("both", "Coffee Annex").copy(isFree = true)
        assertEmpty(content(listOf(queryOnly, filterOnly), "coffee", MapFilterChip.FREE))
        val result = content(listOf(queryOnly, filterOnly, both), "coffee", MapFilterChip.FREE,
            selectedId = queryOnly.id)
        assertEquals(listOf(both), result.places)
        assertSame(both, result.selectedPlace)
        assertNull(result.emptyMessage)
    }

    @Test fun query_matches_name_or_district_ignoring_case_and_outer_whitespace() {
        val match = place("match", "Coffee House").copy(district = "District One")
        val unrelated = place("other", "Library").copy(district = "District Two")
        for (query in listOf(" HOUSE ", " district ONE ")) {
            val result = content(listOf(unrelated, match), query)
            assertEquals(listOf(match), result.places)
            assertSame(match, result.selectedPlace)
        }
    }

    @Test fun blank_query_keeps_all_visible_results_in_input_order() {
        val places = listOf(place("b"), place("a"))
        val result = content(places, " \t\n ")
        assertEquals(places, result.places)
        assertSame(places.first(), result.selectedPlace)
        assertNull(result.emptyMessage)
    }

    @Test fun excellent_filter_includes_the_ninety_point_boundary() {
        val below = place("below").copy(score = 89)
        val boundary = place("boundary").copy(score = 90)
        val above = place("above").copy(score = 100)
        val result = content(listOf(below, boundary, above), filter = MapFilterChip.EXCELLENT,
            selectedId = below.id)
        assertEquals(listOf(boundary, above), result.places)
        assertSame(boundary, result.selectedPlace)
    }

    @Test fun work_and_free_filters_use_their_respective_place_attributes() {
        val work = place("work").copy(suitableNeeds = setOf(WifiNeedOption.WORK), isFree = false)
        val free = place("free").copy(suitableNeeds = setOf(WifiNeedOption.GAMING), isFree = true)
        val places = listOf(work, free)
        assertEquals(listOf(work), content(places, filter = MapFilterChip.WORK).places)
        assertEquals(listOf(free), content(places, filter = MapFilterChip.FREE).places)
    }

    @Test fun selected_matching_place_survives_reordering_without_selecting_the_first() {
        val a = place("a")
        val b = place("b")
        for (places in listOf(listOf(a, b), listOf(b, a))) {
            assertSame(b, content(places, selectedId = b.id).selectedPlace)
        }
    }

    @Test fun filtered_out_selection_moves_only_to_a_matching_result() {
        val a = place("a", "Alpha")
        val b = place("b", "Beta")
        val result = content(listOf(a, b), "beta", selectedId = a.id)
        assertEquals(listOf(b), result.places)
        assertSame(b, result.selectedPlace)
    }

    @Test fun deleting_selected_place_selects_a_remaining_result_then_null_when_all_deleted() {
        val a = place("a")
        val b = place("b")
        assertSame(b, content(listOf(a, b), selectedId = b.id).selectedPlace)
        val remaining = content(listOf(a), selectedId = b.id)
        assertSame(a, remaining.selectedPlace)
        assertEmpty(content(emptyList(), selectedId = remaining.selectedPlace?.id))
    }

    @Test fun same_id_uses_the_current_snapshot_for_details_without_mutating_the_old_one() {
        val original = place("a")
        val replacement = original.copy(name = "Renamed", score = 0, latitude = 21.03, longitude = 105.85)
        val first = content(listOf(original), selectedId = original.id)
        val second = content(listOf(replacement), selectedId = original.id)
        assertSame(original, first.selectedPlace)
        assertSame(replacement, second.selectedPlace)
        assertSame(second.places.single(), second.selectedPlace)
        assertSame(original.measurement, second.selectedPlace!!.measurement)
        assertEquals("a", original.name)
        assertEquals(87, original.score)
    }

    @Test fun demo_only_data_is_hidden_until_opted_in_and_selection_clears_when_hidden_again() {
        val hidden = content(DefaultWifiPlaces)
        assertEmpty(hidden)
        assertTrue(hidden.emptyMessage!!.contains("minh họa"))
        val visible = content(DefaultWifiPlaces, showDemo = true)
        assertEquals(DefaultWifiPlaces, visible.places)
        assertSame(DefaultWifiPlaces.first(), visible.selectedPlace)
        assertEmpty(content(DefaultWifiPlaces, selectedId = visible.selectedPlace?.id))
    }

    @Test fun hiding_selected_demo_keeps_only_real_results_and_their_snapshot() {
        val real = place("real")
        val demo = DefaultWifiPlaces.first()
        val places = listOf(demo, real)
        assertSame(demo, content(places, showDemo = true, selectedId = demo.id).selectedPlace)
        val result = content(places, selectedId = demo.id)
        assertEquals(listOf(real), result.places)
        assertSame(real, result.selectedPlace)
        assertNull(result.emptyMessage)
    }

    @Test fun invalid_coordinates_cannot_provide_markers_or_detail_targets_even_in_demo_mode() {
        val valid = place("valid")
        val invalid = listOf(
            valid.copy(id = "nan", latitude = Double.NaN),
            valid.copy(id = "inf", longitude = Double.POSITIVE_INFINITY),
            valid.copy(id = "lat", latitude = 90.1),
            valid.copy(id = "lon", longitude = -180.1),
            valid.copy(id = "demo", isDemo = true, longitude = Double.NaN)
        )
        assertEmpty(content(invalid, showDemo = true, selectedId = "nan"))
        val result = content(invalid + valid, showDemo = true, selectedId = "demo")
        assertEquals(listOf(valid), result.places)
        assertSame(valid, result.selectedPlace)
    }

    @Test fun clearing_query_recovers_results_and_reapplying_it_clears_the_target_again() {
        val original = place("a", "Coffee")
        val places = listOf(original)
        val empty = content(places, "missing", selectedId = original.id)
        assertEmpty(empty)
        val recovered = content(places, selectedId = empty.selectedPlace?.id)
        assertSame(original, recovered.selectedPlace)
        assertNull(recovered.emptyMessage)
        assertEmpty(content(places, "missing", selectedId = recovered.selectedPlace?.id))
    }

    @Test fun no_matches_and_no_valid_locations_explain_different_empty_states() {
        val noLocations = content(emptyList())
        val noMatches = content(listOf(place("a")), "missing")
        assertEmpty(noLocations)
        assertEmpty(noMatches)
        assertTrue(noLocations.emptyMessage!!.contains("tọa độ hợp lệ"))
        assertTrue(noMatches.emptyMessage!!.contains("khớp tìm kiếm và bộ lọc"))
    }

    @Test fun every_filter_demo_and_query_combination_keeps_detail_target_in_marker_results() {
        val inputs = listOf(emptyList(), DefaultWifiPlaces, DefaultWifiPlaces + place("real", "Coffee"))
        for (places in inputs) for (showDemo in listOf(false, true)) {
            for (filter in MapFilterChip.values()) for (query in listOf("", "coffee", "missing")) {
                val result = content(places, query, filter, showDemo, "stale-id")
                if (result.places.isEmpty()) {
                    assertEmpty(result)
                } else {
                    assertTrue(result.places.any { it === result.selectedPlace })
                    assertNull(result.emptyMessage)
                }
            }
        }
    }
}
