package com.mobilelegend.wlo.domain.model

/** Identity and time captured by the benchmark, never reconstructed from live UI state. */
data class MeasurementContext(
    val id: String,
    val ssid: String,
    val bssid: String,
    val measuredAtMillis: Long,
    val location: MeasurementLocation? = null
)

data class MeasurementLocation(val latitude: Double, val longitude: Double)
