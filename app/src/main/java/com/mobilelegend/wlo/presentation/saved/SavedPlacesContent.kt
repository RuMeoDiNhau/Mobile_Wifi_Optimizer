package com.mobilelegend.wlo.presentation.saved

import com.mobilelegend.wlo.data.sensor.UserGeoCoordinate
import com.mobilelegend.wlo.presentation.components.WifiPlaceItem
import com.mobilelegend.wlo.presentation.components.placesWithDistances
import com.mobilelegend.wlo.presentation.components.toWifiPlace
import com.mobilelegend.wlo.presentation.measure_and_result.SavedSpotItem

data class SavedPlacesContent(
    val places: List<WifiPlaceItem>,
    val totalMeasurements: Int,
    val savedWithoutLocation: Int
)

/** All counts and rows come from the same history snapshot; demo data has no input here. */
fun savedPlacesContent(spots: List<SavedSpotItem>, userLocation: UserGeoCoordinate): SavedPlacesContent {
    val bookmarked = spots.filter { it.isSaved }
    val mapped = bookmarked.asReversed().mapNotNull { spot ->
        spot.score.toWifiPlace()?.copy(name = spot.spotName, isSaved = true)
    }
    return SavedPlacesContent(
        places = placesWithDistances(mapped, userLocation).mapIndexed { index, place -> place.copy(rank = index + 1) },
        totalMeasurements = spots.size,
        savedWithoutLocation = bookmarked.size - mapped.size
    )
}
