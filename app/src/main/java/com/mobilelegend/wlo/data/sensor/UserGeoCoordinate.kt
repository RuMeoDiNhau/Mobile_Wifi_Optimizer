package com.mobilelegend.wlo.data.sensor

import com.mobilelegend.wlo.domain.model.LocationConfidence
import com.mobilelegend.wlo.domain.model.MeasurementLocation
import com.mobilelegend.wlo.domain.model.validCoordinates

const val DEFAULT_MAP_LAT = 10.7772
const val DEFAULT_MAP_LON = 106.7018
const val MAX_LOCATION_AGE_MS = 120_000L
const val PRECISE_LOCATION_ACCURACY_METERS = 100f

data class UserGeoCoordinate(
    val latitude: Double = DEFAULT_MAP_LAT,
    val longitude: Double = DEFAULT_MAP_LON,
    val confidence: LocationConfidence = LocationConfidence.UNAVAILABLE,
    val capturedAtMillis: Long? = null,
    val accuracyMeters: Float? = null
) {
    val isRealFix: Boolean get() = validCoordinates(latitude, longitude) &&
        confidence in setOf(LocationConfidence.RECENT, LocationConfidence.APPROXIMATE)
    private val accuracyText: String get() = accuracyMeters?.takeIf { it.isFinite() && it > 0 }
        ?.let { "khoảng ${it.toInt()} m" } ?: "chưa xác định"
    val statusText: String get() = when (confidence) {
        LocationConfidence.RECENT -> "Vị trí mới · sai số $accuracyText"
        LocationConfidence.APPROXIMATE -> "Vị trí gần đúng · sai số $accuracyText"
        LocationConfidence.STALE -> "Vị trí đã cũ; đang chờ cập nhật"
        LocationConfidence.MOCK -> "Vị trí mô phỏng; không gắn vào kết quả đo"
        else -> "Chưa xác định vị trí; bản đồ đang dùng tâm mặc định"
    }
    fun toMeasurementLocation(): MeasurementLocation? = if (isRealFix) MeasurementLocation(
        latitude, longitude, capturedAtMillis, accuracyMeters, confidence
    ) else null
}

fun assessLocation(
    latitude: Double, longitude: Double, capturedAtMillis: Long,
    ageMillis: Long, accuracyMeters: Float?, precisePermission: Boolean, isMock: Boolean
): UserGeoCoordinate {
    val confidence = when {
        !validCoordinates(latitude, longitude) || accuracyMeters == null ||
            !accuracyMeters.isFinite() || accuracyMeters <= 0 -> LocationConfidence.UNAVAILABLE
        isMock -> LocationConfidence.MOCK
        ageMillis !in 0..MAX_LOCATION_AGE_MS -> LocationConfidence.STALE
        !precisePermission || accuracyMeters > PRECISE_LOCATION_ACCURACY_METERS -> LocationConfidence.APPROXIMATE
        else -> LocationConfidence.RECENT
    }
    return UserGeoCoordinate(latitude, longitude, confidence, capturedAtMillis, accuracyMeters)
}
