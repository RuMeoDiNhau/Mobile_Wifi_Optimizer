package com.mobilelegend.wlo.presentation.home

import com.mobilelegend.wlo.domain.model.LocationScore
import com.mobilelegend.wlo.presentation.components.WifiNeedOption
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.measure_and_result.SavedSpotItem

/** Compare only scores actually measured for this category; never rescore another need. */
fun homeRecommendations(
    places: List<WifiPlaceItem>,
    need: WifiNeedOption,
    query: String = "",
    showAll: Boolean = false
): List<WifiPlaceItem> {
    val search = query.trim()
    val matching = places.filter { place ->
        val score = place.measurement ?: return@filter false
        !place.isDemo && score.measurement != null && score.category == need.category &&
            (search.isEmpty() || place.name.contains(search, ignoreCase = true) ||
                place.district.contains(search, ignoreCase = true))
    }.sortedWith(compareByDescending<WifiPlaceItem> { it.measurement!!.finalScore }
        .thenByDescending { it.measurement!!.measurement!!.measuredAtMillis }
        .thenBy { it.id })
    return (if (showAll) matching else matching.take(3)).mapIndexed { index, place ->
        place.copy(rank = index + 1, score = place.measurement!!.finalScore)
    }
}

/** Includes measurements without GPS, but never the latest result of a different category. */
fun latestMeasurementForNeed(spots: List<SavedSpotItem>, need: WifiNeedOption): LocationScore? =
    spots.map { it.score }.filter { it.category == need.category && it.measurement != null }
        .maxWithOrNull(compareBy<LocationScore> { it.measurement!!.measuredAtMillis }
            .thenBy { it.measurement!!.id })
