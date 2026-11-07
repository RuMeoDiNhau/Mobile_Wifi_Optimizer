package com.mobilelegend.wlo.domain.model

/** Identity and time captured by the benchmark, never reconstructed from live UI state. */
data class MeasurementContext(
    val id: String,
    val ssid: String,
    val bssid: String,
    val measuredAtMillis: Long,
    val location: MeasurementLocation? = null,
    val networkId: String? = null
)

enum class LocationConfidence { RECENT, APPROXIMATE, STALE, MOCK, UNAVAILABLE, UNKNOWN }

fun validCoordinates(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

data class MeasurementLocation(
    val latitude: Double,
    val longitude: Double,
    val capturedAtMillis: Long? = null,
    val accuracyMeters: Float? = null,
    val confidence: LocationConfidence = LocationConfidence.UNKNOWN
)
