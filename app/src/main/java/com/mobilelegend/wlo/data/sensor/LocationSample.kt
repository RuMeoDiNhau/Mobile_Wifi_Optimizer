package com.mobilelegend.wlo.data.sensor

/** Raw timestamps remain available so a cached fix ages on every read. */
data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val capturedAtMillis: Long,
    val elapsedRealtimeNanos: Long,
    val accuracyMeters: Float?,
    val isMock: Boolean = false
) {
    fun assess(nowNanos: Long, precisePermission: Boolean): UserGeoCoordinate {
        val age = if (elapsedRealtimeNanos <= 0 || elapsedRealtimeNanos > nowNanos) -1L
            else (nowNanos - elapsedRealtimeNanos) / 1_000_000L
        return assessLocation(latitude, longitude, capturedAtMillis, age, accuracyMeters, precisePermission, isMock)
    }
}

/** Prefer usable samples; a newer mock/invalid fix cannot hide a genuine recent fix.
 * Recency wins within each group, with accuracy breaking equal-time ties.
 */
fun selectLocationSample(samples: List<LocationSample>, nowNanos: Long, precise: Boolean): UserGeoCoordinate {
    val assessed = samples.map { it to it.assess(nowNanos, precise) }
    val usable = assessed.filter { it.second.isRealFix }
    return (usable.ifEmpty { assessed }).maxWithOrNull(
        compareBy<Pair<LocationSample, UserGeoCoordinate>> { it.first.elapsedRealtimeNanos }
            .thenBy { -(it.first.accuracyMeters?.takeIf { accuracy -> accuracy.isFinite() && accuracy > 0 }
                ?: Float.MAX_VALUE) }
    )?.second ?: UserGeoCoordinate()
}
