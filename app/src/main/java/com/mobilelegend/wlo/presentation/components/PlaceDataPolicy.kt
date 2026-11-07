package com.mobilelegend.wlo.presentation.components

import com.mobilelegend.wlo.data.sensor.UserGeoCoordinate
import com.mobilelegend.wlo.domain.model.validCoordinates
import java.util.Locale
import kotlin.math.*

fun measuredPlacesOnly(places: List<WifiPlaceItem>): List<WifiPlaceItem> =
    places.filter { !it.isDemo && it.measurement != null }

fun visibleMapPlaces(places: List<WifiPlaceItem>, showDemo: Boolean): List<WifiPlaceItem> =
    places.filter { (!it.isDemo || showDemo) && validCoordinates(it.latitude, it.longitude) }

/** Movement changes distance labels only, never the coordinates of a place. */
fun placesWithDistances(places: List<WifiPlaceItem>, user: UserGeoCoordinate): List<WifiPlaceItem> =
    places.map { place ->
        val distance = if (!user.isRealFix || place.isDemo || !validCoordinates(place.latitude, place.longitude)) {
            MISSING_METRIC
        } else {
            val latDelta = Math.toRadians(place.latitude - user.latitude)
            val lonDelta = Math.toRadians(place.longitude - user.longitude)
            val a = sin(latDelta / 2).pow(2) + cos(Math.toRadians(user.latitude)) *
                cos(Math.toRadians(place.latitude)) * sin(lonDelta / 2).pow(2)
            val meters = (6_371_000 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))).roundToInt()
            if (meters < 1000) "$meters m" else String.format(Locale.US, "%.1f km", meters / 1000.0).replace('.', ',')
        }
        place.copy(distanceText = distance)
    }
